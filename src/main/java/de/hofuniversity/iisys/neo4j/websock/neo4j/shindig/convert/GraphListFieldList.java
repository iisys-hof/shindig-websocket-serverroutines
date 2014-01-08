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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;

import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class for list field list nodes in the graph that can create a transferable object.
 */
public class GraphListFieldList implements IGraphObject {
  private static final String VALUE_FIELD = "value";
  private static final String TYPE_FIELD = "type";
  private static final String PRIMARY_FIELD = "primary";

  private final Node fNode;

  private final ImplUtil fImpl;

  /**
   * Creates a list field list converter, taking properties from the given node. Throws a
   * NullPointerException if the parameter is null.
   *
   * @param node
   *          node representing the list of list fields
   */
  public GraphListFieldList(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates a list field list converter, taking properties from the given node. Throws a
   * NullPointerException if the parameter is null.
   *
   * @param node
   *          node representing the list of list fields
   */
  public GraphListFieldList(Node node, ImplUtil impl) {
    this.fNode = node;

    this.fImpl = impl;
  }

  @Override
  public Map<String, Object> toMap(Set<String> fields) {
    final Map<String, Object> list = this.fImpl.newMap();

    // get fields from database
    final String[] values = (String[]) this.fNode.getProperty(GraphListFieldList.VALUE_FIELD, null);
    final String[] types = (String[]) this.fNode.getProperty(GraphListFieldList.TYPE_FIELD, null);

    final Integer primary = (Integer) this.fNode
            .getProperty(GraphListFieldList.PRIMARY_FIELD, null);

    // set available list fields
    if (values != null) {
      list.put(GraphListFieldList.VALUE_FIELD, values);

      if (types != null) {
        list.put(GraphListFieldList.TYPE_FIELD, types);
      }

      if (primary != null) {
        list.put(GraphListFieldList.PRIMARY_FIELD, primary);
      }
    }

    return list;
  }

  @SuppressWarnings("unchecked")
  private String[] getArray(Object value) {
    String[] array = null;

    if (value != null) {
      if (value instanceof String[]) {
        array = (String[]) value;
      } else {
        final List<String> list = (List<String>) value;
        array = list.toArray(new String[list.size()]);
      }
    }

    return array;
  }

  @Override
  public void setData(Map<String, ?> list) {
    final String[] values = getArray(list.get(GraphListFieldList.VALUE_FIELD));

    // delete content if empty
    if (list == null || list.isEmpty() || values == null || values.length == 0) {
      this.fNode.removeProperty(GraphListFieldList.VALUE_FIELD);
      this.fNode.removeProperty(GraphListFieldList.TYPE_FIELD);
      this.fNode.removeProperty(GraphListFieldList.PRIMARY_FIELD);
    } else {
      String[] types = getArray(list.get(GraphListFieldList.TYPE_FIELD));

      final Integer primary = (Integer) list.get(GraphListFieldList.PRIMARY_FIELD);

      final int size = values.length;

      // cut or fill array size mismatches
      if (types == null || types.length != size) {
        final String[] newTypes = new String[size];

        if (types != null) {
          for (int i = 0; i < size && i < types.length; ++i) {
            newTypes[i] = types[i];
          }
        }

        types = newTypes;
      }

      // replace null values
      for (int i = 0; i < size; ++i) {
        if (values[i] == null) {
          values[i] = "";
        }

        if (types[i] == null) {
          types[i] = "";
        }
      }

      this.fNode.setProperty(GraphListFieldList.VALUE_FIELD, values);
      this.fNode.setProperty(GraphListFieldList.TYPE_FIELD, types);

      if (primary != null) {
        this.fNode.setProperty(GraphListFieldList.PRIMARY_FIELD, primary);
      } else {
        this.fNode.removeProperty(GraphListFieldList.PRIMARY_FIELD);
      }
    }
  }
}
