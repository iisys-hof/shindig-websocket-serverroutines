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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.ConvHelper;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.SimpleGraphObject;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class for activity object nodes in the graph that can create a transferable object.
 */
public class GraphActivityObject implements IGraphObject {
  private static final String ATTACHMENTS_FIELD = "attachments";
  private static final String AUTHOR_FIELD = "author";
  private static final String IMAGE_FIELD = "image";

  private static final String OPENSOCIAL_FIELD = "opensocial";

  private static final Set<String> NON_ATOMIC = new HashSet<String>();
  private static final ConvHelper HELPER = createHelper();

  private final Node fNode;

  private final ImplUtil fImpl;

  private static ConvHelper createHelper() {
    final Set<String> relMapped = new HashSet<String>();
    relMapped.add(GraphActivityObject.ATTACHMENTS_FIELD);
    relMapped.add(GraphActivityObject.AUTHOR_FIELD);
    relMapped.add(GraphActivityObject.IMAGE_FIELD);

    final Set<String> conSens = new HashSet<String>();
    conSens.add(GraphActivityObject.OPENSOCIAL_FIELD);

    GraphActivityObject.NON_ATOMIC.addAll(relMapped);
    GraphActivityObject.NON_ATOMIC.addAll(conSens);

    return new ConvHelper(null, relMapped, conSens);
  }

  /**
   * Creates an activity object converter, taking properties from the given node. Throws a
   * NullPointerException if the parameter is null.
   *
   * @param node
   *          node representing the activity object
   */
  public GraphActivityObject(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates an activity object converter, taking properties from the given node. Throws a
   * NullPointerException if the parameter is null.
   *
   * @param node
   *          node representing the activity object
   */
  public GraphActivityObject(Node node, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("Underlying node was null");
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

      final Set<String> newProps = new HashSet<String>(fields);
      newProps.removeAll(GraphActivityObject.HELPER.getRelationshipMapped());

      for (final String prop : newProps) {
        if (this.fNode.hasProperty(prop)) {
          dto.put(prop, this.fNode.getProperty(prop));
        }
      }
    }

    return dto;
  }

  private void copyRelMapped(final Map<String, Object> dto, final Set<String> properties) {
    if (properties.contains(GraphActivityObject.ATTACHMENTS_FIELD)) {
      copyAttachments(dto);
    }
    if (properties.contains(GraphActivityObject.AUTHOR_FIELD)) {
      copyAuthor(dto);
    }
    if (properties.contains(GraphActivityObject.IMAGE_FIELD)) {
      copyImage(dto);
    }
  }

  private void copyAllRelMapped(final Map<String, Object> dto) {
    copyAttachments(dto);
    copyAuthor(dto);
    copyImage(dto);
  }

  private void copyAttachments(final Map<String, Object> dto) {
    final List<Map<String, Object>> attachments = this.fImpl.newList();
    final Iterable<Relationship> attRels = this.fNode.getRelationships(Direction.OUTGOING,
            Neo4jRelTypes.ATTACHED);

    // TODO: wrapper for people?

    Node attNode = null;
    Map<String, Object> attDTO = null;
    for (final Relationship rel : attRels) {
      attNode = rel.getEndNode();
      attDTO = new GraphActivityObject(attNode).toMap(null);
      attachments.add(attDTO);
    }

    if (!attachments.isEmpty()) {
      dto.put(GraphActivityObject.ATTACHMENTS_FIELD, attachments);
    }
  }

  private void copyAuthor(final Map<String, Object> dto) {
    final Relationship authRel = this.fNode.getSingleRelationship(Neo4jRelTypes.AUTHOR,
            Direction.OUTGOING);

    // TODO: wrapper for people?

    if (authRel != null) {
      final Node authNode = authRel.getEndNode();
      final Map<String, Object> authDTO = new GraphActivityObject(authNode).toMap(null);
      dto.put(GraphActivityObject.AUTHOR_FIELD, authDTO);
    }
  }

  private void copyImage(final Map<String, Object> dto) {
    final Relationship imgRel = this.fNode.getSingleRelationship(Neo4jRelTypes.HAS_ICON,
            Direction.OUTGOING);

    if (imgRel != null) {
      final Node imgNode = imgRel.getEndNode();
      final Map<String, Object> imgDTO = new SimpleGraphObject(imgNode).toMap(null);
      dto.put(GraphActivityObject.IMAGE_FIELD, imgDTO);
    }
  }

  @Override
  public void setData(Map<String, ?> map) {
    String key = null;
    Object value = null;

    for (final Entry<String, ?> fieldE : map.entrySet()) {
      key = fieldE.getKey();
      value = fieldE.getValue();

      if (GraphActivityObject.NON_ATOMIC.contains(key)) {
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
