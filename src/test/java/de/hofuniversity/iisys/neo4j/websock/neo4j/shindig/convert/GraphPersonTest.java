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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;

/**
 * Test for the person, account, organization, address and list field converter classes.
 */
public class GraphPersonTest {
  private static final String ABOUT_ME_FIELD = "aboutMe";
  private static final String CHILDREN_FIELD = "children";
  private static final String DISP_NAME_FIELD = "displayName";
  private static final String ID_FIELD = "id";
  private static final String JOB_INTS_FIELD = "jobInterests";
  private static final String NICKNAME_FIELD = "nickname";
  private static final String PREF_USERNAME_FIELD = "preferredUsername";
  private static final String PROFILE_URL_FIELD = "profileUrl";
  private static final String REL_STAT_FIELD = "relationshipStatus";
  private static final String STATUS_FIELD = "status";
  private static final String THUMBNAIL_FIELD = "thumbnailUrl";
  private static final String PROFILE_VID_FIELD = "profileVideo";

  private static final String ADD_NAME_FIELD = "additionalName";
  private static final String FAM_NAME_FIELD = "familyName";
  private static final String FORMATTED_FIELD = "formatted";
  private static final String GIV_NAME_FIELD = "givenName";
  private static final String HON_PREF_FIELD = "honorificPrefix";
  private static final String HON_SUFF_FIELD = "honorificSuffix";

  private static final String OFFSET_FIELD = "utcOffset";
  private static final String BDAY_FIELD = "birthday";
  private static final String GENDER_FIELD = "gender";
  private static final String UPDATED_FIELD = "lastUpdated";
  private static final String NET_PRES_FIELD = "networkPresence";

  private static final String ACTIVITIES_FIELD = "activities";
  private static final String BOOKS_FIELD = "books";
  private static final String INTERESTS_FIELD = "interests";
  private static final String LANGUAGES_FIELD = "languagesSpoken";
  private static final String LOOKING_FOR_FIELD = "lookingFor";
  private static final String QUOTES_FIELD = "quotes";
  private static final String TAGS_FIELD = "tags";
  private static final String URLS_FIELD = "urls";

  private static final String ABOUT_ME = "person's about me";
  private static final String CHILDREN = "person's children";
  private static final String DISP_NAME = "person's display name";
  private static final String ID = "person's ID";
  private static final String JOB_INTS = "person's job interests";
  private static final String NICKNAME = "person's nickname";
  private static final String PREF_USERNAME = "person's preferred username";
  private static final String PROFILE_URL = "person's profile URL";
  private static final String REL_STAT = "person's relationship status";
  private static final String STATUS = "person's status";
  private static final String THUMBNAIL = "person's thumbnail URL";
  private static final String PROFILE_VID = "person's profile video URL";

  private static final String ADD_NAME = "person's additional name";
  private static final String FAM_NAME = "person's family name";
  private static final String FORMATTED = "person's formatted name";
  private static final String GIV_NAME = "person's given name";
  private static final String HON_PREF = "person's honorarific prefix";
  private static final String HON_SUFF = "person's honorarific suffix";

  private static final Long OFFSET = 42L;
  private static final Long BIRTHDAY = System.currentTimeMillis();
  private static final String GENDER = "female";
  private static final Long UPDATED = System.currentTimeMillis();
  private static final String NET_PRES = "ONLINE";

  private static final String[] ACTIVITIES = { "activity1", "activity2" };
  private static final String[] BOOKS = { "book1", "book2" };
  private static final String[] INTERESTS = { "interest1", "interest2" };
  private static final String[] LANGUAGES = { "language1", "language2" };
  private static final String[] LOOKING_FOR = { "FRIENDS", "RANDOM" };
  private static final String[] QUOTES = { "quote1", "quote2" };
  private static final String[] TAGS = { "tag1", "tag2" };
  private static final String[] URLS = { "url1", "url2" };

  // account
  private static final String ACCOUNTS_FIELD = "accounts";
  private static final String DOMAIN_FIELD = "domain";
  private static final String USER_ID_FIELD = "userId";
  private static final String USER_NAME_FIELD = "userName";

  private static final String ACC_DOMAIN = "account domain";
  private static final String ACC_USER_ID = "account user ID";
  private static final String ACC_USER_NAME = "account user name";

  // address
  private static final String ADDRESS_FIELD = "address";
  private static final String ADDRESSES_FIELD = "addresses";
  private static final String CURR_LOC_FIELD = "currentLocation";
  private static final String COUNTRY_FIELD = "country";
  private static final String LATITUDE_FIELD = "latitude";
  private static final String LONGITUDE_FIELD = "longitude";
  private static final String LOCALITY_FIELD = "locality";
  private static final String POSTAL_FIELD = "postalCode";
  private static final String REGION_FIELD = "region";
  private static final String ADD_ADDRESS_FIELD = "streetAddress";
  private static final String ADD_TYPE_FIELD = "type";
  private static final String PRIMARY_FIELD = "primary";

