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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class converting person objects stored in the graph to transferable objects and vice
 * versa.
 */
public class GraphPerson implements IGraphObject {
  // TODO: verify fields
  private static final String ACCOUNTS_FIELD = "accounts";
  private static final String ADDRESSES_FIELD = "addresses";
  private static final String APP_DATA_FIELD = "appData";
  private static final String CURR_LOC_FIELD = "currentLocation";
  private static final String ORGS_FIELD = "organizations";

  private static final String ABOUT_ME_FIELD = "aboutMe";
  private static final String AGE_FIELD = "age";
  private static final String CHILDREN_FIELD = "children";
  private static final String DISP_NAME_FIELD = "displayName";
  private static final String ID_FIELD = "id";
  private static final String JOB_INTS_FIELD = "jobInterests";
  private static final String NICKNAME_FIELD = "nickname";
  private static final String PREF_USRNAME_FIELD = "preferredUsername";
  private static final String PROFILE_URL_FIELD = "profileUrl";
  private static final String REL_STAT_FIELD = "relationshipStatus";
  private static final String STATUS_FIELD = "status";
  private static final String THUMBNAIL_FIELD = "thumbnailUrl";
  private static final String OFFSET_FIELD = "utcOffset";

  private static final String BDAY_FIELD = "birthday";
  private static final String GENDER_FIELD = "gender";
  private static final String UPDATED_FIELD = "lastUpdated";
  private static final String NET_PRES_FIELD = "networkPresence";
  private static final String PROF_VID_FIELD = "profileVideo";

  private static final String ADD_NAME_FIELD = "additionalName";
  private static final String FAM_NAME_FIELD = "familyName";
  private static final String FORMATTED_FIELD = "formatted";
  private static final String GIV_NAME_FIELD = "givenName";
  private static final String HON_PREF_FIELD = "honorificPrefix";
  private static final String HON_SUFF_FIELD = "honorificSuffix";
  private static final String NAME_FIELD = "name";

  private static final String ACTIVITIES_FIELD = "activities";
  private static final String BOOKS_FIELD = "books";
  private static final String EMAILS_FIELD = "emails";
  private static final String IMS_FIELD = "ims";
  private static final String INTERESTS_FIELD = "interests";
  private static final String LANGUAGES_FIELD = "languagesSpoken";
  private static final String LOOKING_FIELD = "lookingFor";
  private static final String PHONES_FIELD = "phoneNumbers";
  private static final String PHOTOS_FIELD = "photos";
  private static final String QUOTES_FIELD = "quotes";
  private static final String TAGS_FIELD = "tags";
  private static final String URLS_FIELD = "urls";

  private static final String BUILD_FIELD = "build";
  private static final String EYE_COLOR_FIELD = "eyeColor";
  private static final String HAIR_COLOR_FIELD = "hairColor";
  private static final String WEIGHT_FIELD = "weight";
  private static final String HEIGHT_FIELD = "height";
  private static final String BODY_TYPE_FIELD = "bodyType";

  private static final String CARS_FIELD = "cars";
  private static final String FOOD_FIELD = "food";
  private static final String HEROES_FIELD = "heroes";
  private static final String MOVIES_FIELD = "movies";
  private static final String MUSIC_FIELD = "music";
  private static final String SPORTS_FIELD = "sports";
  private static final String TURN_OFFS_FIELD = "turnOffs";
  private static final String TURN_ONS_FIELD = "turnOns";
  private static final String TV_SHOWS_FIELD = "tvShows";

  private static final String DRINKER_FIELD = "drinker";
  private static final String ETHNICITY_FIELD = "ethnicity";
  private static final String FASHION_FIELD = "fashion";
  private static final String HAPPIEST_FIELD = "happiestWhen";
  private static final String HUMOR_FIELD = "humor";
  private static final String LIVING_FIELD = "livingArrangement";
  private static final String PETS_FIELD = "pets";
  private static final String POLITICS_FIELD = "politicalViews";
  private static final String PROF_SONG = "profileSong";
  private static final String RELIGION_FIELD = "religion";
  private static final String ROMANCE_FIELD = "romance";
  private static final String SCARED_FIELD = "scaredOf";
  private static final String S_ORI_FIELD = "sexualOrientation";
  private static final String SMOKER_FIELD = "smoker";

