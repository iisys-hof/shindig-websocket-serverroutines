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

/**
 * Collection of general OpenSocial fields and other constants. Could also partially be replaced by
 * direct dependencies to Apache Shindig.
 */
public class OSFields {
  public static final String INBOX_NAME = "inbox";

  public static final String ANONYMOUS_NAME = "Anonymous";

  public static final String APP_ID = "appId";

  // group
  public static final String GROUP_TYPE_SELF = "@self";
  public static final String GROUP_TYPE_ALL = "@all";
  public static final String GROUP_TYPE_FRIENDS = "@friends";

  /**
   * Field outside of specification for retrieving a list of all open friend requests.
   */
  public static final String FRIENDREQUESTS = "@friendrequests";

  // properties
  public static final String ID_FIELD = "id";
  public static final String NAME_FIELD = "name";
  public static final String FORMATTED_FIELD = "formatted";

  public static final String ACTOR_FIELD = "actor";
  public static final String OBJECT_FIELD = "object";
  public static final String TARGET_FIELD = "target";
  public static final String GENERATOR_FIELD = "generator";
  public static final String ACT_PUBLISHED_FIELD = "published";
  public static final String TITLE_FIELD = "title";
  public static final String VERB_FIELD = "verb";

  public static final String OBJECT_TYPE = "objectType";
  public static final String DISP_NAME_FIELD = "displayName";
  public static final String CONTENT_FIELD = "content";

  // other constants
  public static final String SHINDIG_ID = "shindig";
  public static final String SHINDIG_NAME = "Apache Shindig";
  public static final String APPLICATION_TYPE = "application";
  public static final String PERSON_TYPE = "person";

  public static final String TIME_ZONE = "UTC";
  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
}
