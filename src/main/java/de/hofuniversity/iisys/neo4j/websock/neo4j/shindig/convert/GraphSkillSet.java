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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class converting skill sets stored in the graph to transferable objects. These skill
 * sets contain the name of the skill and people linking it to a person.
 */
public class GraphSkillSet implements IGraphObject {

  private static final String NAME_FIELD = "name";
  private static final String PEOPLE_FIELD = "people";
  private static final String CONFIRMED_FIELD = "confirmed";

  private static final Set<String> PERSON_FIELDS;
  private static final Map<String, Object> PERSON_SORTING;

  static {
    PERSON_FIELDS = new HashSet<String>();
    GraphSkillSet.PERSON_FIELDS.add("id");
    GraphSkillSet.PERSON_FIELDS.add("displayName");
    GraphSkillSet.PERSON_FIELDS.add("thumbnailUrl");

    PERSON_SORTING = new HashMap<String, Object>();
    GraphSkillSet.PERSON_SORTING.put(WebsockConstants.SORT_FIELD, "displayName");
  }

  private final Node fNode;

  private final ImplUtil fImpl;

  /**
   * Creates a skill set in the graph based on an underlying node. Throws a NullPointerException if
   * the given node is null.
   *
   * @param node
   *          node representing a skill link
   */
  public GraphSkillSet(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates a skill set in the graph based on an underlying node. Throws a NullPointerException if
   * the given node is null.
   *
   * @param node
   *          node this person is based on.
   */
  public GraphSkillSet(Node node, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("Underlying node was null");
    }

    this.fNode = node;

    this.fImpl = impl;
  }

  @Override
  public Map<String, Object> toMap(Set<String> fields) {
    final Map<String, Object> dto = this.fImpl.newMap();

    // get name through link to general node
    final Node skill = this.fNode.getRelationships(ShindigRelTypes.IS_SKILL, Direction.OUTGOING)
            .iterator().next().getEndNode();

    dto.put(GraphSkillSet.NAME_FIELD, skill.getProperty(GraphSkillSet.NAME_FIELD));

    getLinkers(dto);

    return dto;
  }

  private void getLinkers(Map<String, Object> dto) {
    // people linking this skill
    final List<Map<String, ?>> people = this.fImpl.newList();
    final List<Node> nodeList = new ArrayList<Node>();

    final Iterable<Relationship> rels = this.fNode.getRelationships(ShindigRelTypes.LINKED_BY,
            Direction.OUTGOING);

    GraphPerson person = null;
    for (final Relationship r : rels) {
      nodeList.add(r.getEndNode());
    }

    // sort by name
    NodeSorter.sortNodes(nodeList, GraphSkillSet.PERSON_SORTING);

    for (final Node node : nodeList) {
      person = new GraphPerson(node, this.fImpl);
      people.add(person.toMap(GraphSkillSet.PERSON_FIELDS));
    }

    dto.put(GraphSkillSet.PEOPLE_FIELD, people);

    // check if owner is among them
    final Node owner = this.fNode.getRelationships(ShindigRelTypes.HAS_SKILL, Direction.INCOMING)
            .iterator().next().getStartNode();
    if (nodeList.contains(owner)) {
      dto.put(GraphSkillSet.CONFIRMED_FIELD, true);
    } else {
      dto.put(GraphSkillSet.CONFIRMED_FIELD, false);
    }
  }

  @Override
  public void setData(Map<String, ?> map) {
    // nothing can be set directly
  }

}