  private static final String ADD_COUNTRY = "address country";
  private static final Float ADD_LATITUDE = 42.0f;
  private static final Float ADD_LONGITUDE = 3.14159f;
  private static final String ADD_LOCALITY = "address locality";
  private static final String ADD_POSTAL = "address postal code";
  private static final String ADD_REGION = "address region";
  private static final String ADD_ADDRESS = "address street address";
  private static final String ADD_TYPE = "address type";
  private static final String ADD_FORMATTED = "address formatted";
  private static final Boolean ADD_PRIMARY = true;

  // organization, affiliation
  private static final String ORGANIZATIONS_FIELD = "organizations";
  private static final String DESCRIPTION_FIELD = "description";
  private static final String END_DATE_FIELD = "endDate";
  private static final String FIELD_FIELD = "field";
  private static final String NAME_FIELD = "name";
  private static final String SALARY_FIELD = "salary";
  private static final String START_DATE_FIELD = "startDate";
  private static final String SUB_FIELD_FIELD = "subField";
  private static final String TITLE_FIELD = "title";
  private static final String WEBPAGE_FIELD = "webpage";
  private static final String ORG_TYPE_FIELD = "type";
  private static final String DEPARTMENT_FIELD = "department";
  private static final String MANAGER_ID_FIELD = "managerId";
  private static final String SECRETARY_ID_FIELD = "secretaryId";
  private static final String DEPARTMENT_HEAD_FIELD = "departmentHead";

  private static final String ORG_DESCRIPTION = "organization description";
  private static final Long AFF_END_DATE = System.currentTimeMillis();
  private static final String ORG_FIELD = "organization field";
  private static final String ORG_NAME = "organization name";
  private static final String AFF_SALARY = "affiliation salary";
  private static final Long AFF_START_DATE = System.currentTimeMillis();
  private static final String ORG_SUB_FIELD = "organization sub-field";
  private static final String AFF_TITLE = "affiliation title";
  private static final String ORG_WEB_PAGE = "organization web page";
  private static final String ORG_TYPE = "organization type";
  private static final Boolean AFF_PRIMARY = true;
  private static final String EXT_AFF_DEPARTMENT = "affiliation department";
  private static final String EXT_AFF_MANAGER_ID = "affiliation manager ID";
  private static final String EXT_AFF_SECR_ID = "affiliation secretary ID";
  private static final Boolean EXT_AFF_DEP_HEAD = true;

  // list field list data
  private static final String PHONES_FIELD = "phoneNumbers";
  private static final String EMAILS_FIELD = "emails";
  private static final String IMS_FIELD = "ims";

  private static final String LFL_TYPE_FIELD = "type";
  private static final String LFL_VALUE_FIELD = "value";
  private static final String LFL_PRIMARY_FIELD = "primary";

  private static final String[] LFL_TYPES = { "type1", "type2", "type3" };
  private static final String[] LFL_VALUES = { "value1", "value2", "value3" };
  private static final Integer LFL_PRIMARY = 1;

