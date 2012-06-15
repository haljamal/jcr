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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.PersistedValueData;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ByteArrayValueData.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class ByteArrayValueData extends AbstractValueData
{

   /**
    * The value.
    */
   protected byte[] value;

   /**
    * ByteArrayValueData constructor.
    */
   protected ByteArrayValueData(int orderNumber, byte[] value)
   {
      super(orderNumber);
      this.value = value;
   }

   /**
    * {@inheritDoc}
    */
   protected boolean internalEquals(ValueData another)
   {
      if (another instanceof ByteArrayValueData)
      {
         return Arrays.equals(((ByteArrayValueData)another).value, value);
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   protected byte[] spoolInternalValue()
   {
      return value;
   }

   /**
    * {@inheritDoc}
    */
   public PersistedValueData createPersistedCopy(int orderNumber) throws IOException
   {
      return new ByteArrayPersistedValueData(orderNumber, value);
   }

   /**
    * {@inheritDoc}
    */
   public TransientValueData createTransientCopy(int orderNumber) throws IOException
   {
      return new TransientValueData(orderNumber, value);
   }

}
