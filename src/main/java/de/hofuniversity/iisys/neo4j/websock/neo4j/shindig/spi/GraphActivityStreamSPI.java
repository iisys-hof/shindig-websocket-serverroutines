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
import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IDManager;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphActivityEntry;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the activity stream service retrieving activity stream data from the Neo4j
 * graph database.
 */
public class GraphActivityStreamSPI {
  private static final String ID_FIELD = "id";

  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;
  private final ActivityObjectService fActObjSPI;
  private final ApplicationService fApplicationSPI;
  private final IDManager fIDMan;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  /**
   * Creates a graph activity stream service using data from the given provider of person data and
   * activity object service. Throws a NullPointerException if one of the parameters is null.
   *
   * @param database
   *          graph database to use
   * @param personSPI
   *          person data provider to use
   * @param actObjSPI
   *          activity object data provider to use
   * @param appSPI
   *          application service to use
   * @param idMan
   *          ID manager to use
   * @param impl
   *          implementation utility to use
   */
  public GraphActivityStreamSPI(GraphDatabaseService database, GraphPersonSPI personSPI,
          ActivityObjectService actObjSPI, ApplicationService appSPI, IDManager idMan, ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("database service was null");
    }
    if (personSPI == null) {
      throw new NullPointerException("person service was null");
    }
    if (actObjSPI == null) {
      throw new NullPointerException("activity object service was null");
    }
    if (appSPI == null) {
      throw new NullPointerException("application service was null");
    }
    if (idMan == null) {
      throw new NullPointerException("ID manager was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    this.fDatabase = database;
    this.fPersonSPI = personSPI;
    this.fActObjSPI = actObjSPI;
    this.fApplicationSPI = appSPI;
    this.fIDMan = idMan;

    this.fImpl = impl;

    // TODO: index

    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  private void addActivities(Node person, Set<String> actIds, final List<Node> activities) {
    Node actNode = null;

    final Iterable<Relationship> actRels = person.getRelationships(Neo4jRelTypes.ACTED);

    // id-filter?
    if (actIds != null) {
      for (final Relationship rel : actRels) {
        actNode = rel.getEndNode();

        if (actIds.contains(actNode.getProperty(OSFields.ID_FIELD))) {
          activities.add(actNode);

          // check if complete
          if (activities.size() == actIds.size()) {
            break;
          }
        }
      }
    }
    // copy all
    else {
      for (final Relationship rel : actRels) {
        actNode = rel.getEndNode();
        activities.add(actNode);
      }
    }
  }

  private void addActivities(String userId, Set<String> actIds, final List<Node> activities) {
    final Node person = this.fPersonSPI.getPersonNode(userId);

    if (person != null) {
      addActivities(person, actIds, activities);
    }
  }

  private void addFriendActivities(String userId, Set<String> actIds, final List<Node> activities) {
    final Node person = this.fPersonSPI.getPersonNode(userId);

    if (person != null) {
      final Iterable<Relationship> friendships = person.getRelationships(Direction.OUTGOING,
              Neo4jRelTypes.FRIEND_OF);

      for (final Relationship rel : friendships) {
        addActivities(rel.getEndNode(), actIds, activities);
      }
    }
  }

  private ListResult convertRequested(final List<Node> actNodes, final Set<String> fields,
          Map<String, Object> options) {
    final List<Map<String, Object>> actEntries = this.fImpl.newList();

    GraphActivityEntry gActEntry = null;
    Map<String, Object> tmpEntry = null;

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = actNodes.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(actNodes.size(), first + max);

    // convert the items requested
    for (int index = first; index < last; ++index) {
      gActEntry = new GraphActivityEntry(actNodes.get(index), this.fImpl);
      tmpEntry = gActEntry.toMap(fields);
      actEntries.add(tmpEntry);
    }

    final ListResult actColl = new ListResult(actEntries);
    actColl.setFirst(first);
    actColl.setMax(max);
    actColl.setTotal(actNodes.size());
    return actColl;
  }

  /**
   * Retrieves activities for the people and groups specified, as defined by the given options. User
   * IDs may not be null or empty.
   *
   * @param userIds
   *          set of user IDs to base the request on
   * @param groupId
   *          group of people to retrieve activities for
   * @param appId
   *          application ID to retrieve activities for
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of results
   */
  public ListResult getActivityEntries(List<String> userIds, String groupId, String appId,
          Map<String, Object> options, List<String> fields) {
    // TODO: retrieve by application

    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final List<Node> activities = new ArrayList<Node>();

    // activities of the people themselves
    if (groupId == null || groupId.equals(OSFields.GROUP_TYPE_SELF)
            || groupId.equals(OSFields.GROUP_TYPE_ALL)) {
      for (final String id : userIds) {
        addActivities(id, null, activities);
      }
    }

    // activities for friends
    if (groupId != null
            && (groupId.equals(OSFields.GROUP_TYPE_FRIENDS) || groupId
                    .equals(OSFields.GROUP_TYPE_ALL))) {
      for (final String id : userIds) {
        addFriendActivities(id, null, activities);
      }
    }

    // activities for the members of a group
    if (groupId != null && groupId.charAt(0) != '@') {
      final Set<Node> memNodes = this.fPersonSPI.getGroupMemberNodes(groupId);

      for (final Node memNode : memNodes) {
        addActivities(memNode, null, activities);
      }
    }

    // filter
    NodeFilter.filterNodes(activities, options);
    // TODO: other filters?

    // sort as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null) {
      options.put(WebsockConstants.SORT_FIELD, OSFields.ID_FIELD);
    }
    NodeSorter.sortNodes(activities, options);

    return convertRequested(activities, fieldSet, options);
  }

  /**
   * Retrieves a set of activities by ID for a person and group specified, as defined by the given
   * options. User ID and set of activity IDs may not be null or empty.
   *
   * @param userId
   *          ID of the user to base the request on
   * @param groupId
   *          group to retrieve activities for
   * @param appId
   *          application to retrieve activities for
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @param activityIds
   *          IDs of activities to retrieve
   * @return list of results
   */
  public ListResult getActivityEntries(String userId, String groupId, String appId,
          Map<String, Object> options, List<String> fields, List<String> activityIds) {
    if (activityIds == null || activityIds.isEmpty()) {
      throw new RuntimeException("no activity IDs given");
    }

    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final Set<String> actIdSet = new HashSet<String>();
    if (activityIds != null) {
      actIdSet.addAll(activityIds);
    }
    final List<Node> activities = new ArrayList<Node>();

    // activities of the people themselves
    if (groupId == null || groupId.equals(OSFields.GROUP_TYPE_SELF)
            || groupId.equals(OSFields.GROUP_TYPE_ALL)) {
      addActivities(userId, actIdSet, activities);
    }

    // activities for friends
    if (groupId != null
            && (groupId.equals(OSFields.GROUP_TYPE_FRIENDS) || groupId
                    .equals(OSFields.GROUP_TYPE_ALL))) {
      addFriendActivities(userId, actIdSet, activities);
    }

    // activities for the members of a group
    if (groupId != null && groupId.charAt(0) != '@') {
      final Set<Node> memNodes = this.fPersonSPI.getGroupMemberNodes(groupId);

      for (final Node memNode : memNodes) {
        addActivities(memNode, null, activities);
      }
    }

    // filter
    NodeFilter.filterNodes(activities, options);
    // TODO: other filters?

    // sort as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null) {
      options.put(WebsockConstants.SORT_FIELD, OSFields.ACT_PUBLISHED_FIELD);
    }
    NodeSorter.sortNodes(activities, options);

    return convertRequested(activities, fieldSet, options);
  }