  private GraphDatabaseService fDb;
  private Node fPersonNode;

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
        if (GraphPersonTest.this.fDb != null) {
          GraphPersonTest.this.fDb.shutdown();
        }
      }
    });

    createTestData();
  }

  private void createTestData() {
    final Transaction trans = this.fDb.beginTx();

    this.fPersonNode = this.fDb.createNode();

    this.fPersonNode.setProperty(GraphPersonTest.ABOUT_ME_FIELD, GraphPersonTest.ABOUT_ME);
    this.fPersonNode.setProperty(GraphPersonTest.CHILDREN_FIELD, GraphPersonTest.CHILDREN);
    this.fPersonNode.setProperty(GraphPersonTest.DISP_NAME_FIELD, GraphPersonTest.DISP_NAME);
    this.fPersonNode.setProperty(GraphPersonTest.ID_FIELD, GraphPersonTest.ID);
    this.fPersonNode.setProperty(GraphPersonTest.JOB_INTS_FIELD, GraphPersonTest.JOB_INTS);
    this.fPersonNode.setProperty(GraphPersonTest.NICKNAME_FIELD, GraphPersonTest.NICKNAME);
    this.fPersonNode
            .setProperty(GraphPersonTest.PREF_USERNAME_FIELD, GraphPersonTest.PREF_USERNAME);
    this.fPersonNode.setProperty(GraphPersonTest.PROFILE_URL_FIELD, GraphPersonTest.PROFILE_URL);
    this.fPersonNode.setProperty(GraphPersonTest.REL_STAT_FIELD, GraphPersonTest.REL_STAT);
    this.fPersonNode.setProperty(GraphPersonTest.CHILDREN_FIELD, GraphPersonTest.CHILDREN);
    this.fPersonNode.setProperty(GraphPersonTest.STATUS_FIELD, GraphPersonTest.STATUS);
    this.fPersonNode.setProperty(GraphPersonTest.THUMBNAIL_FIELD, GraphPersonTest.THUMBNAIL);
    this.fPersonNode.setProperty(GraphPersonTest.PROFILE_VID_FIELD, GraphPersonTest.PROFILE_VID);

    this.fPersonNode.setProperty(GraphPersonTest.ADD_NAME_FIELD, GraphPersonTest.ADD_NAME);
    this.fPersonNode.setProperty(GraphPersonTest.FAM_NAME_FIELD, GraphPersonTest.FAM_NAME);
    this.fPersonNode.setProperty(GraphPersonTest.FORMATTED_FIELD, GraphPersonTest.FORMATTED);
    this.fPersonNode.setProperty(GraphPersonTest.GIV_NAME_FIELD, GraphPersonTest.GIV_NAME);
    this.fPersonNode.setProperty(GraphPersonTest.HON_PREF_FIELD, GraphPersonTest.HON_PREF);
    this.fPersonNode.setProperty(GraphPersonTest.HON_SUFF_FIELD, GraphPersonTest.HON_SUFF);

    this.fPersonNode.setProperty(GraphPersonTest.OFFSET_FIELD, GraphPersonTest.OFFSET);
    this.fPersonNode.setProperty(GraphPersonTest.BDAY_FIELD, GraphPersonTest.BIRTHDAY);
    this.fPersonNode.setProperty(GraphPersonTest.GENDER_FIELD, GraphPersonTest.GENDER);
    this.fPersonNode.setProperty(GraphPersonTest.UPDATED_FIELD, GraphPersonTest.UPDATED);
    this.fPersonNode.setProperty(GraphPersonTest.NET_PRES_FIELD, GraphPersonTest.NET_PRES);

    this.fPersonNode.setProperty(GraphPersonTest.ACTIVITIES_FIELD, GraphPersonTest.ACTIVITIES);
    this.fPersonNode.setProperty(GraphPersonTest.BOOKS_FIELD, GraphPersonTest.BOOKS);
    this.fPersonNode.setProperty(GraphPersonTest.INTERESTS_FIELD, GraphPersonTest.INTERESTS);
    this.fPersonNode.setProperty(GraphPersonTest.LANGUAGES_FIELD, GraphPersonTest.LANGUAGES);
    this.fPersonNode.setProperty(GraphPersonTest.LOOKING_FOR_FIELD, GraphPersonTest.LOOKING_FOR);
    this.fPersonNode.setProperty(GraphPersonTest.QUOTES_FIELD, GraphPersonTest.QUOTES);
    this.fPersonNode.setProperty(GraphPersonTest.TAGS_FIELD, GraphPersonTest.TAGS);
    this.fPersonNode.setProperty(GraphPersonTest.URLS_FIELD, GraphPersonTest.URLS);

    // account
    final Relationship accRel = this.fPersonNode.createRelationshipTo(this.fDb.createNode(),
            Neo4jRelTypes.ACCOUNT);
    accRel.setProperty(GraphPersonTest.DOMAIN_FIELD, GraphPersonTest.ACC_DOMAIN);
    accRel.setProperty(GraphPersonTest.USER_ID_FIELD, GraphPersonTest.ACC_USER_ID);
    accRel.setProperty(GraphPersonTest.USER_NAME_FIELD, GraphPersonTest.ACC_USER_NAME);

    // address (located at and currently at)
    final Node addNode = this.fDb.createNode();
    this.fPersonNode.createRelationshipTo(addNode, Neo4jRelTypes.CURRENTLY_AT);
    this.fPersonNode.createRelationshipTo(addNode, Neo4jRelTypes.LOCATED_AT);

    addNode.setProperty(GraphPersonTest.COUNTRY_FIELD, GraphPersonTest.ADD_COUNTRY);
    addNode.setProperty(GraphPersonTest.FORMATTED_FIELD, GraphPersonTest.ADD_FORMATTED);
    addNode.setProperty(GraphPersonTest.LATITUDE_FIELD, GraphPersonTest.ADD_LATITUDE);
    addNode.setProperty(GraphPersonTest.LONGITUDE_FIELD, GraphPersonTest.ADD_LONGITUDE);
    addNode.setProperty(GraphPersonTest.LOCALITY_FIELD, GraphPersonTest.ADD_LOCALITY);
    addNode.setProperty(GraphPersonTest.POSTAL_FIELD, GraphPersonTest.ADD_POSTAL);
    addNode.setProperty(GraphPersonTest.PRIMARY_FIELD, GraphPersonTest.ADD_PRIMARY);
    addNode.setProperty(GraphPersonTest.REGION_FIELD, GraphPersonTest.ADD_REGION);
    addNode.setProperty(GraphPersonTest.ADD_ADDRESS_FIELD, GraphPersonTest.ADD_ADDRESS);
    addNode.setProperty(GraphPersonTest.ADD_TYPE_FIELD, GraphPersonTest.ADD_TYPE);

    // organization, affiliation
    final Node orgNode = this.fDb.createNode();
    orgNode.createRelationshipTo(addNode, Neo4jRelTypes.LOCATED_AT);
    final Relationship affRel = this.fPersonNode.createRelationshipTo(orgNode,
            Neo4jRelTypes.AFFILIATED);

    orgNode.setProperty(GraphPersonTest.DESCRIPTION_FIELD, GraphPersonTest.ORG_DESCRIPTION);
    orgNode.setProperty(GraphPersonTest.FIELD_FIELD, GraphPersonTest.ORG_FIELD);
    orgNode.setProperty(GraphPersonTest.NAME_FIELD, GraphPersonTest.ORG_NAME);
    orgNode.setProperty(GraphPersonTest.SUB_FIELD_FIELD, GraphPersonTest.ORG_SUB_FIELD);
    orgNode.setProperty(GraphPersonTest.ORG_TYPE_FIELD, GraphPersonTest.ORG_TYPE);
    orgNode.setProperty(GraphPersonTest.WEBPAGE_FIELD, GraphPersonTest.ORG_WEB_PAGE);

    affRel.setProperty(GraphPersonTest.END_DATE_FIELD, GraphPersonTest.AFF_END_DATE);
    affRel.setProperty(GraphPersonTest.PRIMARY_FIELD, GraphPersonTest.AFF_PRIMARY);
    affRel.setProperty(GraphPersonTest.SALARY_FIELD, GraphPersonTest.AFF_SALARY);
    affRel.setProperty(GraphPersonTest.START_DATE_FIELD, GraphPersonTest.AFF_START_DATE);
    affRel.setProperty(GraphPersonTest.TITLE_FIELD, GraphPersonTest.AFF_TITLE);
    affRel.setProperty(GraphPersonTest.DEPARTMENT_FIELD, GraphPersonTest.EXT_AFF_DEPARTMENT);
    affRel.setProperty(GraphPersonTest.MANAGER_ID_FIELD, GraphPersonTest.EXT_AFF_MANAGER_ID);
    affRel.setProperty(GraphPersonTest.SECRETARY_ID_FIELD, GraphPersonTest.EXT_AFF_SECR_ID);
    affRel.setProperty(GraphPersonTest.DEPARTMENT_HEAD_FIELD, GraphPersonTest.EXT_AFF_DEP_HEAD);

    // list field lists - same converter class, different links
    final Node lflNode = this.fDb.createNode();
    lflNode.setProperty(GraphPersonTest.LFL_TYPE_FIELD, GraphPersonTest.LFL_TYPES);
    lflNode.setProperty(GraphPersonTest.LFL_VALUE_FIELD, GraphPersonTest.LFL_VALUES);
    lflNode.setProperty(GraphPersonTest.LFL_PRIMARY_FIELD, GraphPersonTest.LFL_PRIMARY);

    // phone numbers
    this.fPersonNode.createRelationshipTo(lflNode, Neo4jRelTypes.PHONE_NUMS);

    // email addresses
    this.fPersonNode.createRelationshipTo(lflNode, Neo4jRelTypes.EMAILS);

    // instant messengers
    this.fPersonNode.createRelationshipTo(lflNode, Neo4jRelTypes.IMS);

    trans.success();
    trans.finish();
  }

  /**
   * Test for conversion of existing data.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void conversionTest() {
    final Map<String, Object> p = new GraphPerson(this.fPersonNode).toMap(null);

    Assert.assertEquals(GraphPersonTest.ABOUT_ME, p.get(GraphPersonTest.ABOUT_ME_FIELD));
    Assert.assertEquals(GraphPersonTest.CHILDREN, p.get(GraphPersonTest.CHILDREN_FIELD));
    Assert.assertEquals(GraphPersonTest.DISP_NAME, p.get(GraphPersonTest.DISP_NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.ID, p.get(GraphPersonTest.ID_FIELD));
    Assert.assertEquals(GraphPersonTest.JOB_INTS, p.get(GraphPersonTest.JOB_INTS_FIELD));
    Assert.assertEquals(GraphPersonTest.NICKNAME, p.get(GraphPersonTest.NICKNAME_FIELD));
    Assert.assertEquals(GraphPersonTest.PREF_USERNAME, p.get(GraphPersonTest.PREF_USERNAME_FIELD));
    Assert.assertEquals(GraphPersonTest.PROFILE_URL, p.get(GraphPersonTest.PROFILE_URL_FIELD));
    Assert.assertEquals(GraphPersonTest.REL_STAT, p.get(GraphPersonTest.REL_STAT_FIELD));
    Assert.assertEquals(GraphPersonTest.STATUS, p.get(GraphPersonTest.STATUS_FIELD));
    Assert.assertEquals(GraphPersonTest.THUMBNAIL, p.get(GraphPersonTest.THUMBNAIL_FIELD));
    Assert.assertEquals(GraphPersonTest.PROFILE_VID, p.get(GraphPersonTest.PROFILE_VID_FIELD));

    Assert.assertEquals(GraphPersonTest.ADD_NAME, p.get(GraphPersonTest.ADD_NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.FAM_NAME, p.get(GraphPersonTest.FAM_NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.FORMATTED, p.get(GraphPersonTest.FORMATTED_FIELD));
    Assert.assertEquals(GraphPersonTest.GIV_NAME, p.get(GraphPersonTest.GIV_NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.HON_PREF, p.get(GraphPersonTest.HON_PREF_FIELD));
    Assert.assertEquals(GraphPersonTest.HON_SUFF, p.get(GraphPersonTest.HON_SUFF_FIELD));

    Assert.assertEquals(GraphPersonTest.OFFSET, p.get(GraphPersonTest.OFFSET_FIELD));
    Assert.assertEquals(GraphPersonTest.BIRTHDAY, p.get(GraphPersonTest.BDAY_FIELD));
    Assert.assertEquals(GraphPersonTest.GENDER, p.get(GraphPersonTest.GENDER_FIELD));
    Assert.assertEquals(GraphPersonTest.UPDATED, p.get(GraphPersonTest.UPDATED_FIELD));
    Assert.assertEquals(GraphPersonTest.NET_PRES, p.get(GraphPersonTest.NET_PRES_FIELD));

    final String[] activities = (String[]) p.get(GraphPersonTest.ACTIVITIES_FIELD);
    Assert.assertArrayEquals(GraphPersonTest.ACTIVITIES, activities);

    final String[] books = (String[]) p.get(GraphPersonTest.BOOKS_FIELD);
    Assert.assertArrayEquals(GraphPersonTest.BOOKS, books);

    final String[] interests = (String[]) p.get(GraphPersonTest.INTERESTS_FIELD);
    Assert.assertArrayEquals(GraphPersonTest.INTERESTS, interests);

    final String[] languages = (String[]) p.get(GraphPersonTest.LANGUAGES_FIELD);
    Assert.assertArrayEquals(GraphPersonTest.LANGUAGES, languages);

    final String[] lookings = (String[]) p.get(GraphPersonTest.LOOKING_FOR_FIELD);
    Assert.assertArrayEquals(GraphPersonTest.LOOKING_FOR, lookings);

    final String[] quotes = (String[]) p.get(GraphPersonTest.QUOTES_FIELD);
    Assert.assertArrayEquals(GraphPersonTest.QUOTES, quotes);

    final String[] tags = (String[]) p.get(GraphPersonTest.TAGS_FIELD);
    Assert.assertArrayEquals(GraphPersonTest.TAGS, tags);

    final String[] urls = (String[]) p.get(GraphPersonTest.URLS_FIELD);
    Assert.assertArrayEquals(GraphPersonTest.URLS, urls);

    // account
    final List<Map<String, Object>> accounts = (List<Map<String, Object>>) p
            .get(GraphPersonTest.ACCOUNTS_FIELD);
    Assert.assertEquals(1, accounts.size());
    final Map<String, Object> acc = accounts.get(0);
    Assert.assertEquals(GraphPersonTest.ACC_DOMAIN, acc.get(GraphPersonTest.DOMAIN_FIELD));
    Assert.assertEquals(GraphPersonTest.ACC_USER_ID, acc.get(GraphPersonTest.USER_ID_FIELD));
    Assert.assertEquals(GraphPersonTest.ACC_USER_NAME, acc.get(GraphPersonTest.USER_NAME_FIELD));

    // address
    final Map<String, Object> add = (Map<String, Object>) p.get(GraphPersonTest.CURR_LOC_FIELD);
    Assert.assertEquals(GraphPersonTest.ADD_ADDRESS, add.get(GraphPersonTest.ADD_ADDRESS_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_COUNTRY, add.get(GraphPersonTest.COUNTRY_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_FORMATTED, add.get(GraphPersonTest.FORMATTED_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_LOCALITY, add.get(GraphPersonTest.LOCALITY_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_POSTAL, add.get(GraphPersonTest.POSTAL_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_REGION, add.get(GraphPersonTest.REGION_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_TYPE, add.get(GraphPersonTest.ADD_TYPE_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_LATITUDE, add.get(GraphPersonTest.LATITUDE_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_LONGITUDE, add.get(GraphPersonTest.LONGITUDE_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_PRIMARY, add.get(GraphPersonTest.PRIMARY_FIELD));

    // sanity check, same address
    final List<Map<String, Object>> addresses = (List<Map<String, Object>>) p
            .get(GraphPersonTest.ADDRESSES_FIELD);
    Assert.assertEquals(1, addresses.size());
    Assert.assertEquals(addresses.get(0).get(GraphPersonTest.FORMATTED_FIELD),
            GraphPersonTest.ADD_FORMATTED);

    // organization, affiliation
    final List<Map<String, Object>> organizations = (List<Map<String, Object>>) p
            .get(GraphPersonTest.ORGANIZATIONS_FIELD);
    Assert.assertEquals(1, organizations.size());
    final Map<String, Object> org = organizations.get(0);
    Assert.assertEquals(GraphPersonTest.EXT_AFF_DEPARTMENT,
            org.get(GraphPersonTest.DEPARTMENT_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_DESCRIPTION, org.get(GraphPersonTest.DESCRIPTION_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_FIELD, org.get(GraphPersonTest.FIELD_FIELD));
    Assert.assertEquals(GraphPersonTest.EXT_AFF_MANAGER_ID,
            org.get(GraphPersonTest.MANAGER_ID_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_NAME, org.get(GraphPersonTest.NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_SALARY, org.get(GraphPersonTest.SALARY_FIELD));
    Assert.assertEquals(GraphPersonTest.EXT_AFF_SECR_ID,
            org.get(GraphPersonTest.SECRETARY_ID_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_SUB_FIELD, org.get(GraphPersonTest.SUB_FIELD_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_TITLE, org.get(GraphPersonTest.TITLE_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_TYPE, org.get(GraphPersonTest.ORG_TYPE_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_WEB_PAGE, org.get(GraphPersonTest.WEBPAGE_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_END_DATE, org.get(GraphPersonTest.END_DATE_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_START_DATE, org.get(GraphPersonTest.START_DATE_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_PRIMARY, org.get(GraphPersonTest.PRIMARY_FIELD));
    Assert.assertEquals(GraphPersonTest.EXT_AFF_DEP_HEAD,
            org.get(GraphPersonTest.DEPARTMENT_HEAD_FIELD));

    // sanity check, same address
    final Map<String, Object> orgAdd = (Map<String, Object>) org.get(GraphPersonTest.ADDRESS_FIELD);
    Assert.assertEquals(orgAdd.get(GraphPersonTest.FORMATTED_FIELD), GraphPersonTest.ADD_FORMATTED);

    // phone numbers
    final Map<String, Object> phones = (Map<String, Object>) p.get(GraphPersonTest.PHONES_FIELD);
    Assert.assertEquals(3, phones.size());

    Assert.assertArrayEquals(GraphPersonTest.LFL_TYPES,
            (String[]) phones.get(GraphPersonTest.LFL_TYPE_FIELD));
    Assert.assertArrayEquals(GraphPersonTest.LFL_TYPES,
            (String[]) phones.get(GraphPersonTest.LFL_TYPE_FIELD));
    Assert.assertEquals(GraphPersonTest.LFL_PRIMARY, phones.get(GraphPersonTest.LFL_PRIMARY_FIELD));

    // email addresses, sanity check
    final Map<String, Object> emails = (Map<String, Object>) p.get(GraphPersonTest.EMAILS_FIELD);
    Assert.assertEquals(3, emails.size());

    // instant messengers, sanity check
    final Map<String, Object> ims = (Map<String, Object>) p.get(GraphPersonTest.IMS_FIELD);
    Assert.assertEquals(3, ims.size());
  }

  /**
   * Test for value storing capabilities.
   */
  @Test
  public void storageTest() {
    final Map<String, Object> p = new GraphPerson(this.fPersonNode).toMap(null);

    final Transaction trans = this.fDb.beginTx();

    final Node newPerson = this.fDb.createNode();
    final GraphPerson gPerson = new GraphPerson(newPerson);
    gPerson.setData(p);
    gPerson.updateRelationships(p);

    trans.success();
    trans.finish();

    Assert.assertEquals(GraphPersonTest.ABOUT_ME,
            newPerson.getProperty(GraphPersonTest.ABOUT_ME_FIELD));
    Assert.assertEquals(GraphPersonTest.CHILDREN,
            newPerson.getProperty(GraphPersonTest.CHILDREN_FIELD));
    Assert.assertEquals(GraphPersonTest.DISP_NAME,
            newPerson.getProperty(GraphPersonTest.DISP_NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.JOB_INTS,
            newPerson.getProperty(GraphPersonTest.JOB_INTS_FIELD));
    Assert.assertEquals(GraphPersonTest.NICKNAME,
            newPerson.getProperty(GraphPersonTest.NICKNAME_FIELD));
    Assert.assertEquals(GraphPersonTest.PREF_USERNAME,
            newPerson.getProperty(GraphPersonTest.PREF_USERNAME_FIELD));
    Assert.assertEquals(GraphPersonTest.PROFILE_URL,
            newPerson.getProperty(GraphPersonTest.PROFILE_URL_FIELD));
    Assert.assertEquals(GraphPersonTest.REL_STAT,
            newPerson.getProperty(GraphPersonTest.REL_STAT_FIELD));
    Assert.assertEquals(GraphPersonTest.STATUS, newPerson.getProperty(GraphPersonTest.STATUS_FIELD));
    Assert.assertEquals(GraphPersonTest.THUMBNAIL,
            newPerson.getProperty(GraphPersonTest.THUMBNAIL_FIELD));
    Assert.assertEquals(GraphPersonTest.PROFILE_VID,
            newPerson.getProperty(GraphPersonTest.PROFILE_VID_FIELD));

    Assert.assertEquals(GraphPersonTest.ADD_NAME,
            newPerson.getProperty(GraphPersonTest.ADD_NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.FAM_NAME,
            newPerson.getProperty(GraphPersonTest.FAM_NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.FORMATTED,
            newPerson.getProperty(GraphPersonTest.FORMATTED_FIELD));
    Assert.assertEquals(GraphPersonTest.GIV_NAME,
            newPerson.getProperty(GraphPersonTest.GIV_NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.HON_PREF,
            newPerson.getProperty(GraphPersonTest.HON_PREF_FIELD));
    Assert.assertEquals(GraphPersonTest.HON_SUFF,
            newPerson.getProperty(GraphPersonTest.HON_SUFF_FIELD));

    Assert.assertEquals(GraphPersonTest.OFFSET, newPerson.getProperty(GraphPersonTest.OFFSET_FIELD));
    Assert.assertEquals(GraphPersonTest.BIRTHDAY, newPerson.getProperty(GraphPersonTest.BDAY_FIELD));
    Assert.assertEquals(GraphPersonTest.GENDER, newPerson.getProperty(GraphPersonTest.GENDER_FIELD));
    Assert.assertEquals(GraphPersonTest.UPDATED,
            newPerson.getProperty(GraphPersonTest.UPDATED_FIELD));
    Assert.assertEquals(GraphPersonTest.NET_PRES,
            newPerson.getProperty(GraphPersonTest.NET_PRES_FIELD));

    final String[] activities = (String[]) newPerson.getProperty(GraphPersonTest.ACTIVITIES_FIELD);
    Assert.assertTrue(Arrays.equals(GraphPersonTest.ACTIVITIES, activities));

    final String[] books = (String[]) newPerson.getProperty(GraphPersonTest.BOOKS_FIELD);
    Assert.assertTrue(Arrays.equals(GraphPersonTest.BOOKS, books));

    final String[] interests = (String[]) newPerson.getProperty(GraphPersonTest.INTERESTS_FIELD);
    Assert.assertTrue(Arrays.equals(GraphPersonTest.INTERESTS, interests));

    final String[] languages = (String[]) newPerson.getProperty(GraphPersonTest.LANGUAGES_FIELD);
    Assert.assertTrue(Arrays.equals(GraphPersonTest.LANGUAGES, languages));

    final String[] lookings = (String[]) newPerson.getProperty(GraphPersonTest.LOOKING_FOR_FIELD);
    Assert.assertTrue(Arrays.equals(GraphPersonTest.LOOKING_FOR, lookings));

    final String[] quotes = (String[]) newPerson.getProperty(GraphPersonTest.QUOTES_FIELD);
    Assert.assertTrue(Arrays.equals(GraphPersonTest.QUOTES, quotes));

    final String[] tags = (String[]) newPerson.getProperty(GraphPersonTest.TAGS_FIELD);
    Assert.assertTrue(Arrays.equals(GraphPersonTest.TAGS, tags));

    final String[] urls = (String[]) newPerson.getProperty(GraphPersonTest.URLS_FIELD);
    Assert.assertTrue(Arrays.equals(GraphPersonTest.URLS, urls));

    // address (split up due to lack of unique identifiers)
    final Node currAdd = newPerson.getSingleRelationship(Neo4jRelTypes.CURRENTLY_AT,
            Direction.OUTGOING).getEndNode();
    Assert.assertEquals(GraphPersonTest.ADD_COUNTRY,
            currAdd.getProperty(GraphPersonTest.COUNTRY_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_FORMATTED,
            currAdd.getProperty(GraphPersonTest.FORMATTED_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_LATITUDE,
            currAdd.getProperty(GraphPersonTest.LATITUDE_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_LOCALITY,
            currAdd.getProperty(GraphPersonTest.LOCALITY_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_LONGITUDE,
            currAdd.getProperty(GraphPersonTest.LONGITUDE_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_POSTAL,
            currAdd.getProperty(GraphPersonTest.POSTAL_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_PRIMARY,
            currAdd.getProperty(GraphPersonTest.PRIMARY_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_REGION,
            currAdd.getProperty(GraphPersonTest.REGION_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_ADDRESS,
            currAdd.getProperty(GraphPersonTest.ADD_ADDRESS_FIELD));
    Assert.assertEquals(GraphPersonTest.ADD_TYPE,
            currAdd.getProperty(GraphPersonTest.ADD_TYPE_FIELD));

    final Node locAdd = newPerson.getSingleRelationship(Neo4jRelTypes.LOCATED_AT,
            Direction.OUTGOING).getEndNode();
    Assert.assertEquals(locAdd.getProperty(GraphPersonTest.FORMATTED_FIELD),
            GraphPersonTest.ADD_FORMATTED);

    // affiliation
    final Relationship aff = newPerson.getSingleRelationship(Neo4jRelTypes.AFFILIATED,
            Direction.OUTGOING);
    Assert.assertEquals(GraphPersonTest.AFF_SALARY, aff.getProperty(GraphPersonTest.SALARY_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_TITLE, aff.getProperty(GraphPersonTest.TITLE_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_END_DATE,
            aff.getProperty(GraphPersonTest.END_DATE_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_PRIMARY, aff.getProperty(GraphPersonTest.PRIMARY_FIELD));
    Assert.assertEquals(GraphPersonTest.AFF_START_DATE,
            aff.getProperty(GraphPersonTest.START_DATE_FIELD));
    Assert.assertEquals(GraphPersonTest.EXT_AFF_DEPARTMENT,
            aff.getProperty(GraphPersonTest.DEPARTMENT_FIELD));
    Assert.assertEquals(GraphPersonTest.EXT_AFF_MANAGER_ID,
            aff.getProperty(GraphPersonTest.MANAGER_ID_FIELD));
    Assert.assertEquals(GraphPersonTest.EXT_AFF_SECR_ID,
            aff.getProperty(GraphPersonTest.SECRETARY_ID_FIELD));
    Assert.assertEquals(GraphPersonTest.EXT_AFF_DEP_HEAD,
            aff.getProperty(GraphPersonTest.DEPARTMENT_HEAD_FIELD));

    // organization
    final Node org = aff.getEndNode();
    Assert.assertEquals(GraphPersonTest.ORG_DESCRIPTION,
            org.getProperty(GraphPersonTest.DESCRIPTION_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_FIELD, org.getProperty(GraphPersonTest.FIELD_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_NAME, org.getProperty(GraphPersonTest.NAME_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_SUB_FIELD,
            org.getProperty(GraphPersonTest.SUB_FIELD_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_TYPE, org.getProperty(GraphPersonTest.ORG_TYPE_FIELD));
    Assert.assertEquals(GraphPersonTest.ORG_WEB_PAGE,
            org.getProperty(GraphPersonTest.WEBPAGE_FIELD));

    final Node orgAdd = org.getSingleRelationship(Neo4jRelTypes.LOCATED_AT, Direction.OUTGOING)
            .getEndNode();
    Assert.assertEquals(orgAdd.getProperty(GraphPersonTest.FORMATTED_FIELD),
            GraphPersonTest.ADD_FORMATTED);

    // phone numbers
    final Node phoneNode = newPerson.getSingleRelationship(Neo4jRelTypes.PHONE_NUMS,
            Direction.OUTGOING).getEndNode();
    String[] types = (String[]) phoneNode.getProperty(GraphPersonTest.LFL_TYPE_FIELD);
    String[] values = (String[]) phoneNode.getProperty(GraphPersonTest.LFL_VALUE_FIELD);
    Integer primary = (Integer) phoneNode.getProperty(GraphPersonTest.PRIMARY_FIELD);

    Assert.assertEquals(3, types.length);
    Assert.assertEquals(3, values.length);

    Assert.assertTrue(Arrays.equals(types, GraphPersonTest.LFL_TYPES));
    Assert.assertTrue(Arrays.equals(values, GraphPersonTest.LFL_VALUES));
    Assert.assertEquals(GraphPersonTest.LFL_PRIMARY, primary);

    // email addresses, sanity check
    final Node mailNode = newPerson.getSingleRelationship(Neo4jRelTypes.EMAILS, Direction.OUTGOING)
            .getEndNode();
    types = (String[]) mailNode.getProperty(GraphPersonTest.LFL_TYPE_FIELD);
    values = (String[]) mailNode.getProperty(GraphPersonTest.LFL_VALUE_FIELD);
    primary = (Integer) mailNode.getProperty(GraphPersonTest.PRIMARY_FIELD);

    Assert.assertEquals(3, types.length);
    Assert.assertEquals(3, values.length);
    Assert.assertEquals(GraphPersonTest.LFL_PRIMARY, primary);

    // instant messengers, sanity check
    final Node imNode = newPerson.getSingleRelationship(Neo4jRelTypes.IMS, Direction.OUTGOING)
            .getEndNode();
    types = (String[]) imNode.getProperty(GraphPersonTest.LFL_TYPE_FIELD);
    values = (String[]) imNode.getProperty(GraphPersonTest.LFL_VALUE_FIELD);
    primary = (Integer) imNode.getProperty(GraphPersonTest.PRIMARY_FIELD);

    Assert.assertEquals(3, types.length);
    Assert.assertEquals(3, values.length);
    Assert.assertEquals(GraphPersonTest.LFL_PRIMARY, primary);
  }
}
