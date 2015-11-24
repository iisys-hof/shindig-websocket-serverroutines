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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphGroup;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphPerson;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Neo4j-related implementation of the graph service interface using the person service to acquire
 * person data and traverse friendships.
 */
public class GraphSPI {
  private static final int MAX_FOF_DEPTH = 10;
  private static final int MAX_SPATH_DEPTH = 10;

  private static final String NAME_FIELD = "name";
  private static final String FORMATTED_FIELD = "formatted";

  private static final PathExpander<Path> PATH_EXP = Traversal.pathExpanderForTypes(
          ShindigRelTypes.FRIEND_OF, Direction.OUTGOING);

  private static final PathFinder<Path> SPATH = GraphAlgoFactory.shortestPath(GraphSPI.PATH_EXP,
          GraphSPI.MAX_SPATH_DEPTH);

  private static final Comparator<Entry<Node, Integer>> SUGG_COMP = new Comparator<Entry<Node, Integer>>() {
    @Override
    public int compare(Entry<Node, Integer> arg0, Entry<Node, Integer> arg1) {
      return arg1.getValue().compareTo(arg0.getValue());
    }
  };

  private final GraphPersonSPI fPersonSPI;

  private final Map<Integer, TraversalDescription> fFofTravs;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  /**
   * Creates a GraphSPI traversing friendships of the people provided by the given person service.
   * Throws a NullPointerException if the given person service is null.
   *
   * @param personSPI
   *          person service to take people from
   * @param listClass
   *          list implementation to use
   * @param mapClass
   *          map implementation to use
   */
  public GraphSPI(GraphPersonSPI personSPI, ImplUtil impl) {
    if (personSPI == null) {
      throw new NullPointerException("person service was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    this.fPersonSPI = personSPI;
    this.fFofTravs = new HashMap<Integer, TraversalDescription>();

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  private ListResult convertRequested(final List<Node> people, final Set<String> fields,
          Map<String, Object> options) {
    final List<Map<String, Object>> dtos = this.fImpl.newList();

    // return empty response if no people available
    if (people == null) {
      final ListResult peopleColl = new ListResult(dtos);
      return peopleColl;
    }

    GraphPerson gPerson = null;
    Map<String, Object> tmpPerson = null;

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = people.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(people.size(), first + max);

    // convert the items requested
    for (int index = first; index < last; ++index) {
      gPerson = new GraphPerson(people.get(index), this.fImpl);
      tmpPerson = gPerson.toMap(fields);

      dtos.add(tmpPerson);
    }

    final ListResult peopleColl = new ListResult(dtos);
    peopleColl.setFirst(first);
    peopleColl.setMax(max);
    peopleColl.setTotal(people.size());
    return peopleColl;
  }

  private TraversalDescription getFofTrav(int depth) {
    TraversalDescription trav = this.fFofTravs.get(depth);

    if (trav == null) {
      trav = Traversal.description().breadthFirst()
              .relationships(ShindigRelTypes.FRIEND_OF, Direction.OUTGOING)
              .evaluator(Evaluators.includingDepths(1, depth)).uniqueness(Uniqueness.NODE_GLOBAL);

      this.fFofTravs.put(depth, trav);
    }

    return trav;
  }

  /**
   * Retrieves a list of friends of friends up to a certain depth, optionally only people the people
   * for whom the request is made are not yet friends with.
   *
   * @param userIds
   *          list of people to retrieve friends of friends for
   * @param depth
   *          traversal depth in the friend graph
   * @param unknown
   *          whether to only retrieve unknown people
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of friends of friends
   */
  public ListResult getFriendsOfFriends(List<String> userIds, int depth, boolean unknown,
          Map<String, Object> options, List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final Set<Node> people = new HashSet<Node>();
    final List<Node> requested = new ArrayList<Node>();

    if (depth > GraphSPI.MAX_FOF_DEPTH) {
      depth = GraphSPI.MAX_FOF_DEPTH;
    }

    // breadth first traversal through friend graph
    Node person = null;

    TraversalDescription trav = getFofTrav(depth);

    for (final String id : userIds) {
      person = this.fPersonSPI.getPersonNode(id);
      requested.add(person);

      for (final Node node : trav.traverse(person).nodes()) {
        people.add(node);
      }
    }

    // remove first-level friends if desired
    if (unknown) {
      final Set<Node> knownNodes = new HashSet<Node>();
      trav = getFofTrav(1);

      for (final String id : userIds) {
        person = this.fPersonSPI.getPersonNode(id);

        for (final Node node : trav.traverse(person).nodes()) {
          knownNodes.add(node);
        }
      }

      people.removeAll(knownNodes);
    }

    // remove the people from the request
    people.removeAll(requested);

    // filter
    final List<Node> peopleList = new ArrayList<Node>(people);
    NodeFilter.filterNodes(peopleList, options);
    // TODO: other filters?

    // sort as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null || sortField.equals(GraphSPI.NAME_FIELD)) {
      options.put(WebsockConstants.SORT_FIELD, GraphSPI.FORMATTED_FIELD);
    }
    NodeSorter.sortNodes(peopleList, options);

    // convert all requested
    return convertRequested(peopleList, fieldSet, options);
  }

  /**
   * Returns an ordered list of people forming the shortest path formed by friendship relations
   * between two users or an empty list if they are identical.
   *
   * @param userId
   *          user ID marking the beginning of the path
   * @param targetId
   *          user ID marking the end of the path
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return shortest path between two people
   */
  public ListResult getShortestPath(String userId, String targetId, Map<String, Object> options,
          List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Node target = this.fPersonSPI.getPersonNode(targetId);

    List<Node> people = null;

    if (!person.equals(target)) {
      final Path path = GraphSPI.SPATH.findSinglePath(person, target);

      if (path != null) {
        people = new ArrayList<Node>();
        for (final Node n : path.nodes()) {
          people.add(n);
        }
      }
    }

    // filter
    NodeFilter.filterNodes(people, options);
    // TODO: other filters?

    // sort if defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField != null) {
      NodeSorter.sortNodes(people, options);
    }

    // convert all requested
    return convertRequested(people, fieldSet, options);
  }

  private Map<Node, Integer> getFriendMemberships(Node person) {
    final Map<Node, Integer> memCounts = new HashMap<Node, Integer>();
    final Iterable<Relationship> friendRels = person.getRelationships(Direction.OUTGOING,
            ShindigRelTypes.FRIEND_OF);

    Iterable<Relationship> memRels = null;
    Integer count = null;

    Node friend = null;
    Node group = null;
    for (final Relationship fRel : friendRels) {
      friend = fRel.getEndNode();
      memRels = friend.getRelationships(ShindigRelTypes.MEMBER_OF);

      for (final Relationship mRel : memRels) {
        group = mRel.getEndNode();

        count = memCounts.get(group);
        if (count == null) {
          count = 1;
        } else {
          ++count;
        }

        memCounts.put(group, count);
      }
    }

    return memCounts;
  }

  private List<Node> getGroupSuggNodes(String userId, final int number) {
    final List<Entry<Node, Integer>> sortGroups = new ArrayList<Map.Entry<Node, Integer>>();

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Map<Node, Integer> memCounts = getFriendMemberships(person);

    // remove groups the user is already a member of
    final Iterable<Relationship> memRels = person.getRelationships(ShindigRelTypes.MEMBER_OF);
    for (final Relationship mRel : memRels) {
      memCounts.remove(mRel.getEndNode());
    }

    // sort fitting entries into list
    int value = 0;
    for (final Entry<Node, Integer> groupE : memCounts.entrySet()) {
      value = groupE.getValue();

      // only add if sufficient friends are in
      if (value >= number) {
        sortGroups.add(groupE);
      }
    }
    Collections.sort(sortGroups, GraphSPI.SUGG_COMP);

    // add available groups
    final List<Node> groups = new ArrayList<Node>(sortGroups.size());
    for (final Entry<Node, Integer> groupE : sortGroups) {
      groups.add(groupE.getKey());
    }

    return groups;
  }

  /**
   * Returns a list of group suggestions, ordered by the number of friends who are a member of these
   * groups.
   *
   * @param userId
   *          user to get group recommendations for
   * @param number
   *          minimum number of friends in a suggested group
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of recommended groups
   */
  public ListResult getGroupRecommendation(String userId, int number, Map<String, Object> options,
          List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final List<Node> groups = getGroupSuggNodes(userId, number);

    // filter
    NodeFilter.filterNodes(groups, options);
    // TODO: other filters?

    // sort if defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField != null) {
      NodeSorter.sortNodes(groups, options);
    }

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = groups.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(groups.size(), first + max);

    // convert the items requested
    final List<Map<String, Object>> groupList = this.fImpl.newList();

    Map<String, Object> dto = null;
    for (int index = first; index < last; ++index) {
      dto = new GraphGroup(groups.get(index), this.fImpl).toMap(fieldSet);
      groupList.add(dto);
    }

    final ListResult groupColl = new ListResult(groupList);
    groupColl.setFirst(first);
    groupColl.setMax(max);
    groupColl.setTotal(groups.size());
    return groupColl;
  }

