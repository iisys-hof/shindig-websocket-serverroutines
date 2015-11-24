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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.Traversal;

import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;

/**
 * Service managing a central organization, organizational units and departments. An optional
 * cleanup thread removes unused organizational units and departments.
 */
public class GraphOrganizationSPI implements Runnable {
  public static final String ORGU_NAME_PROP = "orgUnit";

  public static final String CREATE_DEPS = "organizations.create_deparment_ous";

  public static final String ORG_CLEAN_INT = "organizations.cleanup_interval";

  private static final String ORG_NAME = "organization.name";
  private static final String ORG_FIELD = "organization.field";
  private static final String ORG_SUBFIELD = "organization.subfield";
  private static final String ORG_TYPE = "organization.type";
  private static final String ORG_WEBPAGE = "organization.webpage";

  private static final int MAX_PATH_DEPTH = 12;

  private final GraphDatabaseService fDatabase;

  private final Index<Node> fOrgUnitNodes;

  private final Node fOrganization;

  private final long fCleanupInterval;

  private final boolean fCreateDeps;

  private boolean fActive;

  /**
   * Creates an organization service using the given graph database and configuration. Reads and
   * sets information about the central organization node. Starts a cleanup thread if configured
   * accordingly. None of the parameters may be null.
   *
   * @param database
   *          database service to use
   * @param config
   *          configuration to use
   */
  public GraphOrganizationSPI(GraphDatabaseService database, Map<String, String> config) {
    if (database == null) {
      throw new NullPointerException("database service was null");
    }
    if (config == null) {
      throw new NullPointerException("configuration object");
    }

    this.fDatabase = database;

    this.fOrgUnitNodes = this.fDatabase.index().forNodes(ShindigConstants.ORG_UNIT_NODES);

    // create or update central organization object
    this.fOrganization = updateOrganization(config);

    this.fCreateDeps = Boolean.parseBoolean(config.get(GraphOrganizationSPI.CREATE_DEPS));

    String cleanIntStr = config.get(GraphOrganizationSPI.ORG_CLEAN_INT);
    if (cleanIntStr == null || cleanIntStr.isEmpty()) {
      cleanIntStr = "-1";
    }
    this.fCleanupInterval = Long.parseLong(cleanIntStr);

    // start cleanup thread if configured
    if (this.fCleanupInterval > 0) {
      new Thread(this).start();
    }
  }

  private Node updateOrganization(Map<String, String> config) {
    final String orgName = config.get(GraphOrganizationSPI.ORG_NAME);
    final String orgField = config.get(GraphOrganizationSPI.ORG_FIELD);
    final String orgSubField = config.get(GraphOrganizationSPI.ORG_SUBFIELD);
    final String orgType = config.get(GraphOrganizationSPI.ORG_TYPE);
    final String orgWebPage = config.get(GraphOrganizationSPI.ORG_WEBPAGE);

    final Index<Node> orgIndex = this.fDatabase.index().forNodes(ShindigConstants.ORG_NODE);
    Node orgNode = orgIndex.get(ShindigConstants.ORG_NODE, ShindigConstants.ORG_NODE).getSingle();

    final Transaction tx = this.fDatabase.beginTx();
    try {
      if (orgNode == null) {
        orgNode = this.fDatabase.createNode();
        orgIndex.add(orgNode, ShindigConstants.ORG_NODE, ShindigConstants.ORG_NODE);
      }

      // set values
      // TODO: optionally set fields to empty
      if (orgName != null) {
        orgNode.setProperty("name", orgName);
      }
      if (orgField != null) {
        orgNode.setProperty("field", orgField);
      }
      if (orgSubField != null) {
        // wrong in shindig - should be lower case according to spec
        orgNode.setProperty("subField", orgSubField);
      }
      if (orgType != null) {
        orgNode.setProperty("type", orgType);
      }
      if (orgWebPage != null) {
        orgNode.setProperty("webpage", orgWebPage);
      }
      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();
      e.printStackTrace();
      throw new RuntimeException("could not create or update organization node:\n" + e.getMessage());
    }

    return orgNode;
  }

