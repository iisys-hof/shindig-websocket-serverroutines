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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.ConvHelper;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.SimpleGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.ShindigNativeProcedures;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphOrganizationSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphPersonSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class for converting an organization stored in the database, linked to a person to a
 * transferable object and vice versa.
 */
public class GraphOrganization implements IGraphObject {
  private static final String ID_FIELD = "id";
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

  private static final String ORG_UNIT_FIELD = "orgUnit";
  private static final String LOCATION_FIELD = "location";

  private static final ConvHelper HELPER = createHelper();

  private static ConvHelper createHelper() {
    final Set<String> relMapped = new HashSet<String>();

    relMapped.add(GraphOrganization.ADDRESS_FIELD);

    relMapped.add(GraphOrganization.SALARY_FIELD);
    relMapped.add(GraphOrganization.START_DATE_FIELD);
    relMapped.add(GraphOrganization.END_DATE_FIELD);
    relMapped.add(GraphOrganization.TITLE_FIELD);
    relMapped.add(GraphOrganization.PRIMARY_FIELD);

    relMapped.add(GraphOrganization.DEPARTMENT_FIELD);
    relMapped.add(GraphOrganization.DEPARTMENT_HEAD_FIELD);
    relMapped.add(GraphOrganization.MANAGER_ID_FIELD);
    relMapped.add(GraphOrganization.SECRETARY_ID_FIELD);

    return new ConvHelper(null, relMapped, null);
  }

  private final GraphPersonSPI fPeople;
  private final GraphOrganizationSPI fOrgSPI;
  private final ImplUtil fImpl;

  private final Node fPerson;

  private Relationship fAff;

  private final Node fOrg;
  private Node fOrgUnit;
  private Node fDep;

