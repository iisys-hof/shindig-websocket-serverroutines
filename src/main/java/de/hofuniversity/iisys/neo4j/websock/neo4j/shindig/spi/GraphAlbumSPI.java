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
import java.util.LinkedList;
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
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphAlbum;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the album service retrieving album data from the Neo4j graph database.
 */
public class GraphAlbumSPI {
  private static final String ID_FIELD = "id";

  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;
  private final IDManager fIdMan;
  private final ImplUtil fImpl;

  /**
   * Creates an album service retrieving media items for the people from the given person service.
   * Throws a NullPointerException if any parameter is null.
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
  public GraphAlbumSPI(GraphDatabaseService database, GraphPersonSPI personSPI, IDManager idMan,
          ImplUtil impl) {
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
   * Retrieves a single album for a single user, specified by its ID. Throws a NotFoundException if
   * the album can not be found.
   *
   * @param userId
   *          ID of the album's owner
   * @param appId
   *          ID of the application making the request?
   * @param albumId
   *          ID of the album to retrieve
   * @param fields
   *          fields to retrieve or null for all
   * @return wrapped single album
   */
  public SingleResult getAlbum(String userId, String appId, final String albumId,
          List<String> fields) {
    Node albNode = null;
    Map<String, Object> albumMap = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find single album with matching ID
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphAlbumSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    if (albNode != null) {
      Set<String> fieldSet = null;
      if (fields != null && !fields.isEmpty()) {
        fieldSet = new HashSet<String>(fields);
      }

      albumMap = new GraphAlbum(albNode, this.fImpl).toMap(fieldSet);
    } else {
      throw new NotFoundException("album not found");
    }

    return new SingleResult(albumMap);
  }

  private ListResult convert(final List<Node> albNodes, final Map<String, Object> options,
          final Set<String> fields) {
    final List<Map<String, Object>> albumMaps = this.fImpl.newList();

    // filter
    NodeFilter.filterNodes(albNodes, options);
    // TODO: other filters?

    // sort as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null) {
      options.put(WebsockConstants.SORT_FIELD, OSFields.ID_FIELD);
    }
    NodeSorter.sortNodes(albNodes, options);

    GraphAlbum gAlbum = null;
    Map<String, Object> tmpAlbum = null;

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = albNodes.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(albNodes.size(), first + max);

    // convert the items requested
    for (int index = first; index < last; ++index) {
      gAlbum = new GraphAlbum(albNodes.get(index), this.fImpl);
      tmpAlbum = gAlbum.toMap(fields);
      albumMaps.add(tmpAlbum);
    }

