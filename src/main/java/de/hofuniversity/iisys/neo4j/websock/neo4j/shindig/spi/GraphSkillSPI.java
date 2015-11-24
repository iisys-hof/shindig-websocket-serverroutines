/*
 * Copyright (c) 2012-2015 Institute of Information Systems, Hof University
 *
 * This file is part of "Apache Shindig WebSocket Server Routines".
 *
 * "Apache Shindig WebSocket Server Routines" is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphPerson;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphSkillSet;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the skill service retrieving skill data from the Neo4j graph database.
 */
public class GraphSkillSPI {

  private static final String AUTODELETE_PROP = "skills.unused.autodelete";

  private static final String GEN_ACTIVITY_ADD = "autoactivities.skill_added";
  private static final String GEN_ACTIVITY_REM = "autoactivities.skill_removed";
  private static final String FILTER_LINKS = "autoactivities.skills.filter_links";

  private static final String ADD_TITLE_PROP = "titles.skills.add";
  private static final String REMOVE_TITLE_PROP = "titles.skills.remove";
  private static final String NON_REF_ADD_VERB_PROP = "autoactivities.non_reflective_add.verb";

  private static final String NAME_FIELD = "name";
  private static final String FORMATTED_FIELD = "formatted";
  private static final String PEOPLE_FIELD = "people";

  private static final String VERB_ADD = "add";
  private static final String VERB_REMOVE = "remove";

  private static final String ID_FIELD = "id";

  private static final String SKILL_OBJ_TYPE = "skill";

  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;

  private final Index<Node> fSkillNodes;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  private final Map<String, Object> fGeneratorObject;

  private final DateFormat fDateFormat;

  private final boolean fDeleteUnused;
  private final boolean fSkillAddAct, fSkillRemAct, fFilterLinks;
  private final String fAddActTitle, fRemActTitle;
  private final String fNonRefAddVerb;

  private GraphActivityStreamSPI fActivities;

  /**
   * Creates a graph skill service using data from the given neo4j database service, according to
   * the given configuration. Throws a NullPointerException if the given service or configuration
   * object are null.
   *
   * @param database
   *          graph database to use
   * @param config
   *          configuration object to use
   * @param personSPI
   *          person service to use
   * @param impl
   *          implementation utility to use
   */
  public GraphSkillSPI(GraphDatabaseService database, Map<String, String> config,
          GraphPersonSPI personSPI, ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("graph database service was null");
    }
    if (config == null) {
      throw new NullPointerException("configuration object was null");
    }
    if (personSPI == null) {
      throw new NullPointerException("person service was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    this.fDatabase = database;
    this.fPersonSPI = personSPI;

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());

    // whether to delete unlinked skills automatically (global)
    final String autoDelProp = config.get(GraphSkillSPI.AUTODELETE_PROP);
    if (autoDelProp != null) {
      this.fDeleteUnused = Boolean.parseBoolean(autoDelProp);
    } else {
      this.fDeleteUnused = true;
    }

    this.fSkillAddAct = Boolean.parseBoolean(config.get(GraphSkillSPI.GEN_ACTIVITY_ADD));
    this.fSkillRemAct = Boolean.parseBoolean(config.get(GraphSkillSPI.GEN_ACTIVITY_REM));
    this.fFilterLinks = Boolean.parseBoolean(config.get(GraphSkillSPI.FILTER_LINKS));
    this.fAddActTitle = config.get(GraphSkillSPI.ADD_TITLE_PROP);
    this.fRemActTitle = config.get(GraphSkillSPI.REMOVE_TITLE_PROP);
    this.fNonRefAddVerb = config.get(GraphSkillSPI.NON_REF_ADD_VERB_PROP);

    // index
    // the name in lower case is used as the ID and for searching
    this.fSkillNodes = this.fDatabase.index().forNodes(ShindigConstants.SKILL_NODES);

    // activity generator object
    this.fGeneratorObject = new HashMap<String, Object>();
    this.fGeneratorObject.put(OSFields.ID_FIELD, OSFields.SHINDIG_ID);
    this.fGeneratorObject.put(OSFields.OBJECT_TYPE, OSFields.APPLICATION_TYPE);
    this.fGeneratorObject.put(OSFields.DISP_NAME_FIELD, OSFields.SHINDIG_NAME);

    // time stamp formatter
    final TimeZone tz = TimeZone.getTimeZone(OSFields.TIME_ZONE);
    this.fDateFormat = new SimpleDateFormat(OSFields.DATE_FORMAT);
    this.fDateFormat.setTimeZone(tz);
  }

