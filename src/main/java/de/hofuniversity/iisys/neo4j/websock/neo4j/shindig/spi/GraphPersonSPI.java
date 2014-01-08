/*
 * Copyright (c) 2012-2013 Institute of Information Systems, Hof University
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphPerson;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.PersonFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the person service retrieving person data from the Neo4j graph database.
 */
public class GraphPersonSPI {
  private static final String ANONYMOUS_ID = "-1";

  private static final String ID_FIELD = "id";
  private static final String NAME_FIELD = "name";
  private static final String FORMATTED_FIELD = "formatted";
  private static final String UPDATED_FIELD = "updated";

  private final GraphDatabaseService fDatabase;

  private final Index<Node> fPersonNodes, fGroupNodes;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  /**
   * Creates a graph person service using data from the given neo4j database service, according to
   * the given configuration. Throws a NullPointerException if the given service or configuration
   * object are null.
   *
   * @param database
   *          graph database to use
   * @param config
   *          configuration object to use
   * @param impl
   *          implementation utility to use
   */
  public GraphPersonSPI(GraphDatabaseService database, GraphConfig config, ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("database service was null");
    }
    if (config == null) {
      throw new NullPointerException("configuration object was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    this.fDatabase = database;
    this.fPersonNodes = this.fDatabase.index().forNodes(ShindigConstants.PERSON_NODES);
    this.fGroupNodes = this.fDatabase.index().forNodes(ShindigConstants.GROUP_NODES);

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  /**
   * Tries to retrieve the person with the given id from the database. Returns null if there is no
   * such person.
   *
   * @param id
   *          id of the user to retrieve
   * @return user node or null
   */
  public Node getPersonNode(String id) {
    Node personNode = null;

    try {
      if (id != null) {
        final IndexHits<Node> matching = this.fPersonNodes.get(GraphPersonSPI.ID_FIELD, id);
        personNode = matching.getSingle();
        matching.close();
      }
    } catch (final NoSuchElementException e) {
      e.printStackTrace();
    }

    return personNode;
  }

  /**
   * Tries to retrieve the person with the given id from the database and creates a data map
   * containing the requested fields. An empty set of fields implies all. Returns null if there is
   * no such person.
   *
   * @param id
   *          id of the person to retrieve
   * @param fields
   *          fields to retrieve
   * @return person data map with requested fields or null
   */
  public Map<String, Object> getPersonMap(String id, Set<String> fields) {
    Map<String, Object> person = null;
    final Node personNode = getPersonNode(id);

    if (personNode != null) {
      person = convertPerson(personNode, fields);
    }

    return person;
  }

  private Map<String, Object> convertPerson(Node person, Set<String> fields) {
    final Map<String, Object> dto = new GraphPerson(person, this.fImpl).toMap(fields);

    // removed URL generation

    return dto;
  }

  /**
   * Tries to retrieve the group with the given id from the database. Returns null if there is no
   * such group.
   *
   * @param groupId
   *          id of the group to retrieve
   * @return group node or null
   * @throws ProtocolException
   *           if there is more than one group with that id
   */
  public Node getGroupNode(String groupId) {
    Node groupNode = null;

    final IndexHits<Node> matching = this.fGroupNodes.get(GraphPersonSPI.ID_FIELD, groupId);
    try {
      groupNode = matching.getSingle();
    } catch (final NoSuchElementException e) {
      e.printStackTrace();
    }
    matching.close();

    return groupNode;
  }

  private Set<Node> getPeopleNodes(Set<String> userIds, String groupId) {
    Set<Node> people = null;

    if (groupId == null || groupId.equals(OSFields.GROUP_TYPE_SELF)) {
      people = new HashSet<Node>();
      for (final String id : userIds) {
        people.add(getPersonNode(id));
      }
    } else {
      switch (groupId) {
      case OSFields.GROUP_TYPE_ALL:
        people = getEndPoints(userIds, Neo4jRelTypes.KNOWS, Neo4jRelTypes.FRIEND_OF);
        break;

      case OSFields.GROUP_TYPE_FRIENDS:
        people = getEndPoints(userIds, Neo4jRelTypes.FRIEND_OF);
        break;

      default:
        people = getGroupMemberNodes(groupId);
        break;
      }
    }

    return people;
  }

  private Set<Node> getEndPoints(Set<String> userIds, Neo4jRelTypes... types) {
    final Set<Node> people = new HashSet<Node>();

    Node startNode = null;
    Iterable<Relationship> relations = null;

    for (final String id : userIds) {
      startNode = getPersonNode(id);

      if (startNode != null) {
        relations = startNode.getRelationships(Direction.OUTGOING, types);

        for (final Relationship rel : relations) {
          people.add(rel.getEndNode());
        }
      }
    }

    return people;
  }

  /**
   * Tries to retrieve all nodes representing members of the group specified by the given group ID.
   * If there are no members or there is no such group an empty list is returned.
   *
   * @param groupId
   *          id object of the group in question
   * @return person nodes for all group members
   */
  public Set<Node> getGroupMemberNodes(String groupId) {
    final Set<Node> people = new HashSet<Node>();
    Node group = null;

    group = getGroupNode(groupId);

    if (group != null) {
      final Iterable<Relationship> relations = group.getRelationships(Direction.INCOMING,
              Neo4jRelTypes.MEMBER_OF);

      for (final Relationship rel : relations) {
        people.add(rel.getStartNode());
      }
    }

    return people;
  }

  /**
   * Retrieves a list of people, based on an initial ID list a a group parameter.
   *
   * @param userIds
   *          IDs of users to retrieve
   * @param groupId
   *          group of users to retrieve
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of retrieved people
   */
  public ListResult getPeople(List<String> userIds, String groupId, Map<String, Object> options,
          List<String> fields) {
    final Set<String> idSet = new HashSet<String>(userIds);
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }

    final List<Map<String, Object>> personList = this.fImpl.newList();

    // retrieve people
    final Set<Node> targets = getPeopleNodes(idSet, groupId);
    final List<Node> nodeList = new ArrayList<Node>(targets);

    // filter
    // TODO: filter is not recognized yet
    if ("isFriendsWith".equals(options.get(WebsockConstants.FILTER_OPERATION))) {
      final String filterId = (String) options.get(WebsockConstants.FILTER_VALUE);
      final Node filterNode = getPersonNode(filterId);

      PersonFilter.filterNodes(nodeList, filterNode);
    } else {
      NodeFilter.filterNodes(nodeList, options);
    }
    // TODO: other filters?

    // create a sorted list as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null || sortField.equals(GraphPersonSPI.NAME_FIELD)) {
      options.put(WebsockConstants.SORT_FIELD, GraphPersonSPI.FORMATTED_FIELD);
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
      tmpPerson = convertPerson(personNode, fieldSet);

      personList.add(tmpPerson);
    }

    // return search query information
    final ListResult people = new ListResult(personList);
    people.setFirst(first);
    people.setMax(max);
    people.setTotal(nodeList.size());

    return people;
  }

