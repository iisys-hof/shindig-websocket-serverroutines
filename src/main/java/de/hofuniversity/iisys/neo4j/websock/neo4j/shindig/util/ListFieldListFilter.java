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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.EFilterOperation;

/**
 * Utility class for filtering a list of person nodes based on their list field lists. Currently
 * supported: e-mails, phone numbers, IMs, photos.
 */
// TODO: use from person filter?
public class ListFieldListFilter {
  /**
   * List of supported filterable fields represented by list field lists.
   */
  public static final Set<String> SUPPORTED_FIELDS = new HashSet<String>();

  private static final String VALUE_FIELD = "value";

  private static final String EMAILS_FIELD = "emails";
  private static final String IMS_FIELD = "ims";
  private static final String PHONES_FIELD = "phoneNumbers";
  private static final String PHOTOS_FIELD = "photos";

  static {
    ListFieldListFilter.SUPPORTED_FIELDS.add(ListFieldListFilter.EMAILS_FIELD);
    ListFieldListFilter.SUPPORTED_FIELDS.add(ListFieldListFilter.IMS_FIELD);
    ListFieldListFilter.SUPPORTED_FIELDS.add(ListFieldListFilter.PHONES_FIELD);
    ListFieldListFilter.SUPPORTED_FIELDS.add(ListFieldListFilter.PHOTOS_FIELD);
  }

  /**
   * Filters a list of person nodes by their list field list values. Only works for person nodes.
   * Known problem: objects that don't specify their own toString-method are filtered based on their
   * memory address
   *
   * @param nodes
   *          list of nodes to filter
   * @param options
   *          filter options as defined by websocket constants
   */
  public static void filterNodes(final List<Node> nodes, Map<String, Object> options) {
    final String filterKey = (String) options.get(WebsockConstants.FILTER_FIELD);

    String filterVal = (String) options.get(WebsockConstants.FILTER_VALUE);

    final String opVal = (String) options.get(WebsockConstants.FILTER_OPERATION);
    EFilterOperation filterOp = null;
    if (opVal != null) {
      filterOp = EFilterOperation.getTypeFor(opVal);
    }

    // check if the filter or filter operation are missing
    if (filterKey == null || filterKey.isEmpty()) {
      return;
    }

    // only the "has property" filter requires no value (not supported)
    if ((filterVal == null || filterVal.isEmpty())) {
      return;
    } else {
      filterVal = filterVal.toLowerCase();
    }

    boolean equals = false;
    if (EFilterOperation.EQUALS.equals(filterOp)) {
      equals = true;
    }

    boolean match = false;
    Node node = null;
    Object value = null;
    int size = nodes.size();

    Relationship rel = null;

    for (int i = 0; i < size;) {
      match = false;
      rel = null;
      node = nodes.get(i);

      // determine appropriate relation
      if (ListFieldListFilter.EMAILS_FIELD.equals(filterKey)) {
        rel = node.getSingleRelationship(Neo4jRelTypes.EMAILS, Direction.OUTGOING);
      } else if (ListFieldListFilter.PHONES_FIELD.equals(filterKey)) {
        rel = node.getSingleRelationship(Neo4jRelTypes.PHONE_NUMS, Direction.OUTGOING);
      } else if (ListFieldListFilter.IMS_FIELD.equals(filterKey)) {
        rel = node.getSingleRelationship(Neo4jRelTypes.IMS, Direction.OUTGOING);
      } else if (ListFieldListFilter.PHOTOS_FIELD.equals(filterKey)) {
        rel = node.getSingleRelationship(Neo4jRelTypes.PHOTOS, Direction.OUTGOING);
      }

      // check values for filter condition
      if (rel != null) {
        node = rel.getEndNode();

        value = node.getProperty(ListFieldListFilter.VALUE_FIELD, null);
        match = valueMatch(value, filterVal, equals);
      }

      // continue or remove element if it does not match the condition
      if (match) {
        ++i;
      } else {
        nodes.remove(i);
        --size;
      }
    }
  }

  private static boolean valueMatch(Object value, final String filterVal, boolean equals) {
    boolean match = false;

    if (value == null) {
      // no value, no match
      return match;
    } else if (equals) {
      // exact match
      if (value instanceof String[]) {
        for (final String s : (String[]) value) {
          if (s.toLowerCase().equals(filterVal)) {
            match = true;
            break;
          }
        }
      } else if (value.toString().toLowerCase().equals(filterVal)) {
        match = true;
      }
    } else {
      // contains
      if (value instanceof String[]) {
        for (final String s : (String[]) value) {
          if (s.toLowerCase().contains(filterVal)) {
            match = true;
            break;
          }
        }
      } else if (value.toString().toLowerCase().contains(filterVal)) {
        match = true;
      }
    }

    return match;
  }
}
