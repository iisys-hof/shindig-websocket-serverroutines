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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.ConvHelper;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.SimpleGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class for activity entry nodes in the graph that can create a transferable object.
 */
public class GraphActivityEntry implements IGraphObject {
  public static final String ACTOR_FIELD = "actor";
  public static final String GENERATOR_FIELD = "generator";
  public static final String OBJECT_FIELD = "object";
  public static final String PROVIDER_FIELD = "provider";
  public static final String TARGET_FIELD = "target";
  private static final String ICON_FIELD = "icon";

  private static final String OPENSOCIAL_FIELD = "opensocial";

  private static final Set<String> NON_ATOMIC = new HashSet<String>();
  private static final ConvHelper HELPER = createHelper();

  private final Node fNode;

  private final ImplUtil fImpl;

  private static ConvHelper createHelper() {
    final Set<String> relMapped = new HashSet<String>();
    relMapped.add(GraphActivityEntry.ACTOR_FIELD);
    relMapped.add(GraphActivityEntry.GENERATOR_FIELD);
    relMapped.add(GraphActivityEntry.OBJECT_FIELD);
    relMapped.add(GraphActivityEntry.PROVIDER_FIELD);
    relMapped.add(GraphActivityEntry.TARGET_FIELD);
    relMapped.add(GraphActivityEntry.ICON_FIELD);

    final Set<String> conSens = new HashSet<String>();
    conSens.add(GraphActivityEntry.OPENSOCIAL_FIELD);

    GraphActivityEntry.NON_ATOMIC.addAll(relMapped);
    GraphActivityEntry.NON_ATOMIC.addAll(conSens);

    return new ConvHelper(null, relMapped, conSens);
  }

  /**
   * Creates an activity entry converter, taking properties from the given node. Throws a
   * NullPointerException if the parameter is null.
   *
   * @param node
   *          node representing the activity entry
   */
  public GraphActivityEntry(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates an activity entry converter, taking properties from the given node. Throws a
   * NullPointerException if the parameter is null.
   *
   * @param node
   *          node representing the activity entry
   */
  public GraphActivityEntry(Node node, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("underlying node was null");
    }

    this.fImpl = impl;

    this.fNode = node;
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

      final Set<String> newProps = new HashSet<String>(fields);
      newProps.removeAll(GraphActivityEntry.HELPER.getRelationshipMapped());

      for (final String prop : newProps) {
        if (this.fNode.hasProperty(prop)) {
          dto.put(prop, this.fNode.getProperty(prop));
        }
      }
    }

    return dto;
  }

  private void copyRelMapped(final Map<String, Object> dto, final Set<String> properties) {
    // TODO: wrappers for person object
    if (properties.contains(GraphActivityEntry.ACTOR_FIELD)) {
      copyActor(dto);
    }

    if (properties.contains(GraphActivityEntry.GENERATOR_FIELD)) {
      copyGenerator(dto);
    }

    if (properties.contains(GraphActivityEntry.OBJECT_FIELD)) {
      copyObject(dto);
    }

    if (properties.contains(GraphActivityEntry.PROVIDER_FIELD)) {
      copyProvider(dto);
    }

    if (properties.contains(GraphActivityEntry.TARGET_FIELD)) {
      copyTarget(dto);
    }

    if (properties.contains(GraphActivityEntry.ICON_FIELD)) {
      copyIcon(dto);
    }
  }

  private void copyAllRelMapped(final Map<String, Object> dto) {
    // TODO: wrappers for person object
    copyActor(dto);
    copyGenerator(dto);
    copyObject(dto);
    copyProvider(dto);
    copyTarget(dto);
    copyIcon(dto);
  }

  private void copyActor(Map<String, Object> dto) {
    final Relationship actRel = this.fNode.getSingleRelationship(ShindigRelTypes.ACTOR,
            Direction.OUTGOING);

    if (actRel != null) {
      final Node actNode = actRel.getEndNode();
      final Map<String, Object> actDTO = new GraphActivityObject(actNode, this.fImpl).toMap(null);

      dto.put(GraphActivityEntry.ACTOR_FIELD, actDTO);
    }
  }

  private void copyGenerator(Map<String, Object> dto) {
    final Relationship genRel = this.fNode.getSingleRelationship(ShindigRelTypes.GENERATOR,
            Direction.OUTGOING);

    if (genRel != null) {
      final Node genNode = genRel.getEndNode();
      final Map<String, Object> genDTO = new GraphActivityObject(genNode, this.fImpl).toMap(null);

      dto.put(GraphActivityEntry.GENERATOR_FIELD, genDTO);
    }
  }

  private void copyObject(Map<String, Object> dto) {
    final Relationship objRel = this.fNode.getSingleRelationship(ShindigRelTypes.OBJECT,
            Direction.OUTGOING);

    if (objRel != null) {
      final Node objNode = objRel.getEndNode();
      final Map<String, Object> objDTO = new GraphActivityObject(objNode, this.fImpl).toMap(null);

      dto.put(GraphActivityEntry.OBJECT_FIELD, objDTO);
    }
  }

  private void copyProvider(Map<String, Object> dto) {
    final Relationship provRel = this.fNode.getSingleRelationship(ShindigRelTypes.PROVIDER,
            Direction.OUTGOING);

    if (provRel != null) {
      final Node provNode = provRel.getEndNode();
      final Map<String, Object> provDTO = new GraphActivityObject(provNode, this.fImpl).toMap(null);

      dto.put(GraphActivityEntry.PROVIDER_FIELD, provDTO);
    }
  }

  private void copyTarget(Map<String, Object> dto) {
    final Relationship tarRel = this.fNode.getSingleRelationship(ShindigRelTypes.TARGET,
            Direction.OUTGOING);

    if (tarRel != null) {
      final Node tarNode = tarRel.getEndNode();
      final Map<String, Object> tarDTO = new GraphActivityObject(tarNode, this.fImpl).toMap(null);

      dto.put(GraphActivityEntry.TARGET_FIELD, tarDTO);
    }
  }

  private void copyIcon(Map<String, Object> dto) {
    final Relationship iconRel = this.fNode.getSingleRelationship(ShindigRelTypes.HAS_ICON,
            Direction.OUTGOING);

    if (iconRel != null) {
      final Node iconNode = iconRel.getEndNode();
      final Map<String, Object> iconDTO = new SimpleGraphObject(iconNode, this.fImpl).toMap(null);

      dto.put(GraphActivityEntry.ICON_FIELD, iconDTO);
    }
  }

  @Override
  public void setData(Map<String, ?> map) {
    String key = null;
    Object value = null;

    for (final Entry<String, ?> fieldE : map.entrySet()) {
      key = fieldE.getKey();
      value = fieldE.getValue();

      if (GraphActivityEntry.NON_ATOMIC.contains(key)) {
        continue;
      }

      if (value != null) {
        this.fNode.setProperty(key, value);
      } else {
        this.fNode.removeProperty(key);
      }
    }
  }
}
