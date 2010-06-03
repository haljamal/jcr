/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.init;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JCR Storage DB initializer.
 * 
 * Created by The eXo Platform SAS 12.03.2007. 
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class StorageDBInitializer extends DBInitializer
{

   protected final boolean multiDb;

   public StorageDBInitializer(String containerName, Connection connection, String scriptPath, boolean multiDb)
      throws IOException
   {
      super(containerName, connection, scriptPath);
      this.multiDb = multiDb;
   }

   /**
    * Init root node parent record.
    */
   protected void postInit(Connection connection) throws SQLException
   {
      final String MDB = (multiDb ? "M" : "S");
      String select =
         "select * from JCR_" + MDB + "ITEM where ID='" + Constants.ROOT_PARENT_UUID + "' and PARENT_ID='"
            + Constants.ROOT_PARENT_UUID + "'";

      Statement st = connection.createStatement();
      if (!st.executeQuery(select).next())
      {
         String insert =
            "insert into JCR_" + MDB + "ITEM(ID, PARENT_ID, NAME, " + (multiDb ? "" : "CONTAINER_NAME, ")
               + "VERSION, I_CLASS, I_INDEX, N_ORDER_NUM)" + " VALUES('" + Constants.ROOT_PARENT_UUID + "', '"
               + Constants.ROOT_PARENT_UUID + "', '__root_parent', " + (multiDb ? "" : "'__root_parent_container', ")
               + "0, 0, 0, 0)";

         st.executeUpdate(insert);
      }
      st.close();
   }
}
