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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

/**
 * Service that retrieves and creates application nodes.
 */
public class ApplicationService {
  private final GraphDatabaseService fDatabase;
  private final Index<Node> fApplications;

  /**
   * Creates an application service retrieving applications from the given database service. Throws
   * a NullPointerException if the given service is null.
   *
   * @param database
   *          database service to use
   */
  public ApplicationService(GraphDatabaseService database) {
    if (database == null) {
      throw new NullPointerException("database service was null");
    }

    this.fDatabase = database;
    this.fApplications = this.fDatabase.index().forNodes(ShindigConstants.APP_NODES);
  }

  /**
   * Retrieves an application node for the given application ID. If the application does not yet
   * have a node, it is created.
   *
   * @param appId
   *          application ID
   * @return application Node
   */
  public Node getApplication(final String appId) {
    Node application = this.fApplications.get(OSFields.ID_FIELD, appId).getSingle();

    if (application == null) {
      application = this.fDatabase.createNode();
      application.setProperty(OSFields.ID_FIELD, appId);
      application.setProperty(OSFields.NAME_FIELD, appId);
      this.fApplications.add(application, OSFields.ID_FIELD, appId);
    }

    return application;
  }
}