  /**
   * @return central organization node
   */
  public Node getOrganization() {
    return this.fOrganization;
  }

  /**
   * Retrieves an organizational unit node with the given name, optionally from within the tree
   * structure if a parent node is supplied. If there is no node matching the parameters, one is
   * created and linked to its parent or the central organization node.
   *
   * @param parent
   *          optional parent node of in the organization hierarchy
   * @param name
   *          name of the organizational unit or department
   * @return existing or newly created organizational unit node
   */
  public Node getOrgUnit(Node parent, String name) {
    // TODO: synchronize to avoid duplicates?

    // find existing nodes
    Node orgUnit = null;
    if (parent == null) {
      orgUnit = this.fOrgUnitNodes.get(GraphOrganizationSPI.ORGU_NAME_PROP, name).getSingle();
    } else {
      // look for appropriate sub-orgUnit
      final Iterable<Relationship> subRels = parent.getRelationships(ShindigRelTypes.IN_UNIT,
              Direction.INCOMING);
      for (final Relationship rel : subRels) {
        final Node sub = rel.getStartNode();
        final Object subName = sub.getProperty(GraphOrganizationSPI.ORGU_NAME_PROP, null);
        if (name.equals(subName)) {
          orgUnit = sub;
          break;
        }
      }
    }

    // create nodes if they don't exist yet
    if (orgUnit == null) {
      // create department if it does not exist
      final Transaction tx = this.fDatabase.beginTx();
      try {
        orgUnit = this.fDatabase.createNode();
        orgUnit.setProperty(GraphOrganizationSPI.ORGU_NAME_PROP, name);

        if (parent == null) {
          // link to organization
          orgUnit.createRelationshipTo(this.fOrganization, ShindigRelTypes.PART_OF);
        } else {
          // link to parent
          orgUnit.createRelationshipTo(parent, ShindigRelTypes.IN_UNIT);
        }
        tx.success();
        tx.finish();
      } catch (final Exception e) {
        tx.failure();
        tx.finish();
        e.printStackTrace();
        throw new RuntimeException("could not create organizational unit:\n" + e.getMessage());
      }
    }

    return orgUnit;
  }

  /**
   * Determines the shortest hierarchical path between two person nodes via "is manager of"
   * relations and returns it or null, if there is none. The relations are still included in the
   * path, so the direction of the path can be determined.
   *
   * Not yet implemented.
   *
   * @param person1
   *          person at the beginning of the path
   * @param person2
   *          person at the end of the path
   * @return ordered path between the two people
   */
  public Path getHierarchyPath(Node person1, Node person2) {
    // shortest path over MANAGER relations
    Path path = null;
    // TODO: register expanders and path finder on global level

    // managed to manager
    PathExpander<Path> pathExp = Traversal.pathExpanderForTypes(ShindigRelTypes.MANAGER,
            Direction.OUTGOING);
    PathFinder<Path> sPathFinder = GraphAlgoFactory.shortestPath(pathExp,
            GraphOrganizationSPI.MAX_PATH_DEPTH);

    path = sPathFinder.findSinglePath(person1, person2);

    if (path != null) {
      return path;
    }

    // manager to managed
    // TODO: just swap people instead of creating new traversal?
    pathExp = Traversal.pathExpanderForTypes(ShindigRelTypes.MANAGER, Direction.INCOMING);
    sPathFinder = GraphAlgoFactory.shortestPath(pathExp, GraphOrganizationSPI.MAX_PATH_DEPTH);

    path = sPathFinder.findSinglePath(person1, person2);

    return path;
  }

  /**
   * @return whether to create orgUnits for departments
   */
  public boolean createDeparments() {
    return this.fCreateDeps;
  }

