/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: SybaseSingleDbJDBCConnection 34360 2009-07-22 23:58:59Z tolusha $
 */
public class SybaseSingleDbJDBCConnection extends SingleDbJDBCConnection
{

   /**
    * SybaseSingleDbJDBCConnection constructor.
    */
   public SybaseSingleDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected final void prepareQueries() throws SQLException
   {
      super.prepareQueries();

      FIND_WORKSPACE_DATA_SIZE =
         "select sum(datalength(DATA)) from JCR_SITEM I, JCR_SVALUE V where I.I_CLASS=2 and I.CONTAINER_NAME=?"
            + " and I.ID=V.PROPERTY_ID";

      FIND_NODE_DATA_SIZE =
         "select sum(datalength(DATA)) from JCR_SITEM I, JCR_SVALUE V where I.PARENT_ID=? and I.I_CLASS=2"
            + " and I.CONTAINER_NAME=? and I.ID=V.PROPERTY_ID";

      FIND_VALUE_STORAGE_DESC_AND_SIZE = "select datalength(DATA), STORAGE_DESC from JCR_SVALUE where PROPERTY_ID=?";
   }
}
