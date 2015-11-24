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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
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
  private static final String FR_REQ_ACT_PROP = "autoactivities.friend_request";
  private static final String FR_DENY_ACT_PROP = "autoactivities.friend_deny";
  private static final String NEW_FR_ACT_PROP = "autoactivities.new_friendship";
  private static final String DEL_FR_ACT_PROP = "autoactivities.friendship_deleted";

  private static final String REQ_NAME_PROP = "names.friends.request";
  private static final String REQ_TITLE_PROP = "titles.friends.request";
  private static final String NEW_TITLE_PROP = "titles.friends.new";
  private static final String DENY_TITLE_PROP = "titles.friends.deny";
  private static final String DEL_TITLE_PROP = "titles.friends.delete";

  private static final int TYPE_REQUEST = 0;
  private static final int TYPE_NEW = 1;
  private static final int TYPE_DENY = 2;
  private static final int TYPE_DELETE = 3;

  public static final String FRIEND_REQUEST_TYPE = "friend-request";
  public static final String VERB_MAKE_FRIEND = "make-friend";
  public static final String VERB_REMOVE_FRIEND = "remove-friend";
  public static final String VERB_DENY = "deny";
  public static final String VERB_REQ_FRIEND = "request-friend";

  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;

  private final boolean fFriendReqActivity, fFriendDenyActivity, fNewFriendActivity,
          fDelFriendActivity;

  private final String fRequestName;
  private final String fReqTitle, fDenyTitle, fNewTitle, fDelTitle;

  private final Map<String, Object> fGeneratorObject;

  private final DateFormat fDateFormat;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  private GraphActivityStreamSPI fActivities;

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
  public GraphFriendSPI(GraphDatabaseService database, Map<String, String> config,
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

    this.fFriendReqActivity = Boolean.parseBoolean(config.get(GraphFriendSPI.FR_REQ_ACT_PROP));
    this.fFriendDenyActivity = Boolean.parseBoolean(config.get(GraphFriendSPI.FR_DENY_ACT_PROP));
    this.fNewFriendActivity = Boolean.parseBoolean(config.get(GraphFriendSPI.NEW_FR_ACT_PROP));
    this.fDelFriendActivity = Boolean.parseBoolean(config.get(GraphFriendSPI.DEL_FR_ACT_PROP));

    this.fRequestName = config.get(GraphFriendSPI.REQ_NAME_PROP);
    this.fReqTitle = config.get(GraphFriendSPI.REQ_TITLE_PROP);
    this.fDenyTitle = config.get(GraphFriendSPI.DENY_TITLE_PROP);
    this.fNewTitle = config.get(GraphFriendSPI.NEW_TITLE_PROP);
    this.fDelTitle = config.get(GraphFriendSPI.DEL_TITLE_PROP);

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());

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
    if (sortField == null || sortField.equals(OSFields.NAME_FIELD)) {
      options.put(WebsockConstants.SORT_FIELD, OSFields.FORMATTED_FIELD);
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
    boolean success = true;

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
          success = false;
          e.printStackTrace();
          this.fLogger.log(Level.SEVERE, "friendship confirmation failed", e);
        }

        // generate activity if configured
        if (this.fNewFriendActivity && success) {
          final String userName = getUserName(user);
          final String friendName = getUserName(friend);
          friendActivity(userId, userName, targetId, friendName, GraphFriendSPI.TYPE_NEW);
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
          success = false;
          e.printStackTrace();
          this.fLogger.log(Level.SEVERE, "friendship request failed", e);
        }

        // generate activity if configured
        if (this.fFriendReqActivity && success) {
          final String userName = getUserName(user);
          final String friendName = getUserName(friend);
          friendActivity(userId, userName, targetId, friendName, GraphFriendSPI.TYPE_REQUEST);
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
    boolean success = true;

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
            success = false;
            e.printStackTrace();
            this.fLogger.log(Level.SEVERE, "friendship denial failed", e);
          }

          // generate activity if configured
          if (this.fFriendDenyActivity && success) {
            final String userName = getUserName(user);
            final String friendName = getUserName(friend);
            friendActivity(userId, userName, targetId, friendName, GraphFriendSPI.TYPE_DENY);
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
          success = false;
          e.printStackTrace();
          this.fLogger.log(Level.SEVERE, "friendship deletion failed", e);
        }

        // generate activity if configured
        if (this.fDelFriendActivity && success) {
          final String userName = getUserName(user);
          final String friendName = getUserName(friend);
          friendActivity(userId, userName, targetId, friendName, GraphFriendSPI.TYPE_DELETE);
        }
      }
    }
  }

  private String getUserName(Node user) {
    String name = null;

    if (user != null) {
      // try formatted property
      final Object nameProp = user.getProperty("formatted", null);

      if (nameProp != null) {
        name = nameProp.toString();
      } else {
        // fall back to user ID
        name = user.getProperty("id").toString();
      }
    }

    return name;
  }

  private void friendActivity(String userId, String userName, String friendId, String friendName,
          int type) {
    if (this.fActivities != null) {
      final Map<String, Object> activity = new HashMap<String, Object>();

      // acting person
      final Map<String, Object> actor = new HashMap<String, Object>();
      actor.put(OSFields.ID_FIELD, userId);
      actor.put(OSFields.DISP_NAME_FIELD, userName);
      actor.put(OSFields.OBJECT_TYPE, OSFields.PERSON_TYPE);
      activity.put(OSFields.ACTOR_FIELD, actor);

      // referenced friend
      Map<String, Object> object = new HashMap<String, Object>();
      object.put(OSFields.ID_FIELD, friendId);
      object.put(OSFields.DISP_NAME_FIELD, friendName);
      object.put(OSFields.OBJECT_TYPE, OSFields.PERSON_TYPE);
      activity.put(OSFields.OBJECT_FIELD, object);

      // TODO: title?

      switch (type) {
      case TYPE_REQUEST:
        activity.put(OSFields.VERB_FIELD, GraphFriendSPI.VERB_REQ_FRIEND);
        activity.put(OSFields.TITLE_FIELD, this.fReqTitle);

        break;
      case TYPE_NEW:
        activity.put(OSFields.VERB_FIELD, GraphFriendSPI.VERB_MAKE_FRIEND);
        activity.put(OSFields.TITLE_FIELD, this.fNewTitle);
        break;

      case TYPE_DENY:
        activity.put(OSFields.VERB_FIELD, GraphFriendSPI.VERB_DENY);
        activity.put(OSFields.TITLE_FIELD, this.fDenyTitle);

        // friendship request object, friend is target
        activity.remove(OSFields.OBJECT_FIELD);
        activity.put(OSFields.TARGET_FIELD, object);

        object = new HashMap<String, Object>();
        object.put(OSFields.OBJECT_TYPE, GraphFriendSPI.FRIEND_REQUEST_TYPE);
        object.put(OSFields.DISP_NAME_FIELD, this.fRequestName);
        activity.put(OSFields.OBJECT_FIELD, object);

        break;

      case TYPE_DELETE:
        activity.put(OSFields.VERB_FIELD, GraphFriendSPI.VERB_REMOVE_FRIEND);
        activity.put(OSFields.TITLE_FIELD, this.fDelTitle);
        break;

      // undefined type, abort
      default:
        return;
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
}
