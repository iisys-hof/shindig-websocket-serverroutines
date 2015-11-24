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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * Test for the graph back-end's internal application service.
 */
public class ApplicationServiceTest {
  private static final String APP1_ID = "app1", APP2_ID = "app2";

  private GraphDatabaseService fDb;
  private ApplicationService fAppSPI;

  private Node fApp1;

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
        if (ApplicationServiceTest.this.fDb != null) {
          ApplicationServiceTest.this.fDb.shutdown();
        }
      }
    });

    this.fAppSPI = new ApplicationService(this.fDb);

    createTestData();
  }

  @After
  public void stopDatabase() {
    this.fDb.shutdown();
  }

  private void createTestData() {
    final Index<Node> appNodes = this.fDb.index().forNodes(ShindigConstants.APP_NODES);

    final Transaction trans = this.fDb.beginTx();

    this.fApp1 = this.fDb.createNode();
    this.fApp1.setProperty("id", ApplicationServiceTest.APP1_ID);
    this.fApp1.setProperty("name", ApplicationServiceTest.APP1_ID);
    appNodes.add(this.fApp1, "id", ApplicationServiceTest.APP1_ID);

    trans.success();
    trans.finish();
  }

  /**
   * Test for the retrieval of existing application nodes.
   */
  @Test
  public void existingTest() {
    final Node node = this.fAppSPI.getApplication(ApplicationServiceTest.APP1_ID);
    Assert.assertEquals(this.fApp1, node);
  }

  /**
   * Test for the automatic creation of new application nodes.
   */
  @Test
  public void creationTest() {
    // the service is not supposed/required to handle its own transactions
    final Transaction trans = this.fDb.beginTx();

    // create a new one
    final Node app2 = this.fAppSPI.getApplication(ApplicationServiceTest.APP2_ID);

    // check its retrieval
    Assert.assertEquals(app2, this.fAppSPI.getApplication(ApplicationServiceTest.APP2_ID));

    trans.success();
    trans.finish();
  }
}
