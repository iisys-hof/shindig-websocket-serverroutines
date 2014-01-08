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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;

import com.google.inject.Inject;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.calls.NativeProcedure;
import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IDManager;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.ActivityObjectService;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.ApplicationService;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphActivityStreamSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphAlbumSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphAppDataSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphFriendSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphGroupSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphMediaItemSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphMessageSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphPersonSPI;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphSPI;
import de.hofuniversity.iisys.neo4j.websock.procedures.IProcedureProvider;
import de.hofuniversity.iisys.neo4j.websock.shindig.ShindigNativeQueries;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Class creating stored procedures from the native Shindig back-end implementation using
 * reflection.
 */
public class ShindigNativeProcedures implements IProcedureProvider {
  private final GraphConfig fConfig;
  private final GraphDatabaseService fDb;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  /**
   * Creates an instance of the Shindig native stored procedure initializer based on the given
   * configuration and graph database service, using the given list and map implementations. None of
   * the parameters may be null.
   *
   * @param config
   *          configuration object to use
   * @param database
   *          database service to use
   * @param impl
   *          implementation utility
   */
  @Inject
  public ShindigNativeProcedures(GraphConfig config, GraphDatabaseService database, ImplUtil impl) {
    this.fConfig = config;
    this.fDb = database;

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  @Override
  public Map<String, IStoredProcedure> getProcedures() {
    final Map<String, IStoredProcedure> procedures = new HashMap<String, IStoredProcedure>();

    final IDManager idMan = new IDManager(this.fDb);

    // create native back-end routines
    final GraphPersonSPI personSPI = new GraphPersonSPI(this.fDb, this.fConfig, this.fImpl);
    final GraphFriendSPI friendSPI = new GraphFriendSPI(this.fDb, personSPI, this.fImpl);
    final GraphGroupSPI groupSPI = new GraphGroupSPI(personSPI, this.fImpl);
    final GraphSPI graphSPI = new GraphSPI(personSPI, this.fImpl);

    final ActivityObjectService actObjSPI = new ActivityObjectService(this.fDb, this.fConfig,
            this.fImpl);
    final ApplicationService appSPI = new ApplicationService(this.fDb);
    final GraphActivityStreamSPI activitySPI = new GraphActivityStreamSPI(this.fDb, personSPI,
            actObjSPI, appSPI, idMan, this.fImpl);

    final GraphAppDataSPI appDataSPI = new GraphAppDataSPI(this.fDb, personSPI, appSPI, this.fImpl);

    final GraphMessageSPI messageSPI = new GraphMessageSPI(this.fDb, personSPI, idMan, this.fImpl);

    final GraphAlbumSPI albumSPI = new GraphAlbumSPI(this.fDb, personSPI, idMan, this.fImpl);
    final GraphMediaItemSPI mediaItemSPI = new GraphMediaItemSPI(this.fDb, personSPI, idMan,
            this.fImpl);

    // add individual service methods
    try {
      addPersonService(personSPI, procedures);
      addFriendService(friendSPI, procedures);
      addGroupService(groupSPI, procedures);
      addGraphService(graphSPI, procedures);
      addActivityService(activitySPI, procedures);
      addAppDataService(appDataSPI, procedures);
      addMessageService(messageSPI, procedures);
      addAlbumService(albumSPI, procedures);
      addMediaItemService(mediaItemSPI, procedures);
    } catch (final Exception e) {
      e.printStackTrace();
      this.fLogger.log(Level.SEVERE, "could not create native Shindig procedures", e);
    }

    return procedures;
  }

  private void addPersonService(final GraphPersonSPI personSPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getPeople
    final Method getPeople = GraphPersonSPI.class.getMethod(ShindigNativeQueries.GET_PEOPLE_METHOD,
            List.class, String.class, Map.class, List.class);

    List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID_LIST);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    IStoredProcedure proc = new NativeProcedure(ShindigNativeQueries.GET_PEOPLE_METHOD, personSPI,
            getPeople, paramNames);
    procedures.put(ShindigNativeQueries.GET_PEOPLE_QUERY, proc);

    // getPerson
    final Method getPerson = GraphPersonSPI.class.getMethod(ShindigNativeQueries.GET_PERSON_METHOD,
            String.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_PERSON_METHOD, personSPI, getPerson,
            paramNames);
    procedures.put(ShindigNativeQueries.GET_PERSON_QUERY, proc);

    // updatePerson
    final Method updatePerson = GraphPersonSPI.class.getMethod(
            ShindigNativeQueries.UPDATE_PERSON_METHOD, String.class, Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.PERSON_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.UPDATE_PERSON_METHOD, personSPI, updatePerson,
            paramNames);
    procedures.put(ShindigNativeQueries.UPDATE_PERSON_QUERY, proc);

    // getAllPeople
    final Method getAllPeople = GraphPersonSPI.class.getMethod(
            ShindigNativeQueries.GET_ALL_PEOPLE_METHOD, Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_ALL_PEOPLE_METHOD, personSPI, getAllPeople,
            paramNames);
    procedures.put(ShindigNativeQueries.GET_ALL_PEOPLE_QUERY, proc);

    // createPerson
    final Method createPerson = GraphPersonSPI.class.getMethod(
            ShindigNativeQueries.CREATE_PERSON_METHOD, Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.PERSON_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.CREATE_PERSON_METHOD, personSPI, createPerson,
            paramNames);
    procedures.put(ShindigNativeQueries.CREATE_PERSON_QUERY, proc);

    // deletePerson
    final Method deletePerson = GraphPersonSPI.class.getMethod(
            ShindigNativeQueries.DELETE_PERSON_METHOD, String.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);

    proc = new NativeProcedure(ShindigNativeQueries.DELETE_PERSON_METHOD, personSPI, deletePerson,
            paramNames);
    procedures.put(ShindigNativeQueries.DELETE_PERSON_QUERY, proc);
  }