  private static final Set<String> NON_ATOMIC = new HashSet<String>();
  private static final ConvHelper HELPER = createHelper();

  private final Node fNode;

  private final ImplUtil fImpl;

  private static ConvHelper createHelper() {
    final Map<String, List<String>> splitFields = new HashMap<String, List<String>>();

    // the body type class, integrated into a person node
    final List<String> bTypeList = new ArrayList<String>();
    bTypeList.add(GraphPerson.BUILD_FIELD);
    bTypeList.add(GraphPerson.EYE_COLOR_FIELD);
    bTypeList.add(GraphPerson.HAIR_COLOR_FIELD);
    bTypeList.add(GraphPerson.WEIGHT_FIELD);
    bTypeList.add(GraphPerson.HEIGHT_FIELD);
    splitFields.put(GraphPerson.BODY_TYPE_FIELD, bTypeList);

    // the name class, integrated into a person node
    final List<String> nameList = new ArrayList<String>();
    nameList.add(GraphPerson.ADD_NAME_FIELD);
    nameList.add(GraphPerson.FAM_NAME_FIELD);
    nameList.add(GraphPerson.FORMATTED_FIELD);
    nameList.add(GraphPerson.GIV_NAME_FIELD);
    nameList.add(GraphPerson.HON_PREF_FIELD);
    nameList.add(GraphPerson.HON_SUFF_FIELD);
    splitFields.put(GraphPerson.NAME_FIELD, nameList);

    final Set<String> relMapped = new HashSet<String>();
    relMapped.add(GraphPerson.ACCOUNTS_FIELD);
    relMapped.add(GraphPerson.ADDRESSES_FIELD);
    relMapped.add(GraphPerson.APP_DATA_FIELD);
    relMapped.add(GraphPerson.CURR_LOC_FIELD);
    relMapped.add(GraphPerson.ORGS_FIELD);

    relMapped.add(GraphPerson.EMAILS_FIELD);
    relMapped.add(GraphPerson.IMS_FIELD);
    relMapped.add(GraphPerson.PHONES_FIELD);
    relMapped.add(GraphPerson.PHOTOS_FIELD);

    GraphPerson.NON_ATOMIC.add(GraphPerson.BODY_TYPE_FIELD);
    GraphPerson.NON_ATOMIC.add(GraphPerson.NAME_FIELD);
    GraphPerson.NON_ATOMIC.addAll(relMapped);

    return new ConvHelper(splitFields, relMapped, null);
  }

  /**
   * Creates a person in the graph based on an underlying node. Throws a NullPointerException if the
   * given node is null.
   *
   * @param node
   *          node this person is based on.
   */
  public GraphPerson(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates a person in the graph based on an underlying node. Throws a NullPointerException if the
   * given node is null.
   *
   * @param node
   *          node this person is based on.
   */
  public GraphPerson(Node node, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("Underlying node was null");
    }

    this.fNode = node;

    this.fImpl = impl;
  }

  @Override
  public Map<String, Object> toMap(final Set<String> fields) {
    final Map<String, Object> dto = this.fImpl.newMap();

    if (fields == null || fields.isEmpty()) {
      copyAllProperties(dto);
    } else {
      copyProperties(dto, fields);
    }

    return dto;
  }

  private void copyProperties(final Map<String, Object> dto, Set<String> properties) {
    final Map<String, List<String>> split = GraphPerson.HELPER.getSplitFields();

    // add sub-properties to a copy of the set
    List<String> splitProps = null;
    final Set<String> newProps = new HashSet<String>(properties);

    for (final String prop : properties) {
      splitProps = split.get(prop);

      if (splitProps != null) {
        for (final String splitProp : splitProps) {
          newProps.add(splitProp);
        }
      }
    }

    properties = newProps;

    // copy dynamic age before it is filtered out
    if (properties.contains(GraphPerson.AGE_FIELD)) {
      copyAge(dto);
    }

    // this method only copies known properties based on relations
    copyRelationshipMapped(dto, properties);

    // filter the properties mapped by relationships
    final Set<String> filter = GraphPerson.HELPER.getRelationshipMapped();
    properties.removeAll(filter);

    // copy requested properties
    for (final String key : properties) {
      if (this.fNode.hasProperty(key)) {
        dto.put(key, this.fNode.getProperty(key));
      }
    }
  }

