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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.ConvHelper;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.SimpleGraphObject;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class for converting an organization stored in the database to a transferable object
 * and vice versa.
 */
public class GraphOrganization implements IGraphObject {
  // TODO: verify fields
  private static final String ADDRESS_FIELD = "address";
  private static final String DESCRIPTION_FIELD = "description";
  private static final String END_DATE_FIELD = "endDate";
  private static final String FIELD_FIELD = "field";
  private static final String NAME_FIELD = "name";
  private static final String SALARY_FIELD = "salary";
  private static final String START_DATE_FIELD = "startDate";
  private static final String SUB_FIELD_FIELD = "subField";
  private static final String TITLE_FIELD = "title";
  private static final String WEBPAGE_FIELD = "webpage";
  private static final String TYPE_FIELD = "type";
  private static final String PRIMARY_FIELD = "primary";

  private static final String MANAGER_ID_FIELD = "managerId";
  private static final String SECRETARY_ID_FIELD = "secretaryId";
  private static final String DEPARTMENT_FIELD = "department";
  private static final String DEPARTMENT_HEAD_FIELD = "departmentHead";

  private static final ConvHelper HELPER = createHelper();

  private static ConvHelper createHelper() {
    final Set<String> relMapped = new HashSet<String>();

    relMapped.add(GraphOrganization.ADDRESS_FIELD);

    relMapped.add(GraphOrganization.SALARY_FIELD);
    relMapped.add(GraphOrganization.START_DATE_FIELD);
    relMapped.add(GraphOrganization.END_DATE_FIELD);
    relMapped.add(GraphOrganization.TITLE_FIELD);
    relMapped.add(GraphOrganization.PRIMARY_FIELD);

    /*
     * can't be simply mapped over relationships between people as a person might have multiple
     * organizations with different managers so IDs are stored in the association relation for now
     */
    relMapped.add(GraphOrganization.DEPARTMENT_FIELD);
    relMapped.add(GraphOrganization.DEPARTMENT_HEAD_FIELD);
    relMapped.add(GraphOrganization.MANAGER_ID_FIELD);
    relMapped.add(GraphOrganization.SECRETARY_ID_FIELD);

    return new ConvHelper(null, relMapped, null);
  }

  private final Node fNode;
  private final Relationship fAff;

  private final ImplUtil fImpl;

  /**
   * Creates an organization converter taking properties from and storing data in the given
   * organization node and affiliation relationship. None of the parameters may be null.
   *
   * @param org
   *          node representing the organization
   * @param aff
   *          relationship representing the affiliation
   */
  public GraphOrganization(Node org, Relationship aff) {
    this(org, aff, new ImplUtil(LinkedList.class, HashMap.class));
  }

  public GraphOrganization(Node org, Relationship aff, ImplUtil impl) {
    this.fNode = org;
    this.fAff = aff;

    this.fImpl = impl;
  }

  @Override
  public Map<String, Object> toMap(final Set<String> fields) {
    final Map<String, Object> dto = this.fImpl.newMap();

    // copy relationship-mapped properties before copying the other ones
    copyAddress(dto);
    if (this.fAff != null) {
      copyAffiliation(dto);
    }

    for (final String property : this.fNode.getPropertyKeys()) {
      dto.put(property, this.fNode.getProperty(property));
    }

    return dto;
  }

  private void copyAddress(final Map<String, Object> dto) {
    final Relationship locRel = this.fNode.getSingleRelationship(Neo4jRelTypes.LOCATED_AT,
            Direction.OUTGOING);

    if (locRel != null) {
      final Node locNode = locRel.getEndNode();
      final Map<String, Object> address = new SimpleGraphObject(locNode).toMap(null);
      dto.put(GraphOrganization.ADDRESS_FIELD, address);
    }
  }

  private void copyAffiliation(final Map<String, Object> dto) {
    final Set<String> relMapped = GraphOrganization.HELPER.getRelationshipMapped();

    for (final String prop : relMapped) {
      if (this.fAff.hasProperty(prop)) {
        dto.put(prop, this.fAff.getProperty(prop));
      }
    }
  }

