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
 * Converter class converting album objects stored in the graph to transferable objects and vice
 * versa.
 */
public class GraphAlbum implements IGraphObject {
  private static final String DESCRIPTION_FIELD = "description";
  private static final String ID_FIELD = "id";
  private static final String LOCATION_FIELD = "location";
  private static final String MEDIA_ITEM_COUNT_FIELD = "mediaItemCount";
  private static final String MEDIA_MIME_TYPE_FIELD = "mediaMimeType";
  private static final String MEDIA_TYPE_FIELD = "mediaType";
  private static final String OWNER_ID_FIELD = "ownerId";
  private static final String THUMBNAIL_FIELD = "thumbnailUrl";
  private static final String TITLE_FIELD = "title";

  private static final String MIME_TYPE_FIELD = "mimeType";
  private static final String TYPE_FIELD = "type";

  private static final ConvHelper HELPER = createHelper();

  private final Node fNode;

  private final ImplUtil fImpl;

  private static ConvHelper createHelper() {
    final Set<String> relMapped = new HashSet<String>();
    relMapped.add(GraphAlbum.LOCATION_FIELD);
    relMapped.add(GraphAlbum.MEDIA_ITEM_COUNT_FIELD);
    relMapped.add(GraphAlbum.MEDIA_MIME_TYPE_FIELD);
    relMapped.add(GraphAlbum.MEDIA_TYPE_FIELD);

    return new ConvHelper(null, relMapped, null);
  }

  /**
   * Creates an album in the graph based on an underlying node. Throws a NullPointerException if the
   * given node is null.
   *
   * @param node
   *          node this album is based on.
   */
  public GraphAlbum(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates an album in the graph based on an underlying node. Throws a NullPointerException if the
   * given node is null.
   *
   * @param node
   *          node this album is based on.
   * @param impl
   *          implementation utility to use
   */
  public GraphAlbum(Node node, ImplUtil impl) {
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

      copyAllRelMapped(dto);
    } else {
      copyRelMapped(dto, fields);

      final Set<String> newProps = new HashSet<String>(fields);
      newProps.removeAll(GraphAlbum.HELPER.getRelationshipMapped());
      fields = newProps;

      for (final String prop : fields) {
        if (this.fNode.hasProperty(prop)) {
          dto.put(prop, this.fNode.getProperty(prop));
        }
      }
    }

    return dto;
  }

  private void copyRelMapped(final Map<String, Object> dto, final Set<String> properties) {
    if (properties.contains(GraphAlbum.LOCATION_FIELD)) {
      copyLocation(dto);
    }

    if (properties.contains(GraphAlbum.MEDIA_ITEM_COUNT_FIELD)) {
      copyMediaItemCount(dto);
    }

    if (properties.contains(GraphAlbum.MEDIA_MIME_TYPE_FIELD)) {
      copyMediaMimeType(dto);
    }

    if (properties.contains(GraphAlbum.MEDIA_TYPE_FIELD)) {
      copyMediaType(dto);
    }
  }

  private void copyAllRelMapped(final Map<String, Object> dto) {
    copyLocation(dto);
    copyMediaItemCount(dto);
    copyMediaMimeType(dto);
    copyMediaType(dto);
  }

  private void copyLocation(final Map<String, Object> dto) {
    final Relationship locRel = this.fNode.getSingleRelationship(Neo4jRelTypes.LOCATED_AT,
            Direction.OUTGOING);
    if (locRel != null) {
      final Map<String, Object> addMap = new SimpleGraphObject(locRel.getEndNode(), this.fImpl)
              .toMap(null);
      dto.put(GraphAlbum.LOCATION_FIELD, addMap);
    }
  }

  private void copyMediaItemCount(final Map<String, Object> dto) {
    int count = 0;
    final Iterable<Relationship> itemRels = this.fNode.getRelationships(Neo4jRelTypes.CONTAINS,
            Direction.OUTGOING);

    final Iterator<Relationship> itemIter = itemRels.iterator();

    while (itemIter.hasNext()) {
      ++count;
      itemIter.next();
    }

    dto.put(GraphAlbum.MEDIA_ITEM_COUNT_FIELD, count);
  }

  private void copyMediaMimeType(final Map<String, Object> dto) {
    Object type = null;
    final Set<String> mimes = new HashSet<String>();

    final Iterable<Relationship> itemRels = this.fNode.getRelationships(Neo4jRelTypes.CONTAINS,
            Direction.OUTGOING);

    for (final Relationship rel : itemRels) {
      type = rel.getEndNode().getProperty(GraphAlbum.MIME_TYPE_FIELD, null);

      if (type != null) {
        mimes.add(type.toString());
      }
    }

    final List<String> mimeTypes = this.fImpl.newList();
    mimeTypes.addAll(mimes);
    dto.put(GraphAlbum.MEDIA_MIME_TYPE_FIELD, mimeTypes);
  }

  private void copyMediaType(final Map<String, Object> dto) {
    Object type = null;
    final Set<String> types = new HashSet<String>();

    final Iterable<Relationship> itemRels = this.fNode.getRelationships(Neo4jRelTypes.CONTAINS,
            Direction.OUTGOING);

    for (final Relationship rel : itemRels) {
      type = rel.getEndNode().getProperty(GraphAlbum.TYPE_FIELD, null);

      if (type != null) {
        types.add(type.toString());
      }
    }

    final List<String> mediaTypes = this.fImpl.newList();
    mediaTypes.addAll(types);
    dto.put(GraphAlbum.MEDIA_TYPE_FIELD, mediaTypes);
  }

  @Override
  public void setData(final Map<String, ?> map) {
    // collect new values
    final Map<String, Object> newValues = new HashMap<String, Object>();

    // atomic
    newValues.put(GraphAlbum.DESCRIPTION_FIELD, map.get(GraphAlbum.DESCRIPTION_FIELD));
    newValues.put(GraphAlbum.ID_FIELD, map.get(GraphAlbum.ID_FIELD));
    newValues.put(GraphAlbum.OWNER_ID_FIELD, map.get(GraphAlbum.OWNER_ID_FIELD));
    newValues.put(GraphAlbum.THUMBNAIL_FIELD, map.get(GraphAlbum.THUMBNAIL_FIELD));
    newValues.put(GraphAlbum.TITLE_FIELD, map.get(GraphAlbum.TITLE_FIELD));

    // location
    @SuppressWarnings("unchecked")
    final Map<String, Object> location = (Map<String, Object>) map.get(GraphAlbum.LOCATION_FIELD);

    if (location != null) {
      Node addNode = null;
      final Relationship locRel = this.fNode.getSingleRelationship(Neo4jRelTypes.LOCATED_AT,
              Direction.OUTGOING);
      if (locRel != null) {
        addNode = locRel.getEndNode();
        locRel.delete();

        if (!addNode.hasRelationship()) {
          addNode.delete();
        }
      }

      addNode = this.fNode.getGraphDatabase().createNode();
      new SimpleGraphObject(addNode, this.fImpl).setData(location);
      this.fNode.createRelationshipTo(addNode, Neo4jRelTypes.LOCATED_AT);
    }

    // set new values
    for (final Entry<String, Object> valE : newValues.entrySet()) {
      if (valE.getValue() != null) {
        this.fNode.setProperty(valE.getKey(), valE.getValue());
      }
    }
  }

}