  private void copyAllProperties(final Map<String, Object> dto) {
    for (final String key : this.fNode.getPropertyKeys()) {
      dto.put(key, this.fNode.getProperty(key));
    }

    copyAllRelMapped(dto);

    copyAge(dto);
  }

  private void copyAge(final Map<String, Object> dto) {
    if (this.fNode.hasProperty(GraphPerson.BDAY_FIELD)) {
      final Object value = this.fNode.getProperty(GraphPerson.BDAY_FIELD);
      long birthTime = 0;

      // try circumventing the wrapper's faulty type casting
      // TODO: check for correctness
      if (value instanceof Long) {
        birthTime = (Long) value;
      } else {
        birthTime = (Integer) value;
      }
      final GregorianCalendar cal = new GregorianCalendar();

      // current date
      int year = cal.get(Calendar.YEAR);
      int month = cal.get(Calendar.MONTH);
      int day = cal.get(Calendar.DAY_OF_MONTH);

      // compute difference to birthday
      cal.setTimeInMillis(birthTime);
      year -= cal.get(Calendar.YEAR);
      month -= cal.get(Calendar.MONTH);
      day -= cal.get(Calendar.DAY_OF_MONTH);

      // check if already passed birthday this year
      if (month > 0 || month == 0 && day > 0) {
        --year;
      }

      dto.put(GraphPerson.AGE_FIELD, year);
    }
    // fall back if no birthday but an age is defined
    else if (this.fNode.hasProperty(GraphPerson.AGE_FIELD)) {
      dto.put(GraphPerson.AGE_FIELD, this.fNode.getProperty(GraphPerson.AGE_FIELD));
    }
  }

  private void copyAllRelMapped(final Map<String, Object> dto) {
    copyAccounts(dto);
    copyAddresses(dto);
    copyAppData(dto);
    copyLocation(dto);
    copyOrganizations(dto);

    copyEmails(dto);
    copyIms(dto);
    copyPhones(dto);
    copyPhotos(dto);
  }

  private void copyRelationshipMapped(final Map<String, Object> dto, final Set<String> properties) {
    // other entities
    if (properties.contains(GraphPerson.ACCOUNTS_FIELD)) {
      copyAccounts(dto);
    }

    if (properties.contains(GraphPerson.ADDRESSES_FIELD)) {
      copyAddresses(dto);
    }

    if (properties.contains(GraphPerson.APP_DATA_FIELD)) {
      copyAppData(dto);
    }

    if (properties.contains(GraphPerson.CURR_LOC_FIELD)) {
      copyLocation(dto);
    }

    if (properties.contains(GraphPerson.ORGS_FIELD)) {
      copyOrganizations(dto);
    }

    // list fields
    if (properties.contains(GraphPerson.EMAILS_FIELD)) {
      copyEmails(dto);
    }

    if (properties.contains(GraphPerson.IMS_FIELD)) {
      copyIms(dto);
    }

    if (properties.contains(GraphPerson.PHONES_FIELD)) {
      copyPhones(dto);
    }

    if (properties.contains(GraphPerson.PHOTOS_FIELD)) {
      copyPhotos(dto);
    }
  }

  private void copyAccounts(final Map<String, Object> dto) {
    final List<Map<String, Object>> accounts = this.fImpl.newList();

    final Iterable<Relationship> accs = this.fNode.getRelationships(ShindigRelTypes.ACCOUNT);

    SimpleGraphObject gAcc = null;
    Map<String, Object> tmpAcc = null;
    for (final Relationship rel : accs) {
      gAcc = new SimpleGraphObject(rel);
      tmpAcc = gAcc.toMap(null);
      accounts.add(tmpAcc);
    }

    if (!accounts.isEmpty()) {
      dto.put(GraphPerson.ACCOUNTS_FIELD, accounts);
    }
  }