  /**
   * Creates an organization converter taking properties from and storing data in the single
   * organization node and affiliation relationship linked to the given person node. The person node
   * must not be null.
   *
   * @param person
   *          node of the person to convert the organization for
   */
  public GraphOrganization(Node person) {
    this(person, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates an organization converter taking properties from and storing data in the single
   * organization node and affiliation relationship linked to the given person node. The person node
   * and implementation utility must not be null.
   *
   * @param person
   *          node of the person to convert the organization for
   * @param impl
   *          implementation utility to use
   */
  public GraphOrganization(Node person, ImplUtil impl) {
    this.fPeople = ShindigNativeProcedures.getService(GraphPersonSPI.class);
    this.fOrgSPI = ShindigNativeProcedures.getService(GraphOrganizationSPI.class);
    this.fImpl = impl;

    this.fPerson = person;
    this.fAff = this.fPerson.getSingleRelationship(ShindigRelTypes.EMPLOYED, Direction.OUTGOING);

    // get department, orgUnit and global organization object
    this.fOrg = this.fOrgSPI.getOrganization();

    // case 1: linked to department
    if (this.fOrgSPI.createDeparments() && this.fAff != null) {
      this.fDep = this.fAff.getEndNode();

      // get next orgUnit in hierarchy
      final Relationship inUnitRel = this.fDep.getSingleRelationship(ShindigRelTypes.IN_UNIT,
              Direction.OUTGOING);

      if (inUnitRel != null) {
        this.fOrgUnit = inUnitRel.getEndNode();
      } else {
        // not found
        // TODO: throw exception?
        this.fOrgUnit = null;
      }
    }
    // case 2: linked to organizational unit
    else if (this.fAff != null) {
      this.fDep = null;
      this.fOrgUnit = this.fAff.getEndNode();
    }
  }

  @Override
  public Map<String, Object> toMap(final Set<String> fields) {
    // TODO: fields parameter is ignored, since the client isn't using it

    final Map<String, Object> dto = this.fImpl.newMap();

    // copy relationship-mapped properties before copying the other ones
    copyAddress(dto);

    if (this.fAff != null) {
      copyAffiliation(dto);
    }

    // get manager, department head and secretary fields through relationships
    getRelMapped(dto);

    // copy properties of other nodes
    // department
    if (this.fDep != null) {
      dto.put(GraphOrganization.DEPARTMENT_FIELD,
              this.fDep.getProperty(GraphOrganizationSPI.ORGU_NAME_PROP, null));
    }

    // organizational unit
    if (this.fOrgUnit != null) {
      dto.put(GraphOrganizationSPI.ORGU_NAME_PROP,
              this.fOrgUnit.getProperty(GraphOrganizationSPI.ORGU_NAME_PROP, null));
    }

    // main central organization
    for (final String property : this.fOrg.getPropertyKeys()) {
      dto.put(property, this.fOrg.getProperty(property));
    }

    return dto;
  }

  private void copyAddress(final Map<String, Object> dto) {
    if (this.fOrgUnit == null) {
      // break if there is nor organizational unit
      return;
    }

    final Relationship locRel = this.fOrgUnit.getSingleRelationship(ShindigRelTypes.LOCATED_AT,
            Direction.OUTGOING);

    if (locRel != null) {
      final Node locNode = locRel.getEndNode();
      final Map<String, Object> address = new SimpleGraphObject(locNode).toMap(null);
      dto.put(GraphOrganization.ADDRESS_FIELD, address);
    }
  }

  private void copyAffiliation(final Map<String, Object> dto) {
    for (final String key : this.fAff.getPropertyKeys()) {
      dto.put(key, this.fAff.getProperty(key));
    }
  }

  private void getRelMapped(final Map<String, Object> dto) {
    // manager
    final Relationship manRel = this.fPerson.getSingleRelationship(ShindigRelTypes.MANAGER,
            Direction.OUTGOING);
    if (manRel != null) {
      dto.put(GraphOrganization.MANAGER_ID_FIELD,
              manRel.getEndNode().getProperty(GraphOrganization.ID_FIELD, null));
    }

    // department head
    final Relationship depRel = this.fPerson.getSingleRelationship(ShindigRelTypes.HEAD,
            Direction.INCOMING);
    if (depRel != null) {
      dto.put(GraphOrganization.DEPARTMENT_HEAD_FIELD, true);
    } else {
      dto.put(GraphOrganization.DEPARTMENT_HEAD_FIELD, false);
    }

    // secretary
    final Relationship secRel = this.fPerson.getSingleRelationship(ShindigRelTypes.SECRETARY,
            Direction.OUTGOING);
    if (secRel != null) {
      dto.put(GraphOrganization.SECRETARY_ID_FIELD,
              secRel.getEndNode().getProperty(GraphOrganization.ID_FIELD, null));
    }
  }

  @Override
  public void setData(final Map<String, ?> organization) {
    final Map<String, Object> relVals = new HashMap<String, Object>();

    // not settable anymore, only configurable:
    // GraphOrganization.NAME_FIELD
    // GraphOrganization.FIELD_FIELD
    // GraphOrganization.SUB_FIELD_FIELD
    // GraphOrganization.TYPE_FIELD
    // GraphOrganization.WEBPAGE_FIELD

    // relationship properties
    relVals.put(GraphOrganization.DESCRIPTION_FIELD,
            organization.get(GraphOrganization.DESCRIPTION_FIELD));
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
    relVals.put(GraphOrganization.LOCATION_FIELD,
            organization.get(GraphOrganization.LOCATION_FIELD));
    // TODO: how to do this when other person has not been created yet?
    // TODO: two-pass syncing? lazy setting?
    // GraphOrganization.MANAGER_ID_FIELD
    // GraphOrganization.SECRETARY_ID_FIELD
    final Object manIdProp = organization.get(GraphOrganization.MANAGER_ID_FIELD);
    // remove existing relationships
    final Iterable<Relationship> manRels = this.fPerson.getRelationships(ShindigRelTypes.MANAGER,
            Direction.OUTGOING);
    for (final Relationship manRel : manRels) {
      manRel.delete();
    }
    if (manIdProp != null) {
      // link to new manager if possible
      Node manager = this.fPeople.getPersonNode(manIdProp.toString());

      // create basic manager node if non-existent
      if (manager == null) {
        final Map<String, Object> pMap = new HashMap<String, Object>();
        pMap.put(GraphOrganization.ID_FIELD, manIdProp.toString());
        this.fPeople.createPerson(pMap);
        manager = this.fPeople.getPersonNode(manIdProp.toString());
        // TODO: will cause problems when people are created in bulk without individually checking
      }

      // link
      this.fPerson.createRelationshipTo(manager, ShindigRelTypes.MANAGER);
    }

    final Object secIdProp = organization.get(GraphOrganization.SECRETARY_ID_FIELD);
    // remove existing relationship if it does not match
    final Relationship secRel = this.fPerson.getSingleRelationship(ShindigRelTypes.SECRETARY,
            Direction.OUTGOING);
    if (secRel != null
            && !secRel.getEndNode().getProperty(GraphOrganization.ID_FIELD).equals(secIdProp)) {
      secRel.delete();
    } else if (secIdProp != null) {
      // link to new secretary if possible
      Node secretary = this.fPeople.getPersonNode(secIdProp.toString());

      // create basic secretary node if non-existent
      if (secretary == null) {
        final Map<String, Object> pMap = new HashMap<String, Object>();
        pMap.put(GraphOrganization.ID_FIELD, secIdProp.toString());
        this.fPeople.createPerson(pMap);
        secretary = this.fPeople.getPersonNode(secIdProp.toString());
        // TODO: will cause problems when people are created in bulk without individually checking
      }

      this.fPerson.createRelationshipTo(secretary, ShindigRelTypes.SECRETARY);
    }

    // get and link appropriate organizational unit
    final Object orgUnitProp = organization.get(GraphOrganization.ORG_UNIT_FIELD);
    final Object depProp = organization.get(GraphOrganization.DEPARTMENT_FIELD);
    String orgUnitName = null;
    String depName = null;
    if (orgUnitProp != null) {
      orgUnitName = orgUnitProp.toString();
    }
    if (depProp != null) {
      depName = depProp.toString();
    }
    // ATTENTION: this may swap the affiliation Relationship, orgUnit and
    // department nodes
    setOrgUnits(orgUnitName, depName);

    // store properties if possible
    if (this.fAff != null) {
      Object value = null;
      for (final Entry<String, Object> relE : relVals.entrySet()) {
        value = relE.getValue();

        if (value != null) {
          this.fAff.setProperty(relE.getKey(), value);
        }
      }
    }

    // store address, if any, deleting old addresses
    // TODO: not every user update should trigger this
    // TODO: extra endpoint for these operations?
    if (this.fOrgUnit != null) {
      final Relationship locRel = this.fOrgUnit.getSingleRelationship(ShindigRelTypes.LOCATED_AT,
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
        final GraphDatabaseService db = this.fOrgUnit.getGraphDatabase();
        final Node locNode = db.createNode();
        this.fOrgUnit.createRelationshipTo(locNode, ShindigRelTypes.LOCATED_AT);

        // store attributes
        new SimpleGraphObject(locNode, this.fImpl).setData(add);
      }
    }
  }

  private void setOrgUnits(String orgUnitName, String department) {
    // break if there is nothing to do
    if (orgUnitName == null || this.fOrgSPI.createDeparments() && department == null) {
      return;
    }

    // assure link to correct organizational unit
    final Node newOrgUnit = this.fOrgSPI.getOrgUnit(null, orgUnitName);
    if (!newOrgUnit.equals(this.fOrgUnit) && !this.fOrgSPI.createDeparments()) {
      // remove connections to old unit if departments are not involved
      if (this.fAff != null) {
        this.fAff.delete();
      }

      // link to new orgUnit
      this.fOrgUnit = newOrgUnit;
      this.fAff = this.fPerson.createRelationshipTo(this.fOrgUnit, ShindigRelTypes.EMPLOYED);
    } else if (this.fOrgSPI.createDeparments()) {
      // handle links if departments are involved
      // TODO: get and link appropriate department
      final Node newDep = this.fOrgSPI.getOrgUnit(newOrgUnit, department);

      if (!newDep.equals(this.fDep)) {
        // remove connection to old department
        if (this.fAff != null) {
          this.fAff.delete();
        }
        this.fDep = newDep;
        this.fAff = this.fPerson.createRelationshipTo(this.fDep, ShindigRelTypes.EMPLOYED);
      }
    }
  }
}
