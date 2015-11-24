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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.Node;

import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.ConvHelper;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class converting group objects stored in the graph to transferable objects and vice
 * versa.
 */
public class GraphGroup implements IGraphObject {
  private static final String ID_FIELD = "id";

  private static final ConvHelper HELPER = createHelper();

  private final Node fNode;

  private final ImplUtil fImpl;

  private static ConvHelper createHelper() {
    final Map<String, List<String>> splitFields = new HashMap<String, List<String>>();

    final List<String> idList = new ArrayList<String>();
    idList.add(GraphGroup.ID_FIELD + "_type");
    splitFields.put(GraphGroup.ID_FIELD, idList);

    return new ConvHelper(splitFields, null, null);
  }

  /**
   * Creates a group converter, taking properties from the given node. Throws a NullPointerException
   * if the parameter is null.
   *
   * @param node
   *          node representing the group
   */
  public GraphGroup(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates a group converter, taking properties from the given node. Throws a NullPointerException
   * if the parameter is null.
   *
   * @param node
   *          node representing the group
   */
  public GraphGroup(Node node, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("underyling node was null");
    }

    this.fNode = node;

    this.fImpl = impl;
  }

  @Override
  public Map<String, Object> toMap(final Set<String> fields) {
    final Map<String, Object> dto = this.fImpl.newMap();

    if (fields == null || fields.isEmpty()) {
      for (final String key : this.fNode.getPropertyKeys()) {
        dto.put(key, this.fNode.getProperty(key));
      }
    } else {
      if (fields.contains(GraphGroup.ID_FIELD)) {
        fields.add(GraphGroup.ID_FIELD + "_type");
      }

      for (final String prop : fields) {
        if (this.fNode.hasProperty(prop)) {
          dto.put(prop, this.fNode.getProperty(prop));
        }
      }
    }

    return dto;
  }

  @Override
  public void setData(Map<String, ?> map) {
    for (final Entry<String, ?> fieldE : map.entrySet()) {
      if (fieldE.getValue() == null) {
        this.fNode.removeProperty(fieldE.getKey());
      } else {
        this.fNode.setProperty(fieldE.getKey(), fieldE.getValue());
      }
    }
  }

}