  private void copyAddresses(final Map<String, Object> dto) {
    final List<Map<String, Object>> addresses = this.fImpl.newList();

    final Iterable<Relationship> locs = this.fNode.getRelationships(ShindigRelTypes.LOCATED_AT);

    Node add = null;
    Map<String, Object> tmpAdd = null;
    for (final Relationship rel : locs) {
      add = rel.getEndNode();
      tmpAdd = new SimpleGraphObject(add).toMap(null);
      addresses.add(tmpAdd);
    }

    if (!addresses.isEmpty()) {
      dto.put(GraphPerson.ADDRESSES_FIELD, addresses);
    }
  }

  private void copyAppData(final Map<String, Object> dto) {
    final Iterable<Relationship> dataRels = this.fNode.getRelationships(ShindigRelTypes.HAS_DATA);
    final Iterator<Relationship> dataIter = dataRels.iterator();

    // TODO: check

    if (dataIter.hasNext()) {
      final Node dataNode = dataIter.next().getEndNode();
      final Map<String, Object> appData = new HashMap<String, Object>();

      for (final String key : dataNode.getPropertyKeys()) {
        appData.put(key, dataNode.getProperty(key));
      }

      dto.put(GraphPerson.APP_DATA_FIELD, appData);
    }
  }

  private void copyLocation(final Map<String, Object> dto) {
    final Relationship locRel = this.fNode.getSingleRelationship(ShindigRelTypes.CURRENTLY_AT,
            Direction.OUTGOING);

    if (locRel != null) {
      final Node locNode = locRel.getEndNode();
      final Map<String, Object> location = new SimpleGraphObject(locNode).toMap(null);
      dto.put(GraphPerson.CURR_LOC_FIELD, location);
    }
  }

  private void copyOrganizations(final Map<String, Object> dto) {
    final List<Map<String, Object>> organizations = this.fImpl.newList();

    // having multiple organizations is not supported at the moment

    final Map<String, Object> tmpOrg = new GraphOrganization(this.fNode, this.fImpl).toMap(null);
    organizations.add(tmpOrg);

    if (!organizations.isEmpty()) {
      dto.put(GraphPerson.ORGS_FIELD, organizations);
    }
  }

  private void copyEmails(final Map<String, Object> dto) {
    final Relationship emailRel = this.fNode.getSingleRelationship(ShindigRelTypes.EMAILS,
            Direction.OUTGOING);

    if (emailRel != null) {
      final Node emailNode = emailRel.getEndNode();

      final GraphListFieldList lfl = new GraphListFieldList(emailNode, this.fImpl);
      dto.put(GraphPerson.EMAILS_FIELD, lfl.toMap(null));
    }
  }

  private void copyIms(final Map<String, Object> dto) {
    final Relationship imRel = this.fNode.getSingleRelationship(ShindigRelTypes.IMS,
            Direction.OUTGOING);

    if (imRel != null) {
      final Node imNode = imRel.getEndNode();

      final GraphListFieldList lfl = new GraphListFieldList(imNode, this.fImpl);
      dto.put(GraphPerson.IMS_FIELD, lfl.toMap(null));
    }
  }

  private void copyPhones(final Map<String, Object> dto) {
    final Relationship phoneRel = this.fNode.getSingleRelationship(ShindigRelTypes.PHONE_NUMS,
            Direction.OUTGOING);

    if (phoneRel != null) {
      final Node phoneNode = phoneRel.getEndNode();

      final GraphListFieldList lfl = new GraphListFieldList(phoneNode, this.fImpl);
      dto.put(GraphPerson.PHONES_FIELD, lfl.toMap(null));
    }
  }

  private void copyPhotos(final Map<String, Object> dto) {
    final Relationship photoRel = this.fNode.getSingleRelationship(ShindigRelTypes.PHOTOS,
            Direction.OUTGOING);

    if (photoRel != null) {
      final Node photoNode = photoRel.getEndNode();

      final GraphListFieldList lfl = new GraphListFieldList(photoNode, this.fImpl);
      dto.put(GraphPerson.PHOTOS_FIELD, lfl.toMap(null));
    }
  }