  /**
   * Retrieves a single person.
   *
   * @param id
   *          ID of the person to retrieve
   * @param fields
   *          fields to retrieve
   * @return single person result
   */
  public SingleResult getPerson(String id, List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    Map<String, Object> person = null;

    // check if it's the anonymous user
    if (id != null && GraphPersonSPI.ANONYMOUS_ID.equals(id)) {
      // implemented in client
      // TODO: cause proper error
      throw new RuntimeException("anonymous user not supported");
    }
    // check database for user with this ID
    else {
      final Node personNode = getPersonNode(id);

      if (personNode == null) {
        // TODO
        throw new RuntimeException("Person '" + id + "' not found");
      }

      person = convertPerson(personNode, fieldSet);
    }

    return new SingleResult(person);
  }

  private void updateExternal(Node person, Map<String, Object> profile) {
    /*
     * update externally stored information, only writable through person object
     */

    // TODO: how to handle correctly? read-only for the moment

    // List<Account> accounts = profile.getAccounts();
    // if(accounts != null)
    // {
    // for(Account acc : accounts)
    // {
    //
    // }
    // }
  }

  /**
   * Updates a person in the database based on the given data.
   *
   * @param id
   *          ID of the user to update
   * @param person
   *          person data to set
   * @return resulting person object result
   */
  public SingleResult updatePerson(String id, Map<String, Object> person) {
    Map<String, Object> resultPerson = null;

    // String viewer = token.getViewerId(); // viewer
    // String user = id.getUserId(token); // person to update
    //
    // //check permissions
    // if(!viewer.equals(user))
    // {
    // throw new ProtocolException(HttpServletResponse.SC_FORBIDDEN,
    // "User '" + viewer + "' does not have enough privileges " +
    // "to update person '" + user + "'");
    // }

    // get person node
    final Node personNode = getPersonNode(id);
    if (personNode == null) {
      // TODO
      throw new RuntimeException("Person '" + id + "' not found");
    }

    // set time stamp
    person.put(GraphPersonSPI.UPDATED_FIELD, System.currentTimeMillis());

    // write changes
    final Transaction trans = this.fDatabase.beginTx();
    try {
      final GraphPerson gPerson = new GraphPerson(personNode);

      // update real properties
      gPerson.setData(person);

      // update properties in the person's linked nodes
      gPerson.updateRelationships(person);

      // update relations to shared nodes
      updateExternal(personNode, person);

      resultPerson = convertPerson(personNode, null);

      trans.success();
      trans.finish();
    } catch (final Exception e) {
      trans.failure();
      trans.finish();
      e.printStackTrace();
      throw new RuntimeException("could not update person:\n" + e.getMessage());
    }

    return new SingleResult(resultPerson);
  }

