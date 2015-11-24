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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IDManager;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphMediaItem;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the media item service retrieving media item data from the Neo4j graph
 * database.
 */
public class GraphMediaItemSPI {
  private static final String ID_FIELD = "id";

  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;
  private final IDManager fIdMan;
  private final ImplUtil fImpl;

  /**
   * Creates a media item service retrieving media items for the people from the given person
   * service. Throws a NullPointerException if the given person service is null.
   *
   * @param database
   *          database service to use
   * @param personSPI
   *          person service to take people from
   * @param idMan
   *          ID manager to use
   * @param impl
   *          implementation utility to use
   */
  public GraphMediaItemSPI(GraphDatabaseService database, GraphPersonSPI personSPI,
          IDManager idMan, ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("database service was null");
    }
    if (personSPI == null) {
      throw new NullPointerException("person service was null");
    }
    if (idMan == null) {
      throw new NullPointerException("ID manager was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    this.fDatabase = database;
    this.fPersonSPI = personSPI;
    this.fIdMan = idMan;
    this.fImpl = impl;
  }

  /**
   * Retrieves a single media item for the specified user's album with the, given album ID matching
   * the given meida item ID. Throws a NotFoundException if the album or media item can not be found
   *
   * @param userId
   *          ID of the media item's owner
   * @param appId
   *          ID of the application making the request?
   * @param albumId
   *          ID of the album the media item is in
   * @param mediaItemId
   *          ID of the media item to retrieve
   * @param fields
   *          fields to retrieve or null for all
   * @return single wrapped media item
   */
  public SingleResult getMediaItem(String userId, String appId, final String albumId,
          final String mediaItemId, List<String> fields) {
    Node albNode = null;
    Node itemNode = null;
    Map<String, Object> itemMap = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find album
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    // find media item
    if (albNode != null) {
      final Iterable<Relationship> itemRels = albNode.getRelationships(Neo4jRelTypes.CONTAINS,
              Direction.OUTGOING);

      for (final Relationship rel : itemRels) {
        tmpNode = rel.getEndNode();

        if (mediaItemId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
          itemNode = tmpNode;
          break;
        }
      }
    } else {
      throw new NotFoundException("album not found");
    }

    // convert
    if (itemNode != null) {
      Set<String> fieldSet = null;
      if (fields != null && !fields.isEmpty()) {
        fieldSet = new HashSet<String>(fields);
      }

      itemMap = new GraphMediaItem(itemNode, this.fImpl).toMap(fieldSet);
    } else {
      throw new NotFoundException("media item not found");
    }

    return new SingleResult(itemMap);
  }

  private ListResult convert(final List<Node> itemNodes, final Map<String, Object> options,
          final Set<String> fields) {
    final List<Map<String, Object>> itemMaps = this.fImpl.newList();

    // filter
    NodeFilter.filterNodes(itemNodes, options);
    // TODO: other filters?

    // sort as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null) {
      options.put(WebsockConstants.SORT_FIELD, OSFields.ID_FIELD);
    }
    NodeSorter.sortNodes(itemNodes, options);

    GraphMediaItem gItem = null;
    Map<String, Object> tmpItem = null;

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = itemNodes.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(itemNodes.size(), first + max);

    // convert the items requested
    for (int index = first; index < last; ++index) {
      gItem = new GraphMediaItem(itemNodes.get(index), this.fImpl);
      tmpItem = gItem.toMap(fields);
      itemMaps.add(tmpItem);
    }

    final ListResult result = new ListResult(itemMaps);
    result.setFirst(first);
    result.setMax(max);
    result.setTotal(itemNodes.size());
    return result;
  }

  /**
   * Retrieves a list of media items for the user specified, from the album with the given album ID,
   * matching IDs from the given list of media item IDs.
   *
   * @param userId
   *          ID of the user the media items belong to
   * @param appId
   *          ID of the application making the request?
   * @param albumId
   *          ID of the album to retrieve media items from
   * @param mediaItemIds
   *          list of media item IDs to retrieve
   * @param options
   *          filter, sorting and pagination options
   * @param fields
   *          fields to retrieve or null for all
   * @return wrapped list of media items
   */
  public ListResult getMediaItems(String userId, String appId, final String albumId,
          final List<String> mediaItemIds, Map<String, Object> options, List<String> fields) {
    final List<Node> itemNodes = new ArrayList<Node>();
    Node albNode = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find album
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    // find media items
    if (albNode != null) {
      final Iterable<Relationship> itemRels = albNode.getRelationships(Neo4jRelTypes.CONTAINS,
              Direction.OUTGOING);

      for (final Relationship rel : itemRels) {
        tmpNode = rel.getEndNode();

        if (mediaItemIds.contains((tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD)))) {
          itemNodes.add(tmpNode);

          // break when all items have been found
          if (itemNodes.size() == mediaItemIds.size()) {
            break;
          }
        }
      }
    } else {
      throw new NotFoundException("album not found");
    }