  /**
   * Sets the activitystreams service used to create event-based activities.
   *
   * @param activities
   *          activitystreams service to use
   */
  public void setActivities(GraphActivityStreamSPI activities) {
    this.fActivities = activities;
  }

  /**
   * Retrieves case insensitive skill autocompletion results based on a text fragment. If no
   * fragment is given, all skills will be returned.
   *
   * @param fragment
   *          text fragment to autocomplete
   * @param options
   *          sorting and filtering options
   * @return list of skill autocompletion suggestions
   */
  public ListResult getSkillAutocomp(String fragment, Map<String, Object> options) {
    final List<String> skills = this.fImpl.newList();

    // query for values containing lower case fragment
    final List<Node> nodeList = new ArrayList<Node>();
    String queryString = "*";
    if (fragment != null && !fragment.isEmpty()) {
      queryString += sanitizeFragment(fragment) + "*";
    }

    final IndexHits<Node> qResult = this.fSkillNodes.query(GraphSkillSPI.ID_FIELD, queryString);
    for (final Node node : qResult) {
      nodeList.add(node);
    }

    // filter and sort
    NodeFilter.filterNodes(nodeList, options);
    options.put(WebsockConstants.SORT_FIELD, GraphSkillSPI.NAME_FIELD);
    NodeSorter.sortNodes(nodeList, options);

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = nodeList.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(nodeList.size(), first + max);

    // add skills after filtering and sorting
    Node skillNode = null;
    for (int index = first; index < last; ++index) {
      skillNode = nodeList.get(index);
      skills.add(skillNode.getProperty(GraphSkillSPI.NAME_FIELD).toString());
    }

    final ListResult result = new ListResult(skills);
    result.setFirst(first);
    result.setMax(max);
    result.setTotal(nodeList.size());
    return result;
  }

  // delivers a lower case fragment with syntax character escaped
  private String sanitizeFragment(String fragment) {
    fragment = fragment.toLowerCase();

    // escape special characters
    // backslashes first, as they are used afterwards
    fragment = fragment.replace("\\", "\\\\");

    fragment = fragment.replace("+", "\\+");
    fragment = fragment.replace("-", "\\-");
    fragment = fragment.replace("&", "\\&");
    fragment = fragment.replace("|", "\\|");
    fragment = fragment.replace("!", "\\!");
    fragment = fragment.replace("(", "\\(");
    fragment = fragment.replace(")", "\\)");
    fragment = fragment.replace("{", "\\{");
    fragment = fragment.replace("}", "\\}");
    fragment = fragment.replace("[", "\\[");
    fragment = fragment.replace("]", "\\]");
    fragment = fragment.replace("^", "\\^");
    fragment = fragment.replace("\"", "\\\"");
    fragment = fragment.replace("~", "\\~");
    fragment = fragment.replace("?", "\\?");
    fragment = fragment.replace(":", "\\:");

    // TODO: maybe keep this? - seems to be working anyway
    fragment = fragment.replace("*", "\\*");

    // whitespaces
    fragment = fragment.replace(" ", "\\ ");

    return fragment;
  }

  /**
   * Returns a list of skills linked to the given user ID, including the people that linked him or
   * her to this skill. Sorting is only available for skill names and the number of people linking a
   * skill.
   *
   * @param userId
   *          ID of the user to retrieve linked skills for
   * @param options
   *          sorting and filtering options
   * @return list of linked skills and people linking them
   */
  public ListResult getSkills(String userId, Map<String, Object> options) {
    final List<Map<String, Object>> skills = this.fImpl.newList();
    final Node person = this.fPersonSPI.getPersonNode(userId);

    if (person == null) {
      throw new RuntimeException("person with id '" + userId + "' not found");
    }

    // get all skill link nodes
    final List<Node> nodeList = new ArrayList<Node>();
    final Iterable<Relationship> skillRels = person.getRelationships(ShindigRelTypes.HAS_SKILL,
            Direction.OUTGOING);
    for (final Relationship r : skillRels) {
      nodeList.add(r.getEndNode());
    }

    // filtering/sorting
    filterSkills(nodeList, options);
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null) {
      options.put(WebsockConstants.SORT_FIELD, GraphSkillSPI.NAME_FIELD);
    }

