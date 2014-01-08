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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.ConvHelper;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class for converting a message collection stored in the database to a transferable
 * object and vice versa.
 */
public class GraphMessageCollection implements IGraphObject {
  private static final String TOTAL_FIELD = "total";
  private static final String UNREAD_FIELD = "unread";

  private static final String STATUS_FIELD = "status";
  private static final String NEW_NAME = "NEW";

  private static final ConvHelper HELPER = createHelper();

  private final Node fNode;

  private final ImplUtil fImpl;

  private static ConvHelper createHelper() {
    final Set<String> relMapped = new HashSet<String>();
    relMapped.add(GraphMessageCollection.TOTAL_FIELD);
    relMapped.add(GraphMessageCollection.UNREAD_FIELD);

    return new ConvHelper(null, relMapped, null);
  }

  /**
   * Creates a message collection converter, taking properties from the given node and its
   * relations. Throws a NullPointerException if the parameter is null.
   *
   * @param node
   *          node representing the message collection
   */
  public GraphMessageCollection(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates a message collection converter, taking properties from the given node and its
   * relations. Throws a NullPointerException if the parameter is null.
   *
   * @param node
   *          node representing the message collection
   */
  public GraphMessageCollection(Node node, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("underlying node was null");
    }

    this.fNode = node;

    this.fImpl = impl;
  }

  @Override
  public Map<String, Object> toMap(Set<String> fields) {
    final Map<String, Object> dto = this.fImpl.newMap();

    if (fields == null || fields.isEmpty()) {
      copyAllRelMapped(dto);

      for (final String key : this.fNode.getPropertyKeys()) {
        dto.put(key, this.fNode.getProperty(key));
      }
    } else {
      copyRelMapped(dto, fields);

      for (final String prop : fields) {
        if (!GraphMessageCollection.HELPER.getRelationshipMapped().contains(prop)
                && this.fNode.hasProperty(prop)) {
          dto.put(prop, this.fNode.getProperty(prop));
        }
      }
    }

    return dto;
  }

  private void copyRelMapped(Map<String, Object> dto, Set<String> props) {
    if (props.contains(GraphMessageCollection.TOTAL_FIELD)) {
      copyTotal(dto);
    }

    if (props.contains(GraphMessageCollection.UNREAD_FIELD)) {
      copyUnread(dto);
    }
  }

  private void copyAllRelMapped(Map<String, Object> dto) {
    copyTotal(dto);
    copyUnread(dto);
  }

  private void copyTotal(Map<String, Object> dto) {
    // counts how many "contained" relations the node has
    final Iterable<Relationship> contained = this.fNode.getRelationships(Neo4jRelTypes.CONTAINS);
    final Iterator<Relationship> conRels = contained.iterator();
    Integer total = 0;

    try {
      while (true) {
        conRels.next();
        ++total;
      }
    } catch (final NoSuchElementException e) {
      // expected
    }

    dto.put(GraphMessageCollection.TOTAL_FIELD, total);
  }

  private void copyUnread(Map<String, Object> dto) {
    // check all "contained"-relations for an "unread" status
    final Iterable<Relationship> contained = this.fNode.getRelationships(Neo4jRelTypes.CONTAINS);
    Integer unread = 0;

    for (final Relationship rel : contained) {
      if (rel.getProperty(GraphMessageCollection.STATUS_FIELD, "").equals(
              GraphMessageCollection.NEW_NAME)) {
        ++unread;
      }
    }

    dto.put(GraphMessageCollection.UNREAD_FIELD, unread);
  }

  @Override
  public void setData(Map<String, ?> map) {
    String key = null;
    Object value = null;

    for (final Entry<String, ?> mapE : map.entrySet()) {
      key = mapE.getKey();
      value = mapE.getValue();

      if (!GraphMessageCollection.UNREAD_FIELD.equals(key)
              && !GraphMessageCollection.TOTAL_FIELD.equals(key) && value != null) {
        this.fNode.setProperty(key, value);
      }
    }
  }

}