  @Override
  public void setData(final Map<String, ?> organization) {
    final Map<String, Object> nodeVals = new HashMap<String, Object>();
    final Map<String, Object> relVals = new HashMap<String, Object>();

    // node properties
    nodeVals.put(GraphOrganization.DESCRIPTION_FIELD,
            organization.get(GraphOrganization.DESCRIPTION_FIELD));
    nodeVals.put(GraphOrganization.FIELD_FIELD, organization.get(GraphOrganization.FIELD_FIELD));
    nodeVals.put(GraphOrganization.NAME_FIELD, organization.get(GraphOrganization.NAME_FIELD));
    nodeVals.put(GraphOrganization.SUB_FIELD_FIELD,
            organization.get(GraphOrganization.SUB_FIELD_FIELD));
    nodeVals.put(GraphOrganization.TYPE_FIELD, organization.get(GraphOrganization.TYPE_FIELD));
    nodeVals.put(GraphOrganization.WEBPAGE_FIELD, organization.get(GraphOrganization.WEBPAGE_FIELD));

    // relationship properties
    final Object endDate = organization.get(GraphOrganization.END_DATE_FIELD);
    if (endDate != null) {
      relVals.put(GraphOrganization.END_DATE_FIELD, endDate);
    }
    relVals.put(GraphOrganization.PRIMARY_FIELD, organization.get(GraphOrganization.PRIMARY_FIELD));
    relVals.put(GraphOrganization.SALARY_FIELD, organization.get(GraphOrganization.SALARY_FIELD));
    final Object startDate = organization.get(GraphOrganization.START_DATE_FIELD);
    if (startDate != null) {
      relVals.put(GraphOrganization.START_DATE_FIELD, startDate);
    }
    relVals.put(GraphOrganization.TITLE_FIELD, organization.get(GraphOrganization.TITLE_FIELD));

    // extended model properties
    relVals.put(GraphOrganization.DEPARTMENT_FIELD,
            organization.get(GraphOrganization.DEPARTMENT_FIELD));
    relVals.put(GraphOrganization.DEPARTMENT_HEAD_FIELD,
            organization.get(GraphOrganization.DEPARTMENT_HEAD_FIELD));
    relVals.put(GraphOrganization.MANAGER_ID_FIELD,
            organization.get(GraphOrganization.MANAGER_ID_FIELD));
    relVals.put(GraphOrganization.SECRETARY_ID_FIELD,
            organization.get(GraphOrganization.SECRETARY_ID_FIELD));

    // store properties
    Object value = null;
    for (final Entry<String, Object> nodeE : nodeVals.entrySet()) {
      value = nodeE.getValue();

      if (value != null) {
        this.fNode.setProperty(nodeE.getKey(), value);
      }
    }

    for (final Entry<String, Object> relE : relVals.entrySet()) {
      value = relE.getValue();

      if (value != null) {
        this.fAff.setProperty(relE.getKey(), value);
      }
    }

    // store address, if any, deleting old addresses
    final Relationship locRel = this.fNode.getSingleRelationship(Neo4jRelTypes.LOCATED_AT,
            Direction.OUTGOING);
    if (locRel != null) {
      final Node locNode = locRel.getEndNode();
      locRel.delete();

      // delete unused addresses
      if (!locNode.hasRelationship()) {
        locNode.delete();
      }
    }

    @SuppressWarnings("unchecked")
    final Map<String, Object> add = (Map<String, Object>) organization
            .get(GraphOrganization.ADDRESS_FIELD);
    if (add != null) {
      final GraphDatabaseService db = this.fNode.getGraphDatabase();
      final Node locNode = db.createNode();
      this.fNode.createRelationshipTo(locNode, Neo4jRelTypes.LOCATED_AT);

      // store attributes
      new SimpleGraphObject(locNode, this.fImpl).setData(add);
    }
  }

}