    // normal sorting functions won't work since these nodes are structural
    sortSkills(nodeList, options);

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = nodeList.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(nodeList.size(), first + max);

    // add skills after filtering and sorting
    Node skillNode = null;
    for (int index = first; index < last; ++index) {
      skillNode = nodeList.get(index);

      final GraphSkillSet skillSet = new GraphSkillSet(skillNode, this.fImpl);
      final Map<String, Object> skillSetDTO = skillSet.toMap(null);
      skills.add(skillSetDTO);
    }

    final ListResult result = new ListResult(skills);
    result.setFirst(first);
    result.setMax(max);
    result.setTotal(nodeList.size());
    return result;
  }

  private void filterSkills(List<Node> nodes, Map<String, Object> options) {
    // we can only filter by the name of the skill, in a linked node

    // create map of specific to general skill nodes
    // the general nodes can be filtered, since only they contain a name
    final List<Node> generalNodes = new ArrayList<Node>();
    final Map<Node, Node> specificNodes = new HashMap<Node, Node>();

    Node generalNode = null;
    for (final Node node : nodes) {
      generalNode = node.getRelationships(ShindigRelTypes.IS_SKILL, Direction.OUTGOING).iterator()
              .next().getEndNode();
      generalNodes.add(generalNode);
      specificNodes.put(generalNode, node);
    }

    // filter
    NodeFilter.filterNodes(generalNodes, options);

    // only leave the corresponding specific nodes in the list
    nodes.clear();
    for (final Node node : generalNodes) {
      nodes.add(specificNodes.get(node));
    }
  }

  private void sortSkills(List<Node> nodes, Map<String, Object> options) {
    boolean byName = true;
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField.equals(GraphSkillSPI.PEOPLE_FIELD)) {
      // sort by link numbers
      byName = false;
    }

    final String order = (String) options.get(WebsockConstants.SORT_ORDER);
    boolean ascending = true;
    if (order != null && order.equals(WebsockConstants.DESCENDING)) {
      ascending = false;
    } else if (order != null && !order.equals(WebsockConstants.ASCENDING)) {
      // TODO: log
    }

    // extract values to sort by
    final Map<Node, Object> values = new HashMap<Node, Object>();
    for (final Node node : nodes) {
      if (byName) {
        final Node skill = node.getRelationships(ShindigRelTypes.IS_SKILL, Direction.OUTGOING)
                .iterator().next().getEndNode();

        values.put(node, skill.getProperty(GraphSkillSPI.NAME_FIELD));
      } else {
        int links = 0;

        // TODO: use relationship count in newer Neo4j versions
        final Iterable<Relationship> linkRels = node.getRelationships(ShindigRelTypes.LINKED_BY,
                Direction.OUTGOING);
        final Iterator<Relationship> iter = linkRels.iterator();
        while (iter.hasNext()) {
          ++links;
          iter.next();
        }

        values.put(node, links);
      }
    }

    // use java sorting algorithm using cached values
    Collections.sort(nodes, new Comparator<Node>() {
      @SuppressWarnings("unchecked")
      public int compare(Node o1, Node o2) {
        int value = 0;
        final Object val1 = values.get(o1);
        final Object val2 = values.get(o2);

        if (val1 == null && val2 != null) {
          value = 1;
        } else if (val2 == null) {
          value = -1;
        } else if (val1 instanceof Comparable && val2 instanceof Comparable) {
          value = ((Comparable<Object>) val1).compareTo(val2);
        }

        return value;
      }
    });

    // reverse if necessary
    if (!ascending) {
      Collections.reverse(nodes);
    }
  }

  /**
   * Links a skill to the user with the given ID, noting which user created the link. If a link
   * already exists, the call is ignored. Throws an Exception if any parameter is missing.
   *
   * @param userId
   *          ID of the user to link the skill to
   * @param linker
   *          ID of the user linking the skill
   * @param skill
   *          name of the skill to link
   */
  public void addSkill(String userId, String linker, String skill) {
    boolean genActivity = false;

    final Transaction tx = this.fDatabase.beginTx();
    try {
      boolean linked = false;
      boolean newSkill = false;

      // "person" is linked to the skill by the "linkPerson"
      final Node person = this.fPersonSPI.getPersonNode(userId);
      final Node linkPerson = this.fPersonSPI.getPersonNode(linker);

      // check input
      if (person == null) {
        throw new RuntimeException("person with id '" + userId + "' not found");
      }
      if (linkPerson == null) {
        throw new RuntimeException("linking person with id '" + linker + "' not found");
      }
      if (skill == null || skill.isEmpty()) {
        throw new RuntimeException("missing skill name");
      }

      // global node for the skill
      Node skillNode = this.fSkillNodes.get(GraphSkillSPI.ID_FIELD, skill.toLowerCase())
              .getSingle();

      // create node if it does not exist
      if (skillNode == null) {
        skillNode = this.fDatabase.createNode();
        skillNode.setProperty(GraphSkillSPI.ID_FIELD, skill.toLowerCase());
        skillNode.setProperty(GraphSkillSPI.NAME_FIELD, skill);
        this.fSkillNodes.add(skillNode, GraphSkillSPI.ID_FIELD, skill.toLowerCase());
      }

      // specific node
      final Node linkNode = getOrCreateSkillNode(person, skillNode, skill);

      // check if link already exists, create otherwise
      boolean exists = false;
      int links = 0;
      final Iterable<Relationship> rels = linkNode.getRelationships(ShindigRelTypes.LINKED_BY,
              Direction.OUTGOING);
      for (final Relationship r : rels) {
        ++links;
        if (r.getEndNode().equals(linkPerson)) {
          exists = true;
        }
      }

      if (!exists) {
        linkNode.createRelationshipTo(linkPerson, ShindigRelTypes.LINKED_BY);
        linked = true;
      }
      newSkill = links == 0;

      if (this.fSkillAddAct) {
        if (!this.fFilterLinks || newSkill) {
          genActivity = true;
        }
      }

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      genActivity = false;
      throw new RuntimeException("skill could not be added:\n" + e.getMessage());
    }

    // generate activity
    if (genActivity) {
      skillActivity(linker, userId, skill, true);
    }
  }

  private Node getSkillNode(Node person, String skill) {
    Node node = null;

    skill = skill.toLowerCase();

    // look for linked skills
    final Iterable<Relationship> rels = person.getRelationships(ShindigRelTypes.HAS_SKILL,
            Direction.OUTGOING);
    for (final Relationship r : rels) {
      if (skill.equals(r.getProperty(GraphSkillSPI.ID_FIELD))) {
        node = r.getEndNode();
        break;
      }
    }

    return node;
  }

  private Node getOrCreateSkillNode(Node person, Node skillNode, String skill) {
    // assumes global skill node exists
    Node node = getSkillNode(person, skill);
    skill = skill.toLowerCase();

    // create new skill linking node if not found
    if (node == null) {
      node = this.fDatabase.createNode();

      // person -> skill link
      final Relationship rel = person.createRelationshipTo(node, ShindigRelTypes.HAS_SKILL);
      rel.setProperty(GraphSkillSPI.ID_FIELD, skill);

      // skill link -> global skill node
      node.createRelationshipTo(skillNode, ShindigRelTypes.IS_SKILL);
    }

    return node;
  }

  private boolean deleteAllLinks(Node person, String skill) {
    boolean deleted = false;

    final Node linkNode = getSkillNode(person, skill);

    if (linkNode != null) {
      skill = skill.toLowerCase();

      // delete all relations and the node
      final Iterable<Relationship> rels = linkNode.getRelationships();
      for (final Relationship r : rels) {
        r.delete();
      }
      linkNode.delete();
      deleted = true;
    }

    return deleted;
  }

  private boolean[] deleteSingleLink(Node person, Node linkPerson, String skill) {
    final boolean[] deleted = new boolean[2];

    // delete person specific skill link
    final Node linkNode = getSkillNode(person, skill);
    Relationship linkRel = null;

    if (linkNode != null) {
      final Iterable<Relationship> rels = linkNode.getRelationships(ShindigRelTypes.LINKED_BY,
              Direction.OUTGOING);
      for (final Relationship r : rels) {
        if (r.getEndNode().equals(linkPerson)) {
          linkRel = r;
          break;
        }
      }
    }

    // delete link if it exists
    if (linkRel != null) {
      linkRel.delete();
      deleted[0] = true;

      // delete node if it's not linked anymore
      Iterable<Relationship> rels = linkNode.getRelationships(ShindigRelTypes.LINKED_BY,
              Direction.OUTGOING);

      if (!rels.iterator().hasNext()) {
        // delete all remaining relationships
        rels = linkNode.getRelationships();

        for (final Relationship r : rels) {
          r.delete();
        }

        // delete node
        linkNode.delete();
        deleted[1] = true;
      }
    }

    return deleted;
  }

  /**
   * Removes the skill link between the user with the given ID and the given skill specific to the
   * linking person, specified by the linker ID. If the link to delete does not exist, the call is
   * ignored. If globally nobody is linked to a skill anymore, the skill will be removed by default,
   * which can be changed through the property "skills.unused.autodelete".
   *
   * @param userId
   *          ID of the person to delete a skill link from
   * @param linker
   *          ID of the linking person to delete a skill link for
   * @param skill
   *          name of the skill to be unlinked
   */
  public void removeSkill(String userId, String linker, String skill) {
    boolean genActivity = false;

    final Transaction tx = this.fDatabase.beginTx();
    try {
      // "person" is linked to the skill by the "linkPerson"
      final Node person = this.fPersonSPI.getPersonNode(userId);
      final Node linkPerson = this.fPersonSPI.getPersonNode(linker);

      boolean deleted = false;
      boolean removedCompletely = false;

      // check input
      if (person == null) {
        throw new RuntimeException("person with id '" + userId + "' not found");
      }
      if (linkPerson == null) {
        throw new RuntimeException("linking person with id '" + linker + "' not found");
      }
      if (skill == null || skill.isEmpty()) {
        throw new RuntimeException("missing skill name");
      }

      // global node for the skill
      final Node skillNode = this.fSkillNodes.get(GraphSkillSPI.ID_FIELD, skill.toLowerCase())
              .getSingle();

      // check if the person is deleting a skill from themselves
      if (person.equals(linkPerson)) {
        // delete all links for this skill
        deleted = deleteAllLinks(person, skill);
        removedCompletely = deleted;
      } else {
        // delete skill link for the linking person
        final boolean[] dels = deleteSingleLink(person, linkPerson, skill);
        deleted = dels[0];
        removedCompletely = dels[1];
      }

      // check if global skill is not used anymore
      if (this.fDeleteUnused && skillNode != null) {
        final Iterable<Relationship> rels = skillNode.getRelationships();

        if (!rels.iterator().hasNext()) {
          skillNode.delete();

          // node is automatically deleted from index
        }
      }

      if (this.fSkillRemAct && deleted) {
        if (!this.fFilterLinks || removedCompletely) {
          genActivity = true;
        }
      }

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      genActivity = false;
      throw new RuntimeException("skill could not be removed:\n" + e.getMessage());
    }

    // generate activity
    if (genActivity) {
      skillActivity(linker, userId, skill, false);
    }
  }

  /**
   * Retrieves all people that are linked to skills matching the supplied skill fragment.
   *
   * @param skill
   *          skill to find people for
   * @return set of nodes with the people matching the request
   */
  public Set<Node> getPersonNodesForSkill(String skill) {
    if (skill == null || skill.isEmpty()) {
      throw new RuntimeException("no skill was supplied");
    }

    // set for deduplication
    final Set<Node> personNodeSet = new HashSet<Node>();

    // get matching skill nodes
    final List<Node> skillNodeList = new ArrayList<Node>();
    final String queryString = "*" + sanitizeFragment(skill) + "*";
    final IndexHits<Node> qResult = this.fSkillNodes.query(GraphSkillSPI.ID_FIELD, queryString);
    for (final Node node : qResult) {
      skillNodeList.add(node);
    }

    // TODO: option for only perfect matches?

    // retrieve people linked to all matches
    for (final Node skillNode : skillNodeList) {
      // get skill link nodes
      final List<Node> linkNodes = new ArrayList<Node>();
      Iterable<Relationship> linkedRels = skillNode.getRelationships(ShindigRelTypes.IS_SKILL,
              Direction.INCOMING);
      for (final Relationship rel : linkedRels) {
        linkNodes.add(rel.getStartNode());
      }

      // collect all linked people
      for (final Node linkNode : linkNodes) {
        linkedRels = linkNode.getRelationships(ShindigRelTypes.HAS_SKILL, Direction.INCOMING);
        for (final Relationship rel : linkedRels) {
          personNodeSet.add(rel.getStartNode());
        }
      }
    }

    return personNodeSet;
  }

  /**
   * Retrieves all people that are linked to skills matching the supplied skill fragment.
   *
   * @param skill
   *          skill to find people for
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list result with all people linked to the skill
   */
  public ListResult getPeopleBySkill(String skill, Map<String, Object> options, List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }

    final List<Map<String, Object>> personList = this.fImpl.newList();

    // TODO: visibility?

    // retrieve person nodes
    final Set<Node> personNodeSet = getPersonNodesForSkill(skill);

    // TODO: alternatively sort by number of links?

    // create a list to add sortability
    final List<Node> nodeList = new ArrayList<Node>(personNodeSet);

    // create a sorted list as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null || sortField.equals(GraphSkillSPI.NAME_FIELD)) {
      options.put(WebsockConstants.SORT_FIELD, GraphSkillSPI.FORMATTED_FIELD);
    }
    NodeSorter.sortNodes(nodeList, options);

    Node personNode = null;
    Map<String, Object> tmpPerson = null;

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = nodeList.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(nodeList.size(), first + max);

    // convert the items requested
    for (int index = first; index < last; ++index) {
      personNode = nodeList.get(index);
      tmpPerson = new GraphPerson(personNode, this.fImpl).toMap(fieldSet);

      personList.add(tmpPerson);
    }

    // return search query information
    final ListResult people = new ListResult(personList);
    people.setFirst(first);
    people.setMax(max);
    people.setTotal(nodeList.size());

    return people;
  }

  private void skillActivity(String userId, String otherUserId, String skill, boolean add) {
    final Map<String, Object> activity = new HashMap<String, Object>();
    final boolean reflective = userId.equals(otherUserId);

    // actor: user adding/removing the tag
    Node person = this.fPersonSPI.getPersonNode(userId);
    String userName = person.getProperty(OSFields.DISP_NAME_FIELD, userId).toString();
    final Map<String, Object> actor = new HashMap<String, Object>();
    actor.put(OSFields.ID_FIELD, userId);
    actor.put(OSFields.DISP_NAME_FIELD, userName);
    actor.put(OSFields.OBJECT_TYPE, OSFields.PERSON_TYPE);
    activity.put(OSFields.ACTOR_FIELD, actor);

    if (!reflective) {
      // target: other user
      person = this.fPersonSPI.getPersonNode(otherUserId);
      userName = person.getProperty(OSFields.DISP_NAME_FIELD, otherUserId).toString();
      final Map<String, Object> target = new HashMap<String, Object>();
      target.put(OSFields.ID_FIELD, otherUserId);
      target.put(OSFields.DISP_NAME_FIELD, userName);
      target.put(OSFields.OBJECT_TYPE, OSFields.PERSON_TYPE);
      activity.put(OSFields.TARGET_FIELD, target);
    }

    // object = skill being added
    final Map<String, Object> object = new HashMap<String, Object>();
    object.put(OSFields.ID_FIELD, skill.toLowerCase());
    object.put(OSFields.DISP_NAME_FIELD, "Skill '" + skill + "'");
    object.put(OSFields.OBJECT_TYPE, GraphSkillSPI.SKILL_OBJ_TYPE);
    activity.put(OSFields.OBJECT_FIELD, object);

    // verb and title
    if (add) {
      if (reflective) {
        activity.put(OSFields.VERB_FIELD, GraphSkillSPI.VERB_ADD);
      } else {
        activity.put(OSFields.VERB_FIELD, this.fNonRefAddVerb);
      }
      activity.put(OSFields.TITLE_FIELD, this.fAddActTitle);
    } else {
      activity.put(OSFields.VERB_FIELD, GraphSkillSPI.VERB_REMOVE);
      activity.put(OSFields.TITLE_FIELD, this.fRemActTitle);
    }

    // generator
    activity.put(OSFields.GENERATOR_FIELD, this.fGeneratorObject);

    // generate time stamp
    final String timestamp = this.fDateFormat.format(new Date(System.currentTimeMillis()));
    activity.put(OSFields.ACT_PUBLISHED_FIELD, timestamp);

    try {
      this.fActivities.createActivityEntry(userId, null, OSFields.SHINDIG_ID, activity, null);
    } catch (final Exception e) {
      // don't fail, exception is already logged in the activity service
    }
  }
}