    final ListResult result = new ListResult(albumMaps);
    result.setFirst(first);
    result.setMax(max);
    result.setTotal(albNodes.size());
    return result;
  }

  /**
   * Retrieves a list of albums for the specified user's albums with the given IDs.
   *
   * @param userId
   *          ID of the albums' owner
   * @param appId
   *          ID of the application making the request?
   * @param albumIds
   *          IDs of the albums to retrieve
   * @param options
   *          filter, sorting and pagination options
   * @param fields
   *          fields to retrieve or null for all
   * @return wrapped list of albums
   */
  public ListResult getAlbums(String userId, String appId, final List<String> albumIds,
          Map<String, Object> options, List<String> fields) {
    final List<Node> albNodes = new ArrayList<Node>();

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // add albums with matching IDs
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumIds.contains(tmpNode.getProperty(GraphAlbumSPI.ID_FIELD))) {
        albNodes.add(tmpNode);

        // break if all have been retrieved
        if (albNodes.size() == albumIds.size()) {
          break;
        }
      }
    }

    Set<String> fieldSet = null;
    if (fields != null && !fields.isEmpty()) {
      fieldSet = new HashSet<String>(fields);
    }

    return convert(albNodes, options, fieldSet);
  }

  private void addAlbumNodes(List<Node> people, final List<Node> albNodes) {
    Iterable<Relationship> albRels = null;

    for (final Node person : people) {
      albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF, Direction.OUTGOING);

      for (final Relationship rel : albRels) {
        albNodes.add(rel.getEndNode());
      }
    }
  }

  /**
   * Retrieves a list of albums for the specified users and groups. If no group is specified, albums
   * for the users themselves are retrieved.
   *
   * @param userIds
   *          IDs of the users to retrieve albums for
   * @param groupId
   *          ID of the group of users to retrieve albums for
   * @param appId
   *          ID of the application making the request?
   * @param options
   *          filter, sorting and pagination options
   * @param fields
   *          fields to retrieve or null for all
   * @return list of albums
   */
  public ListResult getAlbums(final List<String> userIds, final String groupId, String appId,
          Map<String, Object> options, List<String> fields) {
    final List<Node> people = new LinkedList<Node>();
    final List<Node> albNodes = new ArrayList<Node>();

    Node person = null;

    // people themselves
    if (groupId == null || groupId.equals(OSFields.GROUP_TYPE_SELF)
            || groupId.equals(OSFields.GROUP_TYPE_ALL)) {
      for (final String uid : userIds) {
        person = this.fPersonSPI.getPersonNode(uid);
        people.add(person);
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
          people.add(rel.getEndNode());
        }
      }
    }

    // actual groups
    if (groupId != null && groupId.charAt(0) != '@') {
      final Set<Node> memNodes = this.fPersonSPI.getGroupMemberNodes(groupId);
      people.addAll(memNodes);
    }

    addAlbumNodes(people, albNodes);

    Set<String> fieldSet = null;
    if (fields != null && !fields.isEmpty()) {
      fieldSet = new HashSet<String>(fields);
    }

    return convert(albNodes, options, fieldSet);
  }

  private void deleteAllItems(Node album) {
    final Iterable<Relationship> relIter = album.getRelationships(Neo4jRelTypes.CONTAINS,
            Direction.OUTGOING);

    Node item = null;
    Relationship locRel = null;
    Node itemLoc = null;
    for (final Relationship rel : relIter) {
      item = rel.getEndNode();
      rel.delete();

      // TODO: check if used in other way
      if (!item.hasRelationship(Neo4jRelTypes.CONTAINS, Direction.INCOMING)) {
        ;
      }
      {
        locRel = item.getSingleRelationship(Neo4jRelTypes.LOCATED_AT, Direction.OUTGOING);

        if (locRel != null) {
          itemLoc = locRel.getEndNode();
          locRel.delete();

          if (!itemLoc.hasRelationship()) {
            itemLoc.delete();
          }
        }

        item.delete();
      }
    }
  }

  /**
   * Deletes the album with the ID specified for the user with the given ID. Throws a
   * NotFoundException if the album could not be found.
   *
   * @param userId
   *          ID of the album's owner
   * @param appId
   *          ID of the application making the request
   * @param albumId
   *          ID of the album to delete
   */
  public void deleteAlbum(String userId, String appId, String albumId) {
    Node albNode = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find single album with matching ID
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphAlbumSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    if (albNode != null) {
      final Transaction tx = this.fDatabase.beginTx();

      try {
        // delete all contained media items
        deleteAllItems(albNode);

        // delete all relationships, album and location
        final Relationship locRel = albNode.getSingleRelationship(Neo4jRelTypes.LOCATED_AT,
                Direction.OUTGOING);
        if (locRel != null) {
          final Node loc = locRel.getEndNode();
          locRel.delete();

          if (!loc.hasRelationship()) {
            loc.delete();
          }
        }

        for (final Relationship rel : albNode.getRelationships()) {
          rel.delete();
        }
        albNode.delete();

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
  }

  /**
   * Creates an album with the data specified for the user with the given ID.
   *
   * @param userId
   *          ID of the user to create the album for
   * @param appId
   *          ID of the application making the request?
   * @param album
   *          album data to use for creation
   */
  public void createAlbum(String userId, String appId, final Map<String, Object> album) {
    String id = (String) album.get(GraphAlbumSPI.ID_FIELD);

    if (id == null) {
      id = this.fIdMan.genID(ShindigConstants.ALBUM_NODES);
      album.put(GraphAlbumSPI.ID_FIELD, id);
    }

    final Node person = this.fPersonSPI.getPersonNode(userId);

    // TODO: check for duplicate ID

    final Transaction tx = this.fDatabase.beginTx();
    try {
      final Node albumNode = this.fDatabase.createNode();
      person.createRelationshipTo(albumNode, Neo4jRelTypes.OWNER_OF);
      new GraphAlbum(albumNode, this.fImpl).setData(album);

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      throw new RuntimeException(e);
    }
  }

  /**
   * Updates an album for the given user, specified by its ID, using the given data. Throws a
   * NotFoundException if the album can not be found.
   *
   * @param userId
   *          ID of the user to update an album for
   * @param appId
   *          ID of the application making the request?
   * @param albumId
   *          ID of the album to update
   * @param album
   *          data to use for the update
   */
  public void updateAlbum(String userId, String appId, String albumId, Map<String, Object> album) {
    Node albNode = null;

    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> albRels = person.getRelationships(Neo4jRelTypes.OWNER_OF,
            Direction.OUTGOING);

    // find single album with matching ID
    Node tmpNode = null;
    for (final Relationship rel : albRels) {
      tmpNode = rel.getEndNode();

      if (albumId.equals(tmpNode.getProperty(GraphAlbumSPI.ID_FIELD))) {
        albNode = tmpNode;
        break;
      }
    }

    if (albNode != null) {
      final Transaction tx = this.fDatabase.beginTx();

      try {
        new GraphAlbum(albNode, this.fImpl).setData(album);

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
  }
}
