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
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;

/**
 * Utility class for filtering a list of person nodes based on all their fields and connected nodes
 * or friendships.
 */
public class PersonFilter {
  private static final String VALUE_FIELD = "value";
  private static final String TYPE_FIELD = "type";

  /**
   * Filters a list of person nodes by the values stored in the database. Only works for Returns
   * without an error if parameters not compatible with this kind of search. Known problem: objects
   * that don't specify their own toString-method are filtered based on their memory address
   *
   * @param nodes
   *          list of nodes to filter
   * @param filter
   *          value to filter by
   */
  public static void filterNodes(final List<Node> nodes, String filter) {
    if (filter == null || filter.isEmpty()) {
      return;
    }

    final String filterVal = filter.toLowerCase();

    boolean match = false;
    Node node = null;
    int size = nodes.size();

    for (int i = 0; i < size;) {
      match = false;
      node = nodes.get(i);

      // filter person's own fields
      match = fieldsMatch(node, filterVal);

      // filter addresses
      if (!match) {
        match = matchAdresses(node, filterVal);
      }

      // filter e-mail addresses
      if (!match) {
        match = matchMails(node, filterVal);
      }

      // filter phone numbers
      if (!match) {
        match = matchPhones(node, filterVal);
      }

      // filter organizations
      if (!match) {
        match = matchOrganizations(node, filterVal);
      }

      // remove if no match
      if (match) {
        ++i;
      } else {
        nodes.remove(i);
        --size;
      }
    }
  }

  private static boolean matchAdresses(Node person, final String filterVal) {
    boolean match = false;

    final Iterable<Relationship> locRels = person.getRelationships(Neo4jRelTypes.LOCATED_AT);

    Node node = null;
    for (final Relationship rel : locRels) {
      node = rel.getEndNode();
      match = fieldsMatch(node, filterVal);

      if (match) {
        break;
      }
    }

    return match;
  }

  private static boolean matchMails(Node person, final String filterVal) {
    boolean match = false;

    final Relationship rel = person.getSingleRelationship(Neo4jRelTypes.EMAILS, Direction.OUTGOING);

    if (rel != null) {
      final Node node = rel.getEndNode();

      Object value = node.getProperty(PersonFilter.TYPE_FIELD, null);
      match = valueMatch(value, filterVal);

      if (!match) {
        value = node.getProperty(PersonFilter.VALUE_FIELD, null);
        match = valueMatch(value, filterVal);
      }
    }

    return match;
  }

  private static boolean matchPhones(Node person, final String filterVal) {
    boolean match = false;

    final Relationship rel = person.getSingleRelationship(Neo4jRelTypes.PHONE_NUMS,
            Direction.OUTGOING);

    if (rel != null) {
      final Node node = rel.getEndNode();

      Object value = node.getProperty(PersonFilter.TYPE_FIELD, null);
      match = valueMatch(value, filterVal);

      if (!match) {
        value = node.getProperty(PersonFilter.VALUE_FIELD, null);
        match = valueMatch(value, filterVal);
      }
    }

    return match;
  }

  private static boolean matchOrganizations(Node person, final String filterVal) {
    boolean match = false;

    final Iterable<Relationship> affiliations = person.getRelationships(Neo4jRelTypes.AFFILIATED);

    Node organization = null;
    for (final Relationship aff : affiliations) {
      organization = aff.getEndNode();

      match = fieldsMatch(aff, filterVal);

      if (!match) {
        match = fieldsMatch(organization, filterVal);
      }

      if (match) {
        break;
      }
    }

    return match;
  }

  private static boolean fieldsMatch(final PropertyContainer cont, final String filterVal) {
    boolean match = false;

    for (final String key : cont.getPropertyKeys()) {
      match = valueMatch(cont.getProperty(key), filterVal);

      if (match) {
        break;
      }
    }

    return match;
  }

  private static boolean valueMatch(Object value, final String filterVal) {
    boolean match = false;

    if (value == null) {
      // no further action
    } else if (value instanceof String[]) {
      for (final String s : (String[]) value) {
        if (s.toLowerCase().contains(filterVal)) {
          match = true;
          break;
        }
      }
    } else if (value.toString().toLowerCase().contains(filterVal)) {
      match = true;
    }

    return match;
  }

  /**
   * Filters a list of person nodes, removing any people that are not friends with the given person,
   * except for the person node itself. Parameters may not be null.
   *
   * @param people
   *          list of person nodes to filter
   * @param friend
   *          person they need to be friends with
   */
  public static void filterNodes(List<Node> people, Node friend) {
    // skip filtering if there is nothing to filter
    if (friend == null || people == null) {
      return;
    }

    final Set<Node> friends = new HashSet<Node>();
    friends.add(friend);

    final Iterable<Relationship> friendRels = friend.getRelationships(Direction.OUTGOING,
            Neo4jRelTypes.FRIEND_OF);
    for (final Relationship fRel : friendRels) {
      friends.add(fRel.getEndNode());
    }

    people.retainAll(friends);
  }
}