  /**
   * Sets the properties of this person in the graph using information from the given person object.
   * Does not handle transactions. Throws a NullPointerException if the given person is null.
   *
   * @param person
   *          person object containing data to store
   */
  @Override
  public void setData(Map<String, ?> person) {
    // collect new values
    final Map<String, Object> newValues = new HashMap<String, Object>();

    readAtomic(person, newValues);

    readNonAtomic(person, newValues);

    readLists(person, newValues);

    // TODO: wrap up into generic property setting loop

    // set new values
    for (final Entry<String, Object> valE : newValues.entrySet()) {
      if (valE.getValue() != null) {
        this.fNode.setProperty(valE.getKey(), valE.getValue());
      }
    }
  }

  private void readAtomic(final Map<String, ?> person, final Map<String, Object> newValues) {
    newValues.put(GraphPerson.ABOUT_ME_FIELD, person.get(GraphPerson.ABOUT_ME_FIELD));
    newValues.put(GraphPerson.AGE_FIELD, person.get(GraphPerson.AGE_FIELD));
    newValues.put(GraphPerson.CHILDREN_FIELD, person.get(GraphPerson.CHILDREN_FIELD));
    newValues.put(GraphPerson.DISP_NAME_FIELD, person.get(GraphPerson.DISP_NAME_FIELD));
    newValues.put(GraphPerson.ID_FIELD, person.get(GraphPerson.ID_FIELD));
    newValues.put(GraphPerson.JOB_INTS_FIELD, person.get(GraphPerson.JOB_INTS_FIELD));
    newValues.put(GraphPerson.NICKNAME_FIELD, person.get(GraphPerson.NICKNAME_FIELD));
    newValues.put(GraphPerson.PREF_USRNAME_FIELD, person.get(GraphPerson.PREF_USRNAME_FIELD));
    newValues.put(GraphPerson.PROFILE_URL_FIELD, person.get(GraphPerson.PROFILE_URL_FIELD));
    newValues.put(GraphPerson.REL_STAT_FIELD, person.get(GraphPerson.REL_STAT_FIELD));
    newValues.put(GraphPerson.STATUS_FIELD, person.get(GraphPerson.STATUS_FIELD));
    newValues.put(GraphPerson.THUMBNAIL_FIELD, person.get(GraphPerson.THUMBNAIL_FIELD));
    newValues.put(GraphPerson.OFFSET_FIELD, person.get(GraphPerson.OFFSET_FIELD));

    newValues.put(GraphPerson.DRINKER_FIELD, person.get(GraphPerson.DRINKER_FIELD));
    newValues.put(GraphPerson.ETHNICITY_FIELD, person.get(GraphPerson.ETHNICITY_FIELD));
    newValues.put(GraphPerson.FASHION_FIELD, person.get(GraphPerson.FASHION_FIELD));
    newValues.put(GraphPerson.HAPPIEST_FIELD, person.get(GraphPerson.HAPPIEST_FIELD));
    newValues.put(GraphPerson.HUMOR_FIELD, person.get(GraphPerson.HUMOR_FIELD));
    newValues.put(GraphPerson.LIVING_FIELD, person.get(GraphPerson.LIVING_FIELD));
    newValues.put(GraphPerson.PETS_FIELD, person.get(GraphPerson.PETS_FIELD));
    newValues.put(GraphPerson.POLITICS_FIELD, person.get(GraphPerson.POLITICS_FIELD));
    newValues.put(GraphPerson.PROF_SONG, person.get(GraphPerson.PROF_SONG));
    newValues.put(GraphPerson.RELIGION_FIELD, person.get(GraphPerson.RELIGION_FIELD));
    newValues.put(GraphPerson.ROMANCE_FIELD, person.get(GraphPerson.ROMANCE_FIELD));
    newValues.put(GraphPerson.SCARED_FIELD, person.get(GraphPerson.SCARED_FIELD));
    newValues.put(GraphPerson.S_ORI_FIELD, person.get(GraphPerson.S_ORI_FIELD));
    newValues.put(GraphPerson.SMOKER_FIELD, person.get(GraphPerson.SMOKER_FIELD));
  }