  private void addFriendCounts(Node person, final Map<Node, Integer> counts) {
    // add up friendships for all friends
    final Iterable<Relationship> friendRels = person.getRelationships(Direction.OUTGOING,
            ShindigRelTypes.FRIEND_OF);

    Node friend = null;
    Integer count = null;
    for (final Relationship rel : friendRels) {
      friend = rel.getEndNode();

      count = counts.get(friend);
      if (count == null) {
        count = 1;
      } else {
        ++count;
      }
      counts.put(friend, count);
    }
  }

  private List<Node> getFriendSuggNodes(String userId, final int number) {
    final Node person = this.fPersonSPI.getPersonNode(userId);

    // return nothing if the person does not exist
    if (person == null) {
      return null;
    }

    final Map<Node, Integer> friendCounts = new HashMap<Node, Integer>();
    final List<Entry<Node, Integer>> sortFriends = new ArrayList<Map.Entry<Node, Integer>>();

    // add up friendships for all friends of friends
    Iterable<Relationship> friendRels = person.getRelationships(Direction.OUTGOING,
            ShindigRelTypes.FRIEND_OF);
    Node friend = null;
    for (final Relationship rel : friendRels) {
      friend = rel.getEndNode();
      addFriendCounts(friend, friendCounts);
    }

    // remove the people the person is already friends with and self
    friendRels = person.getRelationships(Direction.OUTGOING, ShindigRelTypes.FRIEND_OF);
    for (final Relationship rel : friendRels) {
      friendCounts.remove(rel.getEndNode());
    }
    friendCounts.remove(person);

    // sort into list
    int value = 0;
    for (final Entry<Node, Integer> friendE : friendCounts.entrySet()) {
      value = friendE.getValue();

      // only add if sufficient friends are registered
      if (value >= number) {
        sortFriends.add(friendE);
      }
    }
    Collections.sort(sortFriends, GraphSPI.SUGG_COMP);

    // add available friends
    final List<Node> friends = new ArrayList<Node>(sortFriends.size());
    for (final Entry<Node, Integer> friendE : sortFriends) {
      friends.add(friendE.getKey());
    }

    return friends;
  }