  /**
   * Removes unused organizational units and departments. Also determines department heads by
   * determining who has no manager within their department anymore.
   */
  public void cleanup() {
    final Transaction tx = this.fDatabase.beginTx();
    try {
      // clean up hierarchy
      final List<Node> departments = cleanupHierarchy();

      // determine departments heads
      if (this.fCreateDeps) {
        setDepartmentHeads(departments);
      }
      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();
      e.printStackTrace();
      throw new RuntimeException("could perform organization cleanup:\n" + e.getMessage());
    }
  }

  private List<Node> cleanupHierarchy() {
    final Iterable<Relationship> orgURels = this.fOrganization.getRelationships(
            ShindigRelTypes.PART_OF, Direction.INCOMING);
    Iterable<Relationship> depRels = null;
    final List<Node> departments = new LinkedList<Node>();
    Node orgUNode = null;
    Node depNode = null;

    for (final Relationship orgURel : orgURels) {
      orgUNode = orgURel.getStartNode();

      if (this.fCreateDeps) {
        depRels = orgUNode.getRelationships(ShindigRelTypes.IN_UNIT, Direction.INCOMING);

        // remove departments with no employees
        for (final Relationship depRel : depRels) {
          depNode = depRel.getStartNode();

          if (!depNode.hasRelationship(ShindigRelTypes.EMPLOYED, Direction.INCOMING)) {
            deleteAllRels(depNode);
            depNode.delete();
          } else {
            departments.add(depNode);
          }
        }

        // remove orgUnits with no departments
        if (!orgUNode.hasRelationship(ShindigRelTypes.IN_UNIT, Direction.INCOMING)) {
          deleteAllRels(orgUNode);
          orgUNode.delete();
        }
      } else {
        // remove orgUnits with no employees
        if (!orgUNode.hasRelationship(ShindigRelTypes.EMPLOYED, Direction.INCOMING)) {
          deleteAllRels(orgUNode);
          orgUNode.delete();
        }
      }
    }

    return departments;
  }

  private void setDepartmentHeads(List<Node> departments) {
    Iterable<Relationship> empRels = null;
    Node employee = null;
    Relationship manRel = null;
    Node manager = null;
    Relationship manDepRel = null;
    Node manDep = null;

    for (final Node department : departments) {
      // TODO: what if there are multiple matches?

      // remove existing department heads
      final Iterable<Relationship> headRels = department.getRelationships(ShindigRelTypes.HEAD,
              Direction.OUTGOING);
      for (final Relationship headRel : headRels) {
        headRel.delete();
      }

      // determine and set new department head
      empRels = department.getRelationships(ShindigRelTypes.EMPLOYED, Direction.INCOMING);
      for (final Relationship empRel : empRels) {
        employee = empRel.getStartNode();
        manRel = employee.getSingleRelationship(ShindigRelTypes.MANAGER, Direction.OUTGOING);

        if (manRel != null) {
          manager = manRel.getEndNode();

          // compare departments
          manDepRel = manager.getSingleRelationship(ShindigRelTypes.EMPLOYED, Direction.OUTGOING);
          manDep = null;

          if (manDepRel != null) {
            manDep = manDepRel.getEndNode();
          }

          if (!manDep.equals(department)) {
            // manager in different department - is head
            department.createRelationshipTo(employee, ShindigRelTypes.HEAD);
          }
        } else {
          // no manager - is head
          department.createRelationshipTo(employee, ShindigRelTypes.HEAD);
        }
      }
    }
  }

  private void deleteAllRels(Node node) {
    for (final Relationship rel : node.getRelationships()) {
      rel.delete();
    }
  }

  @Override
  public void run() {
    this.fActive = true;

    while (this.fActive) {
      try {
        // TODO: lock deletion if an organization link is in the process of being created
        cleanup();

        Thread.sleep(this.fCleanupInterval);
      } catch (final Exception e) {
        this.fActive = false;
        System.err.println("organization cleanup failed");
        e.printStackTrace();
      }
    }
  }
}