  private void readNonAtomic(final Map<String, ?> person, final Map<String, Object> newValues) {
    final Object bDay = person.get(GraphPerson.BDAY_FIELD);
    if (bDay != null) {
      newValues.put(GraphPerson.BDAY_FIELD, bDay);
    }

    final Object gender = person.get(GraphPerson.GENDER_FIELD);
    if (gender != null) {
      newValues.put(GraphPerson.GENDER_FIELD, gender);
    }

    final Object updated = person.get(GraphPerson.UPDATED_FIELD);
    if (updated != null) {
      newValues.put(GraphPerson.UPDATED_FIELD, updated);
    }

    final Object nPres = person.get(GraphPerson.NET_PRES_FIELD);
    if (nPres != null) {
      newValues.put(GraphPerson.NET_PRES_FIELD, nPres);
    }

    final Object profVid = person.get(GraphPerson.PROF_VID_FIELD);
    if (profVid != null) {
      newValues.put(GraphPerson.PROF_VID_FIELD, profVid);
    }

    newValues.put(GraphPerson.ADD_NAME_FIELD, person.get(GraphPerson.ADD_NAME_FIELD));
    newValues.put(GraphPerson.FAM_NAME_FIELD, person.get(GraphPerson.FAM_NAME_FIELD));
    newValues.put(GraphPerson.FORMATTED_FIELD, person.get(GraphPerson.FORMATTED_FIELD));
    newValues.put(GraphPerson.GIV_NAME_FIELD, person.get(GraphPerson.GIV_NAME_FIELD));
    newValues.put(GraphPerson.HON_PREF_FIELD, person.get(GraphPerson.HON_PREF_FIELD));
    newValues.put(GraphPerson.HON_SUFF_FIELD, person.get(GraphPerson.HON_SUFF_FIELD));

    newValues.put(GraphPerson.BUILD_FIELD, person.get(GraphPerson.BUILD_FIELD));
    newValues.put(GraphPerson.EYE_COLOR_FIELD, person.get(GraphPerson.EYE_COLOR_FIELD));
    newValues.put(GraphPerson.HAIR_COLOR_FIELD, person.get(GraphPerson.HAIR_COLOR_FIELD));
    newValues.put(GraphPerson.WEIGHT_FIELD, person.get(GraphPerson.WEIGHT_FIELD));
    newValues.put(GraphPerson.HEIGHT_FIELD, person.get(GraphPerson.HEIGHT_FIELD));
  }

  private void readLists(final Map<String, ?> person, final Map<String, Object> newValues) {
    updateList(person, GraphPerson.ACTIVITIES_FIELD, newValues);
    updateList(person, GraphPerson.BOOKS_FIELD, newValues);
    updateList(person, GraphPerson.INTERESTS_FIELD, newValues);
    updateList(person, GraphPerson.LANGUAGES_FIELD, newValues);
    updateList(person, GraphPerson.LOOKING_FIELD, newValues);
    updateList(person, GraphPerson.QUOTES_FIELD, newValues);
    updateList(person, GraphPerson.TAGS_FIELD, newValues);
    updateList(person, GraphPerson.URLS_FIELD, newValues);

    updateList(person, GraphPerson.CARS_FIELD, newValues);
    updateList(person, GraphPerson.FOOD_FIELD, newValues);
    updateList(person, GraphPerson.HEROES_FIELD, newValues);
    updateList(person, GraphPerson.MOVIES_FIELD, newValues);
    updateList(person, GraphPerson.MUSIC_FIELD, newValues);
    updateList(person, GraphPerson.SPORTS_FIELD, newValues);
    updateList(person, GraphPerson.TURN_OFFS_FIELD, newValues);
    updateList(person, GraphPerson.TURN_ONS_FIELD, newValues);
    updateList(person, GraphPerson.TV_SHOWS_FIELD, newValues);
  }

  @SuppressWarnings("unchecked")
  private void updateList(final Map<String, ?> person, final String field,
          final Map<String, Object> newValues) {
    final Object value = person.get(field);

    if (value == null) {
      return;
    }

    String[] listEntries = null;

    if (value instanceof String[]) {
      listEntries = (String[]) value;
    } else {
      final List<String> entryList = (List<String>) value;
      listEntries = entryList.toArray(new String[entryList.size()]);
    }

    if (listEntries.length > 0) {
      newValues.put(field, listEntries);
    } else {
      this.fNode.removeProperty(field);
    }
  }

