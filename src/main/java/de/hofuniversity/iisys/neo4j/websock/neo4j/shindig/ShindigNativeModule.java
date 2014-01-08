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

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import de.hofuniversity.iisys.neo4j.websock.procedures.IProcedureProvider;

/**
 * Guice module binding the Shindig native procedure loader.
 */
public class ShindigNativeModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(IProcedureProvider.class).annotatedWith(Names.named("shindigNative")).to(
            ShindigNativeProcedures.class);
  }
}
