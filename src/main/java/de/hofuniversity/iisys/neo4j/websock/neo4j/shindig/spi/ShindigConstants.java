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
 * Collection of constants related to Apache Shindig routines and their configuration.
 */
public class ShindigConstants {
  // configuration
  public static final String ACT_OBJ_DEDUP_PROP = "activityobjects.deduplicate";
  public static final String ACT_OBJ_UPDATE_PROP = "activityobjects.update";

  // indices
  public static final String PERSON_NODES = "persons";
  public static final String APP_NODES = "applications";
  public static final String ACTIVITY_ENTRY_NODES = "activityentry";
  public static final String MESSAGE_NODES = "messages";
  public static final String GROUP_NODES = "groups";
  public static final String ID_NODE = "id";

  // other
  public static final String PERSON_TYPE = "person";
  public static final String ACT_OBJ_TYPE_SUFF = "_activityobject";
  public static final String MESSAGE_COLLECTION_NODES = "message_collections";
  public static final String ALBUM_NODES = "albums";
  public static final String MEDIA_ITEM_NODES = "mediaItems";
}