  private void addFriendService(final GraphFriendSPI friendSPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getRequests
    final Method getRequests = GraphFriendSPI.class.getMethod(
            ShindigNativeQueries.GET_FRIEND_REQUESTS_METHOD, String.class, Map.class, List.class);

    List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    IStoredProcedure proc = new NativeProcedure(ShindigNativeQueries.GET_FRIEND_REQUESTS_METHOD,
            friendSPI, getRequests, paramNames);
    procedures.put(ShindigNativeQueries.GET_FRIEND_REQUESTS_QUERY, proc);

    // requestFriendship
    final Method requestFriendship = GraphFriendSPI.class.getMethod(
            ShindigNativeQueries.REQUEST_FRIENDSHIP_METHOD, String.class, String.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.TARGET_USER_ID);

    proc = new NativeProcedure(ShindigNativeQueries.REQUEST_FRIENDSHIP_METHOD, friendSPI,
            requestFriendship, paramNames);
    procedures.put(ShindigNativeQueries.REQUEST_FRIENDSHIP_QUERY, proc);

    // denyFriendship
    final Method denyFriendship = GraphFriendSPI.class.getMethod(
            ShindigNativeQueries.DENY_FRIENDSHIP_METHOD, String.class, String.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.TARGET_USER_ID);

    proc = new NativeProcedure(ShindigNativeQueries.DENY_FRIENDSHIP_METHOD, friendSPI,
            denyFriendship, paramNames);
    procedures.put(ShindigNativeQueries.DENY_FRIENDSHIP_QUERY, proc);
  }

  private void addGroupService(final GraphGroupSPI groupSPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getGroups
    final Method getGroups = GraphGroupSPI.class.getMethod(ShindigNativeQueries.GET_GROUPS_METHOD,
            String.class, Map.class, List.class);

    final List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    final IStoredProcedure proc = new NativeProcedure(ShindigNativeQueries.GET_GROUPS_METHOD,
            groupSPI, getGroups, paramNames);
    procedures.put(ShindigNativeQueries.GET_GROUPS_QUERY, proc);
  }

  private void addGraphService(final GraphSPI graphSPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getFriendsOfFriends
    final Method getFriendsOfFriends = GraphSPI.class.getMethod(
            ShindigNativeQueries.GET_FOFS_METHOD, List.class, Integer.TYPE, Boolean.TYPE,
            Map.class, List.class);

    List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID_LIST);
    paramNames.add(ShindigNativeQueries.FOF_DEPTH);
    paramNames.add(ShindigNativeQueries.FOF_UNKNOWN);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    IStoredProcedure proc = new NativeProcedure(ShindigNativeQueries.GET_FOFS_METHOD, graphSPI,
            getFriendsOfFriends, paramNames);
    procedures.put(ShindigNativeQueries.GET_FOFS_QUERY, proc);

