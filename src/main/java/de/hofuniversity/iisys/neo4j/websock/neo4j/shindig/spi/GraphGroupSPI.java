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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphGroup;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the group service retrieving group data from the Neo4j graph database.
 */
public class GraphGroupSPI {
  private static final String TITLE_FIELD = "title";

  private final GraphPersonSPI fPersonSPI;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  /**
   * Creates a graph group service using data from the given provider of person data. Throws a
   * NullPointerException if the given person service is null.
   *
   * @param personSPI
   *          person data provider to use
   * @param impl
   *          implementation utility to use
   */
  public GraphGroupSPI(GraphPersonSPI personSPI, ImplUtil impl) {
    if (personSPI == null) {
      throw new NullPointerException("person service was null");
    }

    this.fPersonSPI = personSPI;

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  /**
   * Retrieves groups the specified user is a member of.
   *
   * @param userId
   *          user to retrieve groups for
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of groups
   */
  public ListResult getGroups(String userId, Map<String, Object> options, List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final List<Node> groupNodes = new LinkedList<Node>();

    final List<Map<String, Object>> groupList = this.fImpl.newList();

    final Node person = this.fPersonSPI.getPersonNode(userId);

    final Iterable<Relationship> memberships = person.getRelationships(Neo4jRelTypes.MEMBER_OF);

    Node groupNode = null;
    for (final Relationship link : memberships) {
      groupNode = link.getEndNode();
      groupNodes.add(groupNode);
    }

    // filter
    NodeFilter.filterNodes(groupNodes, options);
    // TODO: other filters?

    // sort as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null) {
      options.put(WebsockConstants.SORT_FIELD, GraphGroupSPI.TITLE_FIELD);
    }
    NodeSorter.sortNodes(groupNodes, options);

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = groupNodes.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(groupNodes.size(), first + max);

    // convert the items requested
    Map<String, Object> dto = null;
    for (int index = first; index < last; ++index) {
      dto = new GraphGroup(groupNodes.get(index), this.fImpl).toMap(fieldSet);
      groupList.add(dto);
    }

    final ListResult groups = new ListResult(groupList);
    groups.setFirst(first);
    groups.setMax(max);
    groups.setTotal(groupNodes.size());
    return groups;
  }
}