  /**
   * Retrieves a list of suggested friends, ordered by the number of common friends the user and
   * these people have in common.
   *
   * @param userId
   *          user to retrieve friends suggestions for
   * @param number
   *          minimum number of common friends
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of suggested friends
   */
  public ListResult getFriendRecommendation(String userId, int number, Map<String, Object> options,
          List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final List<Node> friends = getFriendSuggNodes(userId, number);

    // stop processing if retrieval failed
    if (friends == null) {
      return null;
    }

    // filter
    NodeFilter.filterNodes(friends, options);
    // TODO: other filters?

    // sort if defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField != null) {
      NodeSorter.sortNodes(friends, options);
    }

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = friends.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(friends.size(), first + max);

    // convert the items requested
    List<Map<String, Object>> peopleList;
    try {
      peopleList = this.fImpl.newList();
    } catch (final Exception e) {
      e.printStackTrace();
      this.fLogger.log(Level.SEVERE, "could not instantiate list", e);
      throw new RuntimeException(e);
    }

    Map<String, Object> dto = null;

    for (int index = first; index < last; ++index) {
      dto = new GraphPerson(friends.get(index), this.fImpl).toMap(fieldSet);

      peopleList.add(dto);
    }

    final ListResult peopleColl = new ListResult(peopleList);
    peopleColl.setFirst(first);
    peopleColl.setMax(max);
    peopleColl.setTotal(friends.size());
    return peopleColl;
  }
}
