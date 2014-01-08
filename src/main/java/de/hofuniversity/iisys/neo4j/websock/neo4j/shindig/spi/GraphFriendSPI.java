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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphPerson;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Native Neo4j implementation for the friendship service.
 */
public class GraphFriendSPI {
  private static final String NAME_FIELD = "name";
  private static final String FORMATTED_FIELD = "formatted";

  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  /**
   * Creates a friendship service using people from the given person service. Throws a
   * NullPointerException if any parameter is null.
   *
   * @param database
   *          database service to use
   * @param personSPI
   *          person service to use
   * @param impl
   *          implementation utility to use
   */
  public GraphFriendSPI(GraphDatabaseService database, GraphPersonSPI personSPI, ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("graph database service was null");
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
  }

  private List<Node> getRequestPeople(String id) {
    final Node person = this.fPersonSPI.getPersonNode(id);
    final List<Node> nodeList = new ArrayList<Node>();

    if (person != null) {
      final Iterable<Relationship> reqs = person.getRelationships(Direction.INCOMING,
              Neo4jRelTypes.FRIEND_REQUEST);

      for (final Relationship req : reqs) {
        nodeList.add(req.getStartNode());
      }
    }

    return nodeList;
  }

  /**
   * Retrieves a list of users from whom the given user has received friendship requests.
   *
   * @param userId
   *          user to retrieve requests for
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of users who requested a friendship
   */
  public ListResult getRequests(String userId, Map<String, Object> options, List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }

    final List<Map<String, Object>> personList = this.fImpl.newList();

    final List<Node> nodeList = getRequestPeople(userId);

    // filter
    NodeFilter.filterNodes(nodeList, options);
    // TODO: other filters?

    // create a sorted list as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null || sortField.equals(GraphFriendSPI.NAME_FIELD)) {
      options.put(WebsockConstants.SORT_FIELD, GraphFriendSPI.FORMATTED_FIELD);
    }
    NodeSorter.sortNodes(nodeList, options);

    GraphPerson gPerson = null;
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
      gPerson = new GraphPerson(nodeList.get(index), this.fImpl);
      tmpPerson = gPerson.toMap(fieldSet);

      personList.add(tmpPerson);
    }

    // return search query information
    final ListResult people = new ListResult(personList);
    people.setFirst(first);
    people.setMax(max);
    people.setTotal(nodeList.size());

    return people;
  }

  private List<Relationship> areFriends(Node user, Node target) {
    final List<Relationship> friendships = new ArrayList<Relationship>();

    // get all possible friendship relations (unidirectional -> two)
    final Iterable<Relationship> fRels = user.getRelationships(Neo4jRelTypes.FRIEND_OF);
    for (final Relationship rel : fRels) {
      if (rel.getOtherNode(user).equals(target)) {
        friendships.add(rel);
      }
    }

    return friendships;
  }

  private Relationship hasRequest(Node user, Node target) {
    Relationship request = null;

    // get incoming requests, look for matching
    final Iterable<Relationship> rRels = user.getRelationships(Direction.INCOMING,
            Neo4jRelTypes.FRIEND_REQUEST);
    for (final Relationship rel : rRels) {
      if (rel.getStartNode().equals(target)) {
        request = rel;
        break;
      }
    }

    return request;
  }

  /**
   * Requests a friendship for the first user ID from the user with the second user ID or confirms a
   * request from it.
   *
   * @param userId
   *          ID of the requesting user
   * @param targetId
   *          ID of the user to send the request to
   */
  public void requestFriendship(String userId, String targetId) {
    final Node user = this.fPersonSPI.getPersonNode(userId);
    final Node friend = this.fPersonSPI.getPersonNode(targetId);

    if (user != null && friend != null && !user.equals(friend)
            && areFriends(user, friend).isEmpty()) {
      final Relationship request = hasRequest(user, friend);

      if (request != null) {
        final Transaction tx = this.fDatabase.beginTx();

        // request confirmed
        try {
          request.delete();

          user.createRelationshipTo(friend, Neo4jRelTypes.FRIEND_OF);
          friend.createRelationshipTo(user, Neo4jRelTypes.FRIEND_OF);

          tx.success();
          tx.finish();
        } catch (final Exception e) {
          tx.failure();
          tx.finish();
          e.printStackTrace();
          this.fLogger.log(Level.SEVERE, "friendship confirmation failed", e);
        }
      } else if (hasRequest(friend, user) == null) {
        final Transaction tx = this.fDatabase.beginTx();

        // new request if there isn't already one
        try {
          user.createRelationshipTo(friend, Neo4jRelTypes.FRIEND_REQUEST);

          tx.success();
          tx.finish();
        } catch (final Exception e) {
          tx.failure();
          tx.finish();
          e.printStackTrace();
          this.fLogger.log(Level.SEVERE, "friendship request failed", e);
        }
      }
    }
  }

  /**
   * Terminates a friendship relation between the two users specified or denies a friendship request
   * from the second user to the first.
   *
   * @param userId
   *          denying user
   * @param targetId
   *          user to send denial to
   */
  public void denyFriendship(String userId, String targetId) {
    final Node user = this.fPersonSPI.getPersonNode(userId);
    final Node friend = this.fPersonSPI.getPersonNode(targetId);

    if (user != null && friend != null && !user.equals(friend)) {
      final List<Relationship> friendRels = areFriends(user, friend);

      if (friendRels.isEmpty()) {
        // not friends yet - check for requests
        final Relationship reqRel = hasRequest(user, friend);

        if (reqRel != null) {
          final Transaction tx = this.fDatabase.beginTx();

          try {
            reqRel.delete();

            tx.success();
            tx.finish();
          } catch (final Exception e) {
            tx.failure();
            tx.finish();
            e.printStackTrace();
            this.fLogger.log(Level.SEVERE, "friendship denial failed", e);
          }
        }
      } else {
        final Transaction tx = this.fDatabase.beginTx();

        // delete friendship relations
        try {
          for (final Relationship rel : friendRels) {
            rel.delete();
          }

          tx.success();
          tx.finish();
        } catch (final Exception e) {
          tx.failure();
          tx.finish();
          e.printStackTrace();
          this.fLogger.log(Level.SEVERE, "friendship deletion failed", e);
        }
      }
    }
  }
}