  /**
   * Retrieves a single activity by its ID for a user. Neither user nor ID may be null.
   *
   * @param userId
   *          user to retrieve the activity for
   * @param groupId
   *          group to retrieve the activity for
   * @param appId
   *          application to retrieve the activity for
   * @param fields
   *          fields to retrieve
   * @param activityId
   *          ID of the activity to retrieve
   * @return single activity entry result
   */
  public SingleResult getActivityEntry(String userId, String groupId, String appId,
          List<String> fields, String activityId) {
    if (activityId == null) {
      throw new RuntimeException("no activity ID given");
    }

    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    Map<String, Object> activity = null;

    // TODO: groups, friends?

    // TODO: are those IDs unique? - maybe build an index

    final Node person = this.fPersonSPI.getPersonNode(userId);

    if (person != null) {
      final Iterable<Relationship> actRels = person.getRelationships(Neo4jRelTypes.ACTED);

      String id = null;
      Node actNode = null;
      for (final Relationship rel : actRels) {
        actNode = rel.getEndNode();
        id = actNode.getProperty(OSFields.ID_FIELD).toString();

        if (activityId.equals(id)) {
          activity = new GraphActivityEntry(actNode, this.fImpl).toMap(fieldSet);
          break;
        }
      }
    }

    return new SingleResult(activity);
  }

