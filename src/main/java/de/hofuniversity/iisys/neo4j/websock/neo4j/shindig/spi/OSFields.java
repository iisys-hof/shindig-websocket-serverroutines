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

/**
 * Collection of general OpenSocial fields. Could also partially be replaced by direct dependencies
 * to Apache Shindig.
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
  public static final String OBJECT_TYPE = "objectType";
  public static final String ACT_PUBLISHED_FIELD = "published";
}
