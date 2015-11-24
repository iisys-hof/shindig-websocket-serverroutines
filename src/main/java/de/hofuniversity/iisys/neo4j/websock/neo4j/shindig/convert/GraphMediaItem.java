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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.SimpleGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class converting media item objects stored in the graph to transferable objects and
 * vice versa.
 */
public class GraphMediaItem implements IGraphObject {
  private static final String LOCATION_FIELD = "location";

  private final Node fNode;

  private final ImplUtil fImpl;

  /**
   * Creates a media item in the graph based on an underlying node. Throws a NullPointerException if
   * the given node is null.
   *
   * @param node
   *          node this media item is based on.
   */
  public GraphMediaItem(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates a media item in the graph based on an underlying node. Throws a NullPointerException if
   * the given node is null.
   *
   * @param node
   *          node this media item is based on.
   * @param impl
   *          implementation utility to use
   */
  public GraphMediaItem(Node node, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("underlying node was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    this.fNode = node;
    this.fImpl = impl;
  }

  @Override
  public Map<String, Object> toMap(Set<String> fields) {
    final Map<String, Object> dto = this.fImpl.newMap();

    if (fields == null || fields.isEmpty()) {
      for (final String key : this.fNode.getPropertyKeys()) {
        dto.put(key, this.fNode.getProperty(key));
      }

      copyLocation(dto);
    } else {
      for (final String key : fields) {
        if (this.fNode.hasProperty(key)) {
          dto.put(key, this.fNode.getProperty(key));
        }
      }

      if (fields.contains(GraphMediaItem.LOCATION_FIELD)) {
        copyLocation(dto);
      }
    }

    return dto;
  }

  private void copyLocation(final Map<String, Object> dto) {
    final Relationship locRel = this.fNode.getSingleRelationship(ShindigRelTypes.LOCATED_AT,
            Direction.OUTGOING);

    if (locRel != null) {
      final Map<String, Object> addMap = new SimpleGraphObject(locRel.getEndNode(), this.fImpl)
              .toMap(null);
      dto.put(GraphMediaItem.LOCATION_FIELD, addMap);
    }
  }

  @Override
  public void setData(Map<String, ?> map) {
    // set new values
    String key = null;
    Object value = null;
    for (final Entry<String, ?> mapE : map.entrySet()) {
      key = mapE.getKey();
      value = mapE.getValue();

      if (GraphMediaItem.LOCATION_FIELD.equals(key)) {
        continue;
      } else if (value == null) {
        this.fNode.removeProperty(key);
      } else {
        this.fNode.setProperty(key, value);
      }
    }

    // update location
    @SuppressWarnings("unchecked")
    final Map<String, Object> locMap = (Map<String, Object>) map.get(GraphMediaItem.LOCATION_FIELD);

    if (locMap != null) {
      Node addNode = null;
      final Relationship locRel = this.fNode.getSingleRelationship(ShindigRelTypes.LOCATED_AT,
              Direction.OUTGOING);

      if (locRel != null) {
        addNode = locRel.getEndNode();
        locRel.delete();

        if (!addNode.hasRelationship()) {
          addNode.delete();
        }
      }
    }
  }

}
