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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.SimpleGraphObject;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the application data service retrieving application data from the Neo4j graph
 * database.
 */
public class GraphAppDataSPI {
  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;
  private final ApplicationService fApplications;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  /**
   * Creates a graph application data service using data from the given graph database service and
   * provider of person data. Throws a NullPointerException if a parameter is null.
   *
   * @param database
   *          database service to use
   * @param personSPI
   *          person data provider to use
   * @param applications
   *          application service to use
   * @param impl
   *          implementation utility to use
   */
  public GraphAppDataSPI(GraphDatabaseService database, GraphPersonSPI personSPI,
          ApplicationService applications, ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("database service was null");
    }
    if (personSPI == null) {
      throw new NullPointerException("person service was null");
    }
    if (applications == null) {
      throw new NullPointerException("application service was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    this.fDatabase = database;
    this.fPersonSPI = personSPI;
    this.fApplications = applications;

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  private Map<String, String> getAppData(Node person, String appId, Set<String> fields) {
    final Map<String, String> data = this.fImpl.newMap();

    Node appDataNode = null;
    String relId = null;
    final Iterable<Relationship> dataRels = person.getRelationships(Neo4jRelTypes.HAS_DATA);

    for (final Relationship rel : dataRels) {
      relId = (String) rel.getProperty(OSFields.APP_ID);

      if (appId.equals(relId)) {
        appDataNode = rel.getEndNode();
        final Map<String, Object> nodeData = new SimpleGraphObject(appDataNode).toMap(fields);

        for (final Entry<String, Object> dataE : nodeData.entrySet()) {
          data.put(dataE.getKey(), dataE.getValue().toString());
        }

        break;
      }
    }

    return data;
  }

  /**
   * Returns application data for a list of people, specific groups and applications, returning the
   * requested fields.
   *
   * @param userIds
   *          IDs of users to get application data for
   * @param groupId
   *          group to get application data for
   * @param appId
   *          application ID to get application data for
   * @param fields
   *          fields to retrieve
   * @return single result with data mapped by person
   */
  public SingleResult getPersonData(List<String> userIds, String groupId, String appId,
          List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }

    final Map<String, Map<String, String>> entry = this.fImpl.newMap();

    Map<String, String> data = null;

    Node person = null;
    for (final String id : userIds) {
      person = this.fPersonSPI.getPersonNode(id);
      data = getAppData(person, appId, fieldSet);
      entry.put(id, data);
    }

    return new SingleResult(entry);
  }

  private void checkDeletion(Relationship rel, final Set<String> fields) {
    final Transaction trans = this.fDatabase.beginTx();

    try {
      final Node dataNode = rel.getEndNode();

      if (fields == null || fields.isEmpty()) {
        // full node deletion
        final Iterable<Relationship> rels = dataNode.getRelationships();
        for (final Relationship r : rels) {
          r.delete();
        }

        dataNode.delete();
      } else {
        // delete only the fields specified
        for (final String field : fields) {
          dataNode.removeProperty(field);
        }
      }

      trans.success();
      trans.finish();
    } catch (final Exception e) {
      trans.failure();
      trans.finish();
      e.printStackTrace();
    }
  }

  /**
   * Deletes application data for a user or group, for the given application.
   *
   * @param userId
   *          user to delete application data for
   * @param groupId
   *          group to delete application data for
   * @param appId
   *          application to delete application data for
   * @param fields
   *          fields to delete
   */
  public void deletePersonData(String userId, String groupId, String appId, List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    String relId = null;
    final Node person = this.fPersonSPI.getPersonNode(userId);
    final Iterable<Relationship> dataRels = person.getRelationships(Neo4jRelTypes.HAS_DATA);

    for (final Relationship rel : dataRels) {
      relId = (String) rel.getProperty(OSFields.APP_ID);

      if (appId.equals(relId)) {
        checkDeletion(rel, fieldSet);
        break;
      }
    }
  }

  private Node getOrCreateData(Node person, String appId) {
    Node data = null;
    String relId = null;

    // check if there already is a node to store data
    final Iterable<Relationship> dataRels = person.getRelationships(Neo4jRelTypes.HAS_DATA);

    for (final Relationship rel : dataRels) {
      relId = (String) rel.getProperty(OSFields.APP_ID);

      if (appId.equals(relId)) {
        data = rel.getEndNode();
        break;
      }
    }

    // create a new blank data node and link it to the person
    if (data == null) {
      data = this.fDatabase.createNode();
      final Relationship rel = person.createRelationshipTo(data, Neo4jRelTypes.HAS_DATA);
      rel.setProperty(OSFields.APP_ID, appId);

      final Node application = this.fApplications.getApplication(appId);
      data.createRelationshipTo(application, Neo4jRelTypes.USED_BY);
    }

    return data;
  }

  /**
   * Updates application data for a user or group, for an application.
   *
   * @param userId
   *          user to update application data for
   * @param groupId
   *          group to update application data for
   * @param appId
   *          application to update application data for
   * @param values
   *          application data to store
   */
  public void updatePersonData(String userId, String groupId, String appId,
          Map<String, Object> values) {
    final Transaction trans = this.fDatabase.beginTx();

    try {
      final Node person = this.fPersonSPI.getPersonNode(userId);
      final Node data = getOrCreateData(person, appId);

      final SimpleGraphObject gData = new SimpleGraphObject(data);
      gData.setData(values);

      trans.success();
      trans.finish();
    } catch (final Exception e) {
      trans.failure();
      trans.finish();
      e.printStackTrace();
      this.fLogger.log(Level.SEVERE, e.getLocalizedMessage(), e);
    }
  }
}
