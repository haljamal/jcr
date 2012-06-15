/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.StringValueData;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: StringPersistedValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class StringPersistedValueData extends StringValueData implements PersistedValueData
{
   /**
    * Empty constructor for serialization.
    */
   public StringPersistedValueData()
   {
      super(0, "");
   }

   /**
    * StringPersistedValueData constructor.
    */
   public StringPersistedValueData(int orderNumber, String value)
   {
      super(orderNumber, value);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      orderNumber = in.readInt();

      byte[] data = new byte[in.readInt()];
      if (data.length > 0)
      {
         in.readFully(data);
         value = new String(data, Constants.DEFAULT_ENCODING);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(orderNumber);

      byte[] data = value.getBytes();
      out.writeInt(data.length);
      out.write(data);
   }
}