    // getShortestPath
    final Method getShortestPath = GraphSPI.class.getMethod(
            ShindigNativeQueries.GET_SHORTEST_PATH_METHOD, String.class, String.class, Map.class,
            List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.TARGET_USER_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_SHORTEST_PATH_METHOD, graphSPI,
            getShortestPath, paramNames);
    procedures.put(ShindigNativeQueries.GET_SHORTEST_PATH_QUERY, proc);

    // getGroupRecommendation
    final Method getGroupRecommendation = GraphSPI.class.getMethod(
            ShindigNativeQueries.RECOMMEND_GROUP_METHOD, String.class, Integer.TYPE, Map.class,
            List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.MIN_FRIENDS_IN_GROUP);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.RECOMMEND_GROUP_METHOD, graphSPI,
            getGroupRecommendation, paramNames);
    procedures.put(ShindigNativeQueries.RECOMMEND_GROUP_QUERY, proc);

    // getFriendRecommendation
    final Method getFriendRecommendation = GraphSPI.class.getMethod(
            ShindigNativeQueries.RECOMMEND_FRIEND_METHOD, String.class, Integer.TYPE, Map.class,
            List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.MIN_COMMON_FRIENDS);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.RECOMMEND_FRIEND_METHOD, graphSPI,
            getFriendRecommendation, paramNames);
    procedures.put(ShindigNativeQueries.RECOMMEND_FRIEND_QUERY, proc);
  }

  private void addActivityService(final GraphActivityStreamSPI activitySPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getActivityEntries
    final Method getActivityEntries = GraphActivityStreamSPI.class.getMethod(
            ShindigNativeQueries.GET_ACT_ENTRIES_METHOD, List.class, String.class, String.class,
            Map.class, List.class);

    List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID_LIST);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    IStoredProcedure proc = new NativeProcedure(ShindigNativeQueries.GET_ACT_ENTRIES_METHOD,
            activitySPI, getActivityEntries, paramNames);
    procedures.put(ShindigNativeQueries.GET_ACT_ENTRIES_QUERY, proc);

    // getActivityEntriesById
    final Method getActivityEntriesById = GraphActivityStreamSPI.class.getMethod(
            ShindigNativeQueries.GET_ACT_ENTRIES_BY_ID_METHOD, String.class, String.class,
            String.class, Map.class, List.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);
    paramNames.add(ShindigNativeQueries.ACTIVITY_IDS);

    proc = new NativeProcedure(ShindigNativeQueries.GET_ACT_ENTRIES_BY_ID_METHOD, activitySPI,
            getActivityEntriesById, paramNames);
    procedures.put(ShindigNativeQueries.GET_ACT_ENTRIES_BY_ID_QUERY, proc);

    // getActivityEntry
    final Method getActivityEntry = GraphActivityStreamSPI.class.getMethod(
            ShindigNativeQueries.GET_ACT_ENTRY_METHOD, String.class, String.class, String.class,
            List.class, String.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);
    paramNames.add(ShindigNativeQueries.ACTIVITY_ID);

    proc = new NativeProcedure(ShindigNativeQueries.GET_ACT_ENTRY_METHOD, activitySPI,
            getActivityEntry, paramNames);
    procedures.put(ShindigNativeQueries.GET_ACT_ENTRY_QUERY, proc);

    // deleteActivityEntries
    final Method deleteActivityEntries = GraphActivityStreamSPI.class.getMethod(
            ShindigNativeQueries.DELETE_ACT_ENTRIES_METHOD, String.class, String.class,
            String.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ACTIVITY_IDS);

    proc = new NativeProcedure(ShindigNativeQueries.DELETE_ACT_ENTRIES_METHOD, activitySPI,
            deleteActivityEntries, paramNames);
    procedures.put(ShindigNativeQueries.DELETE_ACT_ENTRIES_METHOD, proc);

    // updateActivityEntry
    final Method updateActivityEntry = GraphActivityStreamSPI.class.getMethod(
            ShindigNativeQueries.UPDATE_ACT_ENTRY_METHOD, String.class, String.class, String.class,
            String.class, Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ACTIVITY_ID);
    paramNames.add(ShindigNativeQueries.ACTIVITY_ENTRY_OBJECT);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.UPDATE_ACT_ENTRY_METHOD, activitySPI,
            updateActivityEntry, paramNames);
    procedures.put(ShindigNativeQueries.UPDATE_ACT_ENTRY_METHOD, proc);

    // createActivityEntry
    final Method createActivityEntry = GraphActivityStreamSPI.class.getMethod(
            ShindigNativeQueries.CREATE_ACT_ENTRY_METHOD, String.class, String.class, String.class,
            Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ACTIVITY_ENTRY_OBJECT);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.CREATE_ACT_ENTRY_METHOD, activitySPI,
            createActivityEntry, paramNames);
    procedures.put(ShindigNativeQueries.CREATE_ACT_ENTRY_QUERY, proc);
  }

  private void addAppDataService(final GraphAppDataSPI appDataSPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getPersonData
    final Method getAppData = GraphAppDataSPI.class.getMethod(
            ShindigNativeQueries.GET_APP_DATA_METHOD, List.class, String.class, String.class,
            List.class);

    List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID_LIST);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    IStoredProcedure proc = new NativeProcedure(ShindigNativeQueries.GET_APP_DATA_METHOD,
            appDataSPI, getAppData, paramNames);
    procedures.put(ShindigNativeQueries.GET_APP_DATA_QUERY, proc);

    // deletePersonData
    final Method deleteAppData = GraphAppDataSPI.class.getMethod(
            ShindigNativeQueries.DELETE_APP_DATA_METHOD, String.class, String.class, String.class,
            List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.DELETE_APP_DATA_METHOD, appDataSPI,
            deleteAppData, paramNames);
    procedures.put(ShindigNativeQueries.DELETE_APP_DATA_QUERY, proc);

    // updatePersonData
    final Method updateAppData = GraphAppDataSPI.class.getMethod(
            ShindigNativeQueries.UPDATE_APP_DATA_METHOD, String.class, String.class, String.class,
            Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.APP_DATA);

    proc = new NativeProcedure(ShindigNativeQueries.UPDATE_APP_DATA_METHOD, appDataSPI,
            updateAppData, paramNames);
    procedures.put(ShindigNativeQueries.UPDATE_APP_DATA_QUERY, proc);
  }

  private void addMessageService(final GraphMessageSPI messageSPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getMessageCollections
    final Method getMessageCollections = GraphMessageSPI.class.getMethod(
            ShindigNativeQueries.GET_MESSAGE_COLLECTIONS_METHOD, String.class, Map.class,
            List.class);

    List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    IStoredProcedure proc = new NativeProcedure(
            ShindigNativeQueries.GET_MESSAGE_COLLECTIONS_METHOD, messageSPI, getMessageCollections,
            paramNames);
    procedures.put(ShindigNativeQueries.GET_MESSAGE_COLLECTIONS_QUERY, proc);

    // createMessageCollection
    final Method createMessageCollection = GraphMessageSPI.class.getMethod(
            ShindigNativeQueries.CREATE_MESS_COLL_METHOD, String.class, Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_COLLECTION_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.CREATE_MESS_COLL_METHOD, messageSPI,
            createMessageCollection, paramNames);
    procedures.put(ShindigNativeQueries.CREATE_MESS_COLL_QUERY, proc);

    // modifyMessageCollection
    final Method modifyMessageCollection = GraphMessageSPI.class.getMethod(
            ShindigNativeQueries.MODIFY_MESS_COLL_METHOD, String.class, Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_COLLECTION_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.MODIFY_MESS_COLL_METHOD, messageSPI,
            modifyMessageCollection, paramNames);
    procedures.put(ShindigNativeQueries.MODIFY_MESS_COLL_QUERY, proc);

    // deleteMessageCollection
    final Method deleteMessageCollection = GraphMessageSPI.class.getMethod(
            ShindigNativeQueries.DELETE_MESS_COLL_METHOD, String.class, String.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_COLLECTION_ID);

    proc = new NativeProcedure(ShindigNativeQueries.DELETE_MESS_COLL_METHOD, messageSPI,
            deleteMessageCollection, paramNames);
    procedures.put(ShindigNativeQueries.DELETE_MESS_COLL_QUERY, proc);

    // getMessages
    final Method getMessages = GraphMessageSPI.class.getMethod(
            ShindigNativeQueries.GET_MESSAGES_METHOD, String.class, String.class, List.class,
            Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_COLLECTION_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_ID_LIST);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_MESSAGES_METHOD, messageSPI, getMessages,
            paramNames);
    procedures.put(ShindigNativeQueries.GET_MESSAGES_QUERY, proc);

    // createMessage
    final Method createMessage = GraphMessageSPI.class.getMethod(
            ShindigNativeQueries.CREATE_MESSAGE_METHOD, String.class, String.class, String.class,
            Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_COLLECTION_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.CREATE_MESSAGE_METHOD, messageSPI,
            createMessage, paramNames);
    procedures.put(ShindigNativeQueries.CREATE_MESSAGE_QUERY, proc);

    // deleteMessages
    final Method deleteMessages = GraphMessageSPI.class.getMethod(
            ShindigNativeQueries.DELETE_MESSAGES_METHOD, String.class, String.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_COLLECTION_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_ID_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.DELETE_MESSAGES_METHOD, messageSPI,
            deleteMessages, paramNames);
    procedures.put(ShindigNativeQueries.DELETE_MESSAGES_QUERY, proc);

    // modifyMessage
    final Method modifyMessage = GraphMessageSPI.class.getMethod(
            ShindigNativeQueries.MODIFY_MESSAGE_METHOD, String.class, String.class, String.class,
            Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_COLLECTION_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_ID);
    paramNames.add(ShindigNativeQueries.MESSAGE_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.MODIFY_MESSAGE_METHOD, messageSPI,
            modifyMessage, paramNames);
    procedures.put(ShindigNativeQueries.MODIFY_MESSAGE_QUERY, proc);
  }

  private void addAlbumService(final GraphAlbumSPI albumSPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getAlbum
    final Method getAlbum = GraphAlbumSPI.class.getMethod(ShindigNativeQueries.GET_ALBUM_METHOD,
            String.class, String.class, String.class, List.class);

    List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    IStoredProcedure proc = new NativeProcedure(ShindigNativeQueries.GET_ALBUM_METHOD, albumSPI,
            getAlbum, paramNames);
    procedures.put(ShindigNativeQueries.GET_ALBUM_QUERY, proc);

    // getAlbums
    final Method getAlbums = GraphAlbumSPI.class.getMethod(ShindigNativeQueries.GET_ALBUMS_METHOD,
            String.class, String.class, List.class, Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID_LIST);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_ALBUMS_METHOD, albumSPI, getAlbums,
            paramNames);
    procedures.put(ShindigNativeQueries.GET_ALBUMS_QUERY, proc);

    // getGroupAlbums
    final Method getGroupAlbums = GraphAlbumSPI.class.getMethod(
            ShindigNativeQueries.GET_GROUP_ALBUMS_METHOD, List.class, String.class, String.class,
            Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID_LIST);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_GROUP_ALBUMS_METHOD, albumSPI,
            getGroupAlbums, paramNames);
    procedures.put(ShindigNativeQueries.GET_GROUP_ALBUMS_QUERY, proc);

    // deleteAlbum
    final Method deleteAlbum = GraphAlbumSPI.class.getMethod(
            ShindigNativeQueries.DELETE_ALBUM_METHOD, String.class, String.class, String.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);

    proc = new NativeProcedure(ShindigNativeQueries.DELETE_ALBUM_METHOD, albumSPI, deleteAlbum,
            paramNames);
    procedures.put(ShindigNativeQueries.DELETE_ALBUM_QUERY, proc);

    // createAlbum
    final Method createAlbum = GraphAlbumSPI.class.getMethod(
            ShindigNativeQueries.CREATE_ALBUM_METHOD, String.class, String.class, Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.CREATE_ALBUM_METHOD, albumSPI, createAlbum,
            paramNames);
    procedures.put(ShindigNativeQueries.CREATE_ALBUM_QUERY, proc);

    // updateAlbum
    final Method updateAlbum = GraphAlbumSPI.class.getMethod(
            ShindigNativeQueries.UPDATE_ALBUM_METHOD, String.class, String.class, String.class,
            Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.UPDATE_ALBUM_METHOD, albumSPI, updateAlbum,
            paramNames);
    procedures.put(ShindigNativeQueries.UPDATE_ALBUM_QUERY, proc);
  }

  private void addMediaItemService(final GraphMediaItemSPI mediaItemSPI,
          final Map<String, IStoredProcedure> procedures) throws Exception {
    // getMediaItem
    final Method getMediaItem = GraphMediaItemSPI.class.getMethod(
            ShindigNativeQueries.GET_MEDIA_ITEM_METHOD, String.class, String.class, String.class,
            String.class, List.class);

    List<String> paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);
    paramNames.add(ShindigNativeQueries.MEDIA_ITEM_ID);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    IStoredProcedure proc = new NativeProcedure(ShindigNativeQueries.GET_MEDIA_ITEM_METHOD,
            mediaItemSPI, getMediaItem, paramNames);
    procedures.put(ShindigNativeQueries.GET_MEDIA_ITEM_QUERY, proc);

    // getMediaItemsById
    final Method getMediaItemsById = GraphMediaItemSPI.class.getMethod(
            ShindigNativeQueries.GET_MEDIA_ITEMS_BY_ID_METHOD, String.class, String.class,
            String.class, List.class, Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);
    paramNames.add(ShindigNativeQueries.MEDIA_ITEM_ID_LIST);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_MEDIA_ITEMS_BY_ID_METHOD, mediaItemSPI,
            getMediaItemsById, paramNames);
    procedures.put(ShindigNativeQueries.GET_MEDIA_ITEMS_BY_ID_QUERY, proc);

    // getMediaItems
    final Method getMediaItems = GraphMediaItemSPI.class.getMethod(
            ShindigNativeQueries.GET_MEDIA_ITEMS_METHOD, String.class, String.class, String.class,
            Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_MEDIA_ITEMS_METHOD, mediaItemSPI,
            getMediaItems, paramNames);
    procedures.put(ShindigNativeQueries.GET_MEDIA_ITEMS_QUERY, proc);

    // getGroupMediaItems
    final Method getGroupMediaItems = GraphMediaItemSPI.class.getMethod(
            ShindigNativeQueries.GET_GROUP_MEDIA_ITEMS_METHOD, List.class, String.class,
            String.class, Map.class, List.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID_LIST);
    paramNames.add(ShindigNativeQueries.GROUP_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.OPTIONS_MAP);
    paramNames.add(ShindigNativeQueries.FIELD_LIST);

    proc = new NativeProcedure(ShindigNativeQueries.GET_GROUP_MEDIA_ITEMS_METHOD, mediaItemSPI,
            getGroupMediaItems, paramNames);
    procedures.put(ShindigNativeQueries.GET_GROUP_MEDIA_ITEMS_QUERY, proc);

    // deleteMediaItem
    final Method deleteMediaItem = GraphMediaItemSPI.class.getMethod(
            ShindigNativeQueries.DELETE_MEDIA_ITEM_METHOD, String.class, String.class,
            String.class, String.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);
    paramNames.add(ShindigNativeQueries.MEDIA_ITEM_ID);

    proc = new NativeProcedure(ShindigNativeQueries.DELETE_MEDIA_ITEM_METHOD, mediaItemSPI,
            deleteMediaItem, paramNames);
    procedures.put(ShindigNativeQueries.DELETE_MEDIA_ITEM_QUERY, proc);

    // createMediaItem
    final Method createMediaItem = GraphMediaItemSPI.class.getMethod(
            ShindigNativeQueries.CREATE_MEDIA_ITEM_METHOD, String.class, String.class,
            String.class, Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);
    paramNames.add(ShindigNativeQueries.MEDIA_ITEM_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.CREATE_MEDIA_ITEM_METHOD, mediaItemSPI,
            createMediaItem, paramNames);
    procedures.put(ShindigNativeQueries.CREATE_MEDIA_ITEM_QUERY, proc);

    // updateMediaItem
    final Method updateMediaItem = GraphMediaItemSPI.class.getMethod(
            ShindigNativeQueries.UPDATE_MEDIA_ITEM_METHOD, String.class, String.class,
            String.class, String.class, Map.class);

    paramNames = new ArrayList<String>();
    paramNames.add(ShindigNativeQueries.USER_ID);
    paramNames.add(ShindigNativeQueries.APP_ID);
    paramNames.add(ShindigNativeQueries.ALBUM_ID);
    paramNames.add(ShindigNativeQueries.MEDIA_ITEM_ID);
    paramNames.add(ShindigNativeQueries.MEDIA_ITEM_OBJECT);

    proc = new NativeProcedure(ShindigNativeQueries.UPDATE_MEDIA_ITEM_METHOD, mediaItemSPI,
            updateMediaItem, paramNames);
    procedures.put(ShindigNativeQueries.UPDATE_MEDIA_ITEM_QUERY, proc);
  }
}
