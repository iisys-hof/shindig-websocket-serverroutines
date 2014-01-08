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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig;

import java.util.Map;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.shindig.ShindigNativeQueries;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Tests the registration routine for the native implementation of the server-side Apache Shindig
 * routines. Only the proper registration is tested, Calls to individual routines are a matter for
 * other tests.
 */
public class ShindigNativeProceduresTest {
  private Map<String, IStoredProcedure> fProcedures;

  private GraphDatabaseService fService;

  /**
   * Creates a map of all native back-end routines.
   */
  @Before
  public void createProcedures() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fService = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (ShindigNativeProceduresTest.this.fService != null) {
          ShindigNativeProceduresTest.this.fService.shutdown();
        }
      }
    });

    final GraphConfig config = new GraphConfig(true);
    this.fProcedures = new ShindigNativeProcedures(config, this.fService, new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class)).getProcedures();
  }

  @After
  public void stopDatabase() {
    this.fService.shutdown();
  }

  @Test
  public void testPersonService() {
    IStoredProcedure proc = this.fProcedures.get(ShindigNativeQueries.GET_PEOPLE_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_PEOPLE_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_PERSON_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_PERSON_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.UPDATE_PERSON_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.UPDATE_PERSON_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_ALL_PEOPLE_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_ALL_PEOPLE_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.CREATE_PERSON_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.CREATE_PERSON_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.DELETE_PERSON_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.DELETE_PERSON_METHOD, proc.getName());
  }

  @Test
  public void testFriendService() {
    IStoredProcedure proc = this.fProcedures.get(ShindigNativeQueries.GET_FRIEND_REQUESTS_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_FRIEND_REQUESTS_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.REQUEST_FRIENDSHIP_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.REQUEST_FRIENDSHIP_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.DENY_FRIENDSHIP_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.DENY_FRIENDSHIP_METHOD, proc.getName());
  }

  @Test
  public void testGroupService() {
    final IStoredProcedure proc = this.fProcedures.get(ShindigNativeQueries.GET_GROUPS_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_GROUPS_METHOD, proc.getName());
  }

  @Test
  public void testGraphService() {
    IStoredProcedure proc = this.fProcedures.get(ShindigNativeQueries.GET_FOFS_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_FOFS_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_SHORTEST_PATH_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_SHORTEST_PATH_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.RECOMMEND_GROUP_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.RECOMMEND_GROUP_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.RECOMMEND_FRIEND_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.RECOMMEND_FRIEND_METHOD, proc.getName());
  }

  @Test
  public void testActivityStreamService() {
    IStoredProcedure proc = this.fProcedures.get(ShindigNativeQueries.GET_ACT_ENTRIES_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_ACT_ENTRIES_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_ACT_ENTRIES_BY_ID_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_ACT_ENTRIES_BY_ID_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_ACT_ENTRY_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_ACT_ENTRY_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.DELETE_ACT_ENTRIES_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.DELETE_ACT_ENTRIES_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.UPDATE_ACT_ENTRY_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.UPDATE_ACT_ENTRY_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.CREATE_ACT_ENTRY_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.CREATE_ACT_ENTRY_METHOD, proc.getName());
  }

  @Test
  public void testAppDataService() {
    IStoredProcedure proc = this.fProcedures.get(ShindigNativeQueries.GET_APP_DATA_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_APP_DATA_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.DELETE_APP_DATA_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.DELETE_APP_DATA_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.UPDATE_APP_DATA_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.UPDATE_APP_DATA_METHOD, proc.getName());
  }

  @Test
  public void testMessageService() {
    IStoredProcedure proc = this.fProcedures
            .get(ShindigNativeQueries.GET_MESSAGE_COLLECTIONS_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_MESSAGE_COLLECTIONS_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.CREATE_MESS_COLL_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.CREATE_MESS_COLL_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.MODIFY_MESS_COLL_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.MODIFY_MESS_COLL_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.DELETE_MESS_COLL_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.DELETE_MESS_COLL_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_MESSAGES_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_MESSAGES_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.CREATE_MESSAGE_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.CREATE_MESSAGE_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.DELETE_MESSAGES_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.DELETE_MESSAGES_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.MODIFY_MESSAGE_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.MODIFY_MESSAGE_METHOD, proc.getName());
  }

  @Test
  public void testAlbumService() {
    IStoredProcedure proc = this.fProcedures.get(ShindigNativeQueries.GET_ALBUM_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_ALBUM_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_ALBUMS_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_ALBUMS_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_GROUP_ALBUMS_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_GROUP_ALBUMS_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.DELETE_ALBUM_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.DELETE_ALBUM_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.CREATE_ALBUM_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.CREATE_ALBUM_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.UPDATE_ALBUM_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.UPDATE_ALBUM_METHOD, proc.getName());
  }

  @Test
  public void testMediaItemService() {
    IStoredProcedure proc = this.fProcedures.get(ShindigNativeQueries.GET_MEDIA_ITEM_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_MEDIA_ITEM_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_MEDIA_ITEMS_BY_ID_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_MEDIA_ITEMS_BY_ID_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_MEDIA_ITEMS_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_MEDIA_ITEMS_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.GET_GROUP_MEDIA_ITEMS_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.GET_GROUP_MEDIA_ITEMS_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.DELETE_MEDIA_ITEM_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.DELETE_MEDIA_ITEM_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.CREATE_MEDIA_ITEM_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.CREATE_MEDIA_ITEM_METHOD, proc.getName());

    proc = this.fProcedures.get(ShindigNativeQueries.UPDATE_MEDIA_ITEM_QUERY);
    Assert.assertNotNull(proc);
    Assert.assertEquals(ShindigNativeQueries.UPDATE_MEDIA_ITEM_METHOD, proc.getName());
  }
}
