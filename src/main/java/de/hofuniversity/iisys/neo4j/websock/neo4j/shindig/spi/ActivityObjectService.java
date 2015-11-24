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

import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IDManager;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphActivityObject;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Service retrieving and storing and indexing activity objects, returning special objects
 * containing profile information for people. Can deduplicate activity objects by always returning
 * already registered objects for id-type-combinations (default: off). Can also update these unique
 * objects (default: off).
 */
public class ActivityObjectService {
  private final GraphDatabaseService fDatabase;
  private final Index<Node> fPersonNodes;

  private final IDManager fIdMan;

  private final ImplUtil fImpl;

  private final boolean fDedupObjects, fUpdateObjects;

  /**
   * Creates an activity object service retrieving data from the given database service, as
   * configured in the given configuration object. Throws a NullPointerException if the given
   * service is null.
   *
   * @param database
   *          database service to use
   * @param config
   *          configuration to use
   * @param impl
   *          implementation utility to use
   */
  public ActivityObjectService(GraphDatabaseService database, Map<String, String> config,
          IDManager idMan, ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("database service was null");
    }
    if (config == null) {
      throw new NullPointerException("configuration object was null");
    }
    if (idMan == null) {
      throw new NullPointerException("ID manager was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    final String dedup = config.get(ShindigConstants.ACT_OBJ_DEDUP_PROP);
    final String update = config.get(ShindigConstants.ACT_OBJ_UPDATE_PROP);

    if (dedup != null) {
      this.fDedupObjects = Boolean.parseBoolean(dedup);
    } else {
      this.fDedupObjects = false;
    }

    if (update != null) {
      this.fUpdateObjects = Boolean.parseBoolean(update);
    } else {
      this.fUpdateObjects = false;
    }

    this.fDatabase = database;
    this.fPersonNodes = this.fDatabase.index().forNodes(ShindigConstants.PERSON_NODES);

    this.fIdMan = idMan;

    this.fImpl = impl;
  }

  private Node create(Index<Node> index, Map<String, Object> object, String id) {
    final Node node = this.fDatabase.createNode();
    index.add(node, OSFields.ID_FIELD, id);

    // TODO: set properties
    new GraphActivityObject(node, this.fImpl).setData(object);

    // TODO: attachments, author, image?

    // TODO: generate URL if not available and internal object?

    return node;
  }

  /**
   * Returns a node representation of the given activity object. If there already is a node with the
   * right type and ID it is returned, otherwise a new node will be created.
   *
   * @param object
   *          activity object to get a node for
   * @return node representing the given object
   */
  public Node getObjectNode(final Map<String, Object> object) {
    Node node = null;

    // TODO: generate Id for objects without an ID?

    // add suffix to avoid collisions
    final String type = object.get(OSFields.OBJECT_TYPE) + ShindigConstants.ACT_OBJ_TYPE_SUFF;
    String id = (String) object.get(OSFields.ID_FIELD);

    if (ShindigConstants.PERSON_NODES.equals(object.get(OSFields.OBJECT_TYPE))) {
      // return person nodes for people
      node = this.fPersonNodes.get(OSFields.ID_FIELD, id).getSingle();
    } else if (id == null) {
      // generate ID if there is none
      id = this.fIdMan.genID(type);
    }

    /*
     * either the object is not a person or just not a person known to Shindig
     */
    if (node == null) {
      final Index<Node> index = this.fDatabase.index().forNodes(type);

      if (this.fDedupObjects) {
        node = index.get(OSFields.ID_FIELD, id).getSingle();
      }

      if (node == null) {
        // no node available or no deduplication
        node = create(index, object, id);
      } else if (this.fUpdateObjects) {
        // deduplication active
        // update properties
        new GraphActivityObject(node, this.fImpl).setData(object);

        // TODO: attachments, author, image?
      }
    }

    return node;
  }

  /**
   * Deletes a set of activity object nodes in case they aren't used by any activity entries
   * anymore.
   *
   * @param objects
   *          set of activity object candidates for deletion
   */
  public void deleteIfUnused(Set<Node> objects) {
    Index<Node> index = null;
    String type = null;

    final Transaction tx = this.fDatabase.beginTx();

    try {

      for (final Node object : objects) {
        type = (String) object.getProperty(OSFields.OBJECT_TYPE, null);

        // people should be the only objects that do not have types
        // TODO: additional types?
        if (type == null) {
          continue;
        }

        if (!object.hasRelationship()) {
          type += ShindigConstants.ACT_OBJ_TYPE_SUFF;
          index = this.fDatabase.index().forNodes(type);

          index.remove(object);

          object.delete();
        }
      }

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      e.printStackTrace();

      tx.failure();
      tx.finish();

      throw new RuntimeException(e);
    }
  }
}