  /**
   * Deletes a list of activity IDs for a user and group.
   *
   * @param userId
   *          user to delete activities for
   * @param groupId
   *          group to delete activities for
   * @param appId
   *          application to delete activities for
   * @param activityIds
   *          list of activities IDs to delete
   */
  public void deleteActivityEntries(String userId, String groupId, String appId,
          List<String> activityIds) {
    final Set<String> actIdSet = new HashSet<String>();
    if (activityIds != null) {
      actIdSet.addAll(activityIds);
    }
    final List<Node> activities = new ArrayList<Node>();
    addActivities(userId, actIdSet, activities);

    final Set<Node> actObjs = new HashSet<Node>();
    Iterable<Relationship> objRels = null;

    final Transaction tx = this.fDatabase.beginTx();

    try {
      for (final Node activity : activities) {
        // collect activity objects
        objRels = activity.getRelationships(Neo4jRelTypes.ACTOR, Neo4jRelTypes.GENERATOR,
                Neo4jRelTypes.OBJECT, Neo4jRelTypes.TARGET, Neo4jRelTypes.PROVIDER);

        for (final Relationship rel : objRels) {
          actObjs.add(rel.getEndNode());
        }

        // delete activity entry
        for (final Relationship rel : activity.getRelationships()) {
          rel.delete();
        }
        activity.delete();
      }

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      throw new RuntimeException(e);
    }

    // free unused objects if possible
    this.fActObjSPI.deleteIfUnused(actObjs);
  }

  public SingleResult updateActivityEntry(String userId, String groupId, String appId,
          final String activityId, Map<String, Object> activity, List<String> fields) {
    // TODO: appId?

    // get activity
    Node actNode = null;
    final Node person = this.fPersonSPI.getPersonNode(userId);
    if (person == null) {
      throw new RuntimeException("person not found");
    }

    final Iterable<Relationship> actRels = person.getRelationships(Neo4jRelTypes.ACTED,
            Direction.OUTGOING);
    Node tmpNode = null;
    for (final Relationship rel : actRels) {
      tmpNode = rel.getEndNode();

      if (activityId.equals(tmpNode.getProperty(GraphActivityStreamSPI.ID_FIELD))) {
        actNode = tmpNode;
        break;
      }
    }

    final Transaction tx = this.fDatabase.beginTx();

    try {
      // strip current activity objects
      final Set<Node> actObjNodes = new HashSet<Node>();
      final Iterable<Relationship> actObjRels = actNode.getRelationships(Direction.OUTGOING,
              Neo4jRelTypes.ACTOR, Neo4jRelTypes.OBJECT, Neo4jRelTypes.TARGET,
              Neo4jRelTypes.GENERATOR, Neo4jRelTypes.PROVIDER);

      for (final Relationship rel : actObjRels) {
        actObjNodes.add(rel.getEndNode());
        rel.delete();
      }

      this.fActObjSPI.deleteIfUnused(actObjNodes);

      // update
      storeEntry(actNode, activity);

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      this.fLogger.log(Level.SEVERE, "failed to update activity " + activityId);
      tx.failure();
      tx.finish();
    }

    final GraphActivityEntry actEntry = new GraphActivityEntry(actNode, this.fImpl);
    return new SingleResult(actEntry.toMap(null));
  }