  /**
   * Tries to determine the difference between currently set relation-based fields and the
   * properties of the given person object. Throws a NullPointerException if the given person is
   * null.
   *
   * @param person
   *          person object containing data to store
   */
  @SuppressWarnings("unchecked")
  public void updateRelationships(final Map<String, Object> person) {
    final List<Map<String, Object>> addresses = (List<Map<String, Object>>) person
            .get(GraphPerson.ADDRESSES_FIELD);
    if (addresses != null) {
      updateAddresses(addresses);
    }

    final Map<String, ?> appData = (Map<String, ?>) person.get(GraphPerson.APP_DATA_FIELD);
    if (appData != null) {
      updateAppData(appData);
    }

    final Map<String, Object> location = (Map<String, Object>) person
            .get(GraphPerson.CURR_LOC_FIELD);
    if (location != null) {
      updateLocation(location);
    }

    final List<Map<String, Object>> organizations = (List<Map<String, Object>>) person
            .get(GraphPerson.ORGS_FIELD);
    if (organizations != null) {
      updateOrganizations(organizations);
    }

    final Map<String, Object> emails = (Map<String, Object>) person.get(GraphPerson.EMAILS_FIELD);
    if (emails != null) {
      updateListField(emails, ShindigRelTypes.EMAILS);
    }

    final Map<String, Object> ims = (Map<String, Object>) person.get(GraphPerson.IMS_FIELD);
    if (ims != null) {
      updateListField(emails, ShindigRelTypes.IMS);
    }

    final Map<String, Object> phones = (Map<String, Object>) person.get(GraphPerson.PHONES_FIELD);
    if (phones != null) {
      updateListField(phones, ShindigRelTypes.PHONE_NUMS);
    }

    final Map<String, Object> photos = (Map<String, Object>) person.get(GraphPerson.PHOTOS_FIELD);
    if (photos != null) {
      updateListField(photos, ShindigRelTypes.PHOTOS);
    }
  }

  private void updateAddresses(List<Map<String, Object>> addresses) {
    // TODO: maybe actually only update differences (requires full scan)

    Node addNode = null;
    for (final Relationship rel : this.fNode.getRelationships(ShindigRelTypes.LOCATED_AT)) {
      addNode = rel.getEndNode();
      rel.delete();

      // delete unused addresses
      if (!addNode.hasRelationship()) {
        addNode.delete();
      }
    }

    final GraphDatabaseService db = this.fNode.getGraphDatabase();
    for (final Map<String, Object> add : addresses) {
      addNode = db.createNode();
      this.fNode.createRelationshipTo(addNode, ShindigRelTypes.LOCATED_AT);

      // store attributes
      new SimpleGraphObject(addNode, this.fImpl).setData(add);
    }
  }

  private void updateAppData(Map<String, ?> appData) {
    // TODO: internal or for which application?
  }

  private void updateLocation(Map<String, Object> location) {
    Node locNode = null;
    final Relationship rel = this.fNode.getSingleRelationship(ShindigRelTypes.CURRENTLY_AT,
            Direction.OUTGOING);

    if (rel != null) {
      locNode = rel.getEndNode();
      rel.delete();

      // delete location if unused
      if (!locNode.hasRelationship()) {
        locNode.delete();
      }
    }

    locNode = this.fNode.getGraphDatabase().createNode();
    this.fNode.createRelationshipTo(locNode, ShindigRelTypes.CURRENTLY_AT);

    // store attributes
    new SimpleGraphObject(locNode, this.fImpl).setData(location);
  }

  private void updateOrganizations(List<Map<String, Object>> organizations) {
    // storing multiple organizations is not possible at the moment

    for (final Map<String, Object> org : organizations) {
      // store attributes
      new GraphOrganization(this.fNode, this.fImpl).setData(org);
      // only one organization is supported at the moment
      break;
    }
  }

  private void updateListField(Map<String, Object> list, ShindigRelTypes type) {
    Node listNode = null;
    final Relationship listRel = this.fNode.getSingleRelationship(type, Direction.OUTGOING);

    if (listRel != null) {
      listNode = listRel.getEndNode();
    } else {
      listNode = this.fNode.getGraphDatabase().createNode();
      this.fNode.createRelationshipTo(listNode, type);
    }

    /*
     * it is probably not useful to determine the difference as the whole array has to be stored
     * again
     */
    new GraphListFieldList(listNode, this.fImpl).setData(list);
  }
}