  // additional methods

  /**
   * Retrieves a list of all visible people.
   *
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list result with all people
   */
  public ListResult getAllPeople(Map<String, Object> options, List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }

    final List<Map<String, Object>> personList = this.fImpl.newList();

    final List<Node> nodeList = new ArrayList<Node>();

    // TODO: visibility?

    // retrieve all person nodes
    final IndexHits<Node> result = this.fPersonNodes.query(GraphPersonSPI.ID_FIELD, "*");
    for (final Node node : result) {
      nodeList.add(node);
    }

    // filter
    if (options != null && options.get(WebsockConstants.FILTER_OPERATION) != null) {
      if (options.get(WebsockConstants.FILTER_OPERATION).equals(OSFields.GROUP_TYPE_ALL)) {
        PersonFilter.filterNodes(nodeList, (String) options.get(WebsockConstants.FILTER_VALUE));
      } else {
        NodeFilter.filterNodes(nodeList, options);
      }
    }
    // TODO: other filters?

    // create a sorted list as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null || sortField.equals(GraphPersonSPI.NAME_FIELD)) {
      options.put(WebsockConstants.SORT_FIELD, GraphPersonSPI.FORMATTED_FIELD);
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
      tmpPerson = convertPerson(personNode, fieldSet);

      personList.add(tmpPerson);
    }

    // return search query information
    final ListResult people = new ListResult(personList);
    people.setFirst(first);
    people.setMax(max);
    people.setTotal(nodeList.size());

    return people;
  }

  /**
   * Creates a person based on the data given.
   *
   * @param person
   *          person data to store
   * @return resulting person
   */
  public SingleResult createPerson(final Map<String, Object> person) {
    Map<String, Object> resultPerson = null;

    final String personId = (String) person.get(GraphPersonSPI.ID_FIELD);

    // check if person already exists
    Node node = this.fPersonNodes.get(GraphPersonSPI.ID_FIELD, personId).getSingle();
    if (node != null) {
      // TODO
      throw new RuntimeException("User with ID '" + personId + "' already exists");
    }

    // set time stamp
    person.put(GraphPersonSPI.UPDATED_FIELD, System.currentTimeMillis());

    // create person object
    final Transaction trans = this.fDatabase.beginTx();
    try {
      node = this.fDatabase.createNode();
      node.setProperty(GraphPersonSPI.ID_FIELD, personId);
      this.fPersonNodes.add(node, GraphPersonSPI.ID_FIELD, personId);

      // set properties and relations
      final GraphPerson gPerson = new GraphPerson(node);

      // update real properties
      gPerson.setData(person);

      // update properties in the person's linked nodes
      gPerson.updateRelationships(person);

      // update relations to shared nodes
      updateExternal(node, person);

      resultPerson = convertPerson(node, null);

      trans.success();
      trans.finish();
    } catch (final Exception e) {
      trans.failure();
      trans.finish();
      e.printStackTrace();
      throw new RuntimeException("could not create person:\n" + e.getMessage());
    }

    // TODO: hooks to create other required nodes, for example initial message collections

    return new SingleResult(resultPerson);
  }

  /**
   * Deletes or deactivates the person with the given ID.
   *
   * @param id
   *          ID of the user to delete
   */
  public void deletePerson(String id) {
    // check if person exists
    final Node node = this.fPersonNodes.get(GraphPersonSPI.ID_FIELD, id).getSingle();
    if (node == null) {
      // TODO
      throw new RuntimeException("User with ID '" + id + "' does not exist");
    }

    final Transaction trans = this.fDatabase.beginTx();
    try {
      // TODO: actually delete or deactivate user
      trans.success();
      trans.finish();
    } catch (final Exception e) {
      trans.failure();
      trans.finish();
      e.printStackTrace();
    }
  }
}