  @SuppressWarnings("unchecked")
  private GraphActivityEntry storeEntry(final Node actNode, final Map<String, Object> entry) {
    final GraphActivityEntry gActEntry = new GraphActivityEntry(actNode, this.fImpl);

    gActEntry.setData(entry);

    // link to activity objects
    Node actObject = null;
    final Map<String, Object> actor = (Map<String, Object>) entry
            .get(GraphActivityEntry.ACTOR_FIELD);
    final Map<String, Object> object = (Map<String, Object>) entry
            .get(GraphActivityEntry.OBJECT_FIELD);
    final Map<String, Object> target = (Map<String, Object>) entry
            .get(GraphActivityEntry.TARGET_FIELD);
    final Map<String, Object> generator = (Map<String, Object>) entry
            .get(GraphActivityEntry.GENERATOR_FIELD);
    final Map<String, Object> provider = (Map<String, Object>) entry
            .get(GraphActivityEntry.PROVIDER_FIELD);

    if (actor != null) {
      actObject = this.fActObjSPI.getObjectNode(actor);
      actNode.createRelationshipTo(actObject, Neo4jRelTypes.ACTOR);
    }

    if (object != null) {
      actObject = this.fActObjSPI.getObjectNode(object);
      actNode.createRelationshipTo(actObject, Neo4jRelTypes.OBJECT);
    }

    if (target != null) {
      actObject = this.fActObjSPI.getObjectNode(target);
      actNode.createRelationshipTo(actObject, Neo4jRelTypes.TARGET);
    }

    if (generator != null) {
      actObject = this.fActObjSPI.getObjectNode(generator);
      actNode.createRelationshipTo(actObject, Neo4jRelTypes.GENERATOR);
    }

    if (provider != null) {
      actObject = this.fActObjSPI.getObjectNode(provider);
      actNode.createRelationshipTo(actObject, Neo4jRelTypes.PROVIDER);
    }

    // TODO: generate URL if not available?

    return gActEntry;
  }

  /**
   * Creates an activity for a user. The activity map may not be null.
   *
   * @param userId
   *          user to create an activity for
   * @param groupId
   *          group to create an activity for
   * @param appId
   *          application to create an activity for
   * @param activity
   *          activity data to store
   * @param fields
   *          fields to retrieve
   * @return single result with the created activity
   */
  public SingleResult createActivityEntry(String userId, String groupId, String appId,
          Map<String, Object> activity, List<String> fields) {
    if (activity == null) {
      throw new RuntimeException("activity data map was null");
    }

    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final String id = this.fIDMan.genID(ShindigConstants.ACTIVITY_ENTRY_NODES);
    final Node person = this.fPersonSPI.getPersonNode(userId);

    // user not found
    if (person == null) {
      // TODO: more appropriate exception
      throw new RuntimeException("User with ID \"" + userId + "\" not found");
    }

    GraphActivityEntry gActEntry = null;
    final Transaction tx = this.fDatabase.beginTx();

    try {

      final Node actNode = this.fDatabase.createNode();
      activity.put(OSFields.ID_FIELD, id);

      // store information
      gActEntry = storeEntry(actNode, activity);

      // link to user
      person.createRelationshipTo(actNode, Neo4jRelTypes.ACTED);

      // link to application
      if (appId != null) {
        final Node application = this.fApplicationSPI.getApplication(appId);
        actNode.createRelationshipTo(application, Neo4jRelTypes.CAME_FROM);
      }

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      this.fLogger.log(Level.SEVERE, e.getMessage(), e);

      tx.failure();
      tx.finish();

      throw new RuntimeException(e);
    }

    final Map<String, Object> entry = gActEntry.toMap(fieldSet);
    return new SingleResult(entry);
  }
}
