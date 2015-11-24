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

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * Test for the group converter class.
 */
public class GraphGroupTest {
  private static final String ID_FIELD = "id";
  private static final String TITLE_FIELD = "title";
  private static final String DESCRIPTION_FIELD = "description";

  private static final String ID = "group ID";
  private static final String TITLE = "group title";
  private static final String DESCRIPTION = "group description";

  private GraphDatabaseService fDb;
  private Node fGroupNode;

  /**
   * Sets up an impermanent database with some test data for testing purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (GraphGroupTest.this.fDb != null) {
          GraphGroupTest.this.fDb.shutdown();
        }
      }
    });

    createTestData();
  }

  private void createTestData() {
    final Transaction trans = this.fDb.beginTx();

    this.fGroupNode = this.fDb.createNode();
    this.fGroupNode.setProperty(GraphGroupTest.ID_FIELD, GraphGroupTest.ID);
    this.fGroupNode.setProperty(GraphGroupTest.TITLE_FIELD, GraphGroupTest.TITLE);
    this.fGroupNode.setProperty(GraphGroupTest.DESCRIPTION_FIELD, GraphGroupTest.DESCRIPTION);

    trans.success();
    trans.finish();
  }

  /**
   * Test for conversion of existing data.
   */
  @Test
  public void conversionTest() {
    final Map<String, Object> group = new GraphGroup(this.fGroupNode).toMap(null);

    Assert.assertEquals(GraphGroupTest.ID, group.get(GraphGroupTest.ID_FIELD));
    Assert.assertEquals(GraphGroupTest.TITLE, group.get(GraphGroupTest.TITLE_FIELD));
    Assert.assertEquals(GraphGroupTest.DESCRIPTION, group.get(GraphGroupTest.DESCRIPTION_FIELD));
  }
}