    Set<String> fieldSet = null;
    if (fields != null && !fields.isEmpty()) {
      fieldSet = new HashSet<String>(fields);
    }

    return convert(itemNodes, options, fieldSet);
  }

  /**
   * Retrieves a list of media items for the user specified, from the album with the given album ID.
   *
   * @param userId
   *          ID of the user the media items belong to
   * @param appId
   *          ID of the application making the request?
   * @param albumId
   *          albumId ID of the album to retrieve media items from
   * @param options
   *          filter, sorting and pagination options
   * @param fields
   *          fields to retrieve or null for all
   * @return wrapped list of media items
   */
  public ListResult getMediaItems(String userId, String appId, String albumId,
          Map<String, Object> options, List<String> fields) {
    final List<Node> itemNodes = new ArrayList<Node>();
    Node albNode = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find album
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    // add all item nodes
    if (albNode != null) {
      final Iterable<Relationship> itemRels = albNode.getRelationships(Neo4jRelTypes.CONTAINS,
              Direction.OUTGOING);

      for (final Relationship rel : itemRels) {
        itemNodes.add(rel.getEndNode());
      }
    } else {
      throw new NotFoundException("album not found");
    }

    Set<String> fieldSet = null;
    if (fields != null && !fields.isEmpty()) {
      fieldSet = new HashSet<String>(fields);
    }

    return convert(itemNodes, options, fieldSet);
  }

  private void addAllMediaItems(Node person, final List<Node> itemNodes) {
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);
    Iterable<Relationship> itemRels = null;

    for (final Relationship rel : albRels) {
      itemRels = rel.getEndNode().getRelationships(Neo4jRelTypes.CONTAINS, Direction.OUTGOING);

      for (final Relationship itemRel : itemRels) {
        itemNodes.add(itemRel.getEndNode());
      }
    }
  }

  /**
   * Retrieves a list of media items for the specified users and groups from all albums. If no group
   * is specified, media items for the users themselves are retrieved.
   *
   * @param userIds
   *          IDs of the users to retrieve media items for
   * @param groupId
   *          ID of the group of users to retrieve media items for
   * @param appId
   *          ID of the application making the request?
   * @param options
   *          filter, sorting and pagination options
   * @param fields
   *          fields to retrieve or null for all
   * @return list of media items
   */
  public ListResult getMediaItems(List<String> userIds, String groupId, String appId,
          Map<String, Object> options, List<String> fields) {
    final List<Node> itemNodes = new ArrayList<Node>();
    Node person = null;

    // people themselves
    if (groupId == null || groupId.equals(OSFields.GROUP_TYPE_SELF)
            || groupId.equals(OSFields.GROUP_TYPE_ALL)) {
      for (final String uid : userIds) {
        person = this.fPersonSPI.getPersonNode(uid);
        addAllMediaItems(person, itemNodes);
      }
    }

    // people's friends
    if (groupId != null
            && (groupId.equals(OSFields.GROUP_TYPE_FRIENDS) || groupId
                    .equals(OSFields.GROUP_TYPE_ALL))) {
      Iterable<Relationship> friendRels = null;

      for (final String uid : userIds) {
        person = this.fPersonSPI.getPersonNode(uid);

        friendRels = person.getRelationships(Neo4jRelTypes.FRIEND_OF, Direction.OUTGOING);
        for (final Relationship rel : friendRels) {
          addAllMediaItems(rel.getEndNode(), itemNodes);
        }
      }
    }

    // actual groups
    if (groupId != null && groupId.charAt(0) != '@') {
      final Set<Node> memNodes = this.fPersonSPI.getGroupMemberNodes(groupId);
      for (final Node pNode : memNodes) {
        addAllMediaItems(pNode, itemNodes);
      }
    }

    Set<String> fieldSet = null;
    if (fields != null && !fields.isEmpty()) {
      fieldSet = new HashSet<String>(fields);
    }

    return convert(itemNodes, options, fieldSet);
  }

  /**
   * Deletes the media item with the ID specified for the user's album with the given ID. Throws a
   * NotFoundException if the album or media item can not be found.
   *
   * @param userId
   *          ID of the media item's owner
   * @param appId
   *          ID of the application making the request
   * @param albumId
   *          ID of the album to delete the media item from
   * @param mediaItemId
   *          ID of the media item to delete
   */
  public void deleteMediaItem(String userId, String appId, String albumId, String mediaItemId) {
    Node albNode = null;
    Node itemNode = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find album
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    // find media item
    if (albNode != null) {
      final Iterable<Relationship> itemRels = albNode.getRelationships(Neo4jRelTypes.CONTAINS,
              Direction.OUTGOING);

      for (final Relationship rel : itemRels) {
        tmpNode = rel.getEndNode();

        if (mediaItemId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
          itemNode = tmpNode;
          break;
        }
      }
    } else {
      throw new NotFoundException("album not found");
    }

    if (itemNode != null) {
      final Transaction tx = this.fDatabase.beginTx();

      try {
        // delete location
        final Relationship locRel = itemNode.getSingleRelationship(Neo4jRelTypes.LOCATED_AT,
                Direction.OUTGOING);
        if (locRel != null) {
          final Node loc = locRel.getEndNode();
          locRel.delete();

          if (!loc.hasRelationship()) {
            loc.delete();
          }
        }

        // delete relations and media item
        // TODO: check if used otherwise
        for (final Relationship rel : itemNode.getRelationships()) {
          rel.delete();
        }
        itemNode.delete();

        tx.success();
        tx.finish();
      } catch (final Exception e) {
        tx.failure();
        tx.finish();

        throw new RuntimeException(e);
      }
    } else {
      throw new NotFoundException("media item not found");
    }
  }

  /**
   * Creates a media item with the data specified for the user with the given ID in the album with
   * the given album ID. Throws a NotFoundException if the given album could not be found.
   *
   * @param userId
   *          ID of the user to create the media item for
   * @param appId
   *          ID of the application making the request
   * @param albumId
   *          ID of the album to create the media item in
   * @param mediaItem
   *          media item data to use for creation
   */
  public void createMediaItem(String userId, String appId, String albumId,
          Map<String, Object> mediaItem) {
    Object id = mediaItem.get(GraphMediaItemSPI.ID_FIELD);
    if (id == null) {
      id = this.fIdMan.genID(ShindigConstants.MEDIA_ITEM_NODES);
      mediaItem.put(GraphMediaItemSPI.ID_FIELD, id);
    }

    Node albNode = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find album
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    if (albNode != null) {
      final Transaction tx = this.fDatabase.beginTx();

      try {
        final Node itemNode = this.fDatabase.createNode();
        albNode.createRelationshipTo(itemNode, Neo4jRelTypes.CONTAINS);

        new GraphMediaItem(itemNode, this.fImpl).setData(mediaItem);

        tx.success();
        tx.finish();
      } catch (final Exception e) {
        tx.failure();
        tx.finish();

        throw new RuntimeException(e);
      }
    } else {
      throw new NotFoundException("album not found");
    }

    // TODO: check for duplicate ID
  }

  /**
   * Updates a media item for the given user, specified by its ID in the album with the given ID,
   * using the given data. Throws a NotFoundException if the album or media item can not be found.
   *
   * @param userId
   *          ID of the user to update a media item for
   * @param appId
   *          ID of the application making the request?
   * @param albumId
   *          ID of the album containing the media item to update
   * @param mediaItemId
   *          ID of the media item to update
   * @param mediaItem
   *          data to use for the update
   */
  public void updateMediaItem(String userId, String appId, String albumId, String mediaItemId,
          Map<String, Object> mediaItem) {
    Node albNode = null;
    Node itemNode = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find album
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    // find media item
    if (albNode != null) {
      final Iterable<Relationship> itemRels = albNode.getRelationships(Neo4jRelTypes.CONTAINS,
              Direction.OUTGOING);

      for (final Relationship rel : itemRels) {
        tmpNode = rel.getEndNode();

        if (mediaItemId.equals(tmpNode.getProperty(GraphMediaItemSPI.ID_FIELD))) {
          itemNode = tmpNode;
          break;
        }
      }
    } else {
      throw new NotFoundException("album not found");
    }

    // TODO: not found exception

    // update
    if (itemNode != null) {
      final Transaction tx = this.fDatabase.beginTx();

      try {
        new GraphMediaItem(itemNode, this.fImpl).setData(mediaItem);

        tx.success();
        tx.finish();
      } catch (final Exception e) {
        tx.failure();
        tx.finish();

        throw new RuntimeException(e);
      }
    } else {
      throw new NotFoundException("media item not found");
    }
  }
}
