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
package org.exoplatform.services.jcr.impl.core.value;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * A <code>LongValue</code> provides an implementation of the <code>Value</code> interface
 * representing a long value.
 * 
 * @author Gennady Azarenkov
 */
public class LongValue extends BaseValue
{

   public static final int TYPE = PropertyType.LONG;

   /**
    * LongValue constructor.
    */
   public LongValue(long l) throws IOException
   {
      super(TYPE, new TransientValueData(l));
   }

   /**
    * LongValue constructor.
    */
   LongValue(ValueData data) throws IOException
   {
      super(TYPE, data);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      Long longNumber = ValueDataUtil.getLong(getInternalData());

      if (longNumber != null)
      {
         // loosing timezone information...
         Calendar cal = Calendar.getInstance();
         cal.setTime(new Date(longNumber.longValue()));
         return cal;
      }
      else
      {
         throw new ValueFormatException("empty value");
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      throw new ValueFormatException("conversion to boolean failed: inconvertible types");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
   {
      validateByteArrayMethodInvoking();

      Long longNumber = ValueDataUtil.getLong(getInternalData());

      if (longNumber != null)
      {
         return longNumber.doubleValue();
      }
      else
      {
         throw new ValueFormatException("empty value");
      }
   }
}
