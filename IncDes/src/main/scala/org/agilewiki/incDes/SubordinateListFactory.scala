/*
 * Copyright 2011 Bill La Forge
 *
 * This file is part of AgileWiki and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (LGPL) as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * or navigate to the following url http://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Note however that only Scala, Java and JavaScript files are being covered by LGPL.
 * All other files are covered by the Common Public License (CPL).
 * A copy of this license is also included and can be
 * found as well at http://www.opensource.org/licenses/cpl1.0.txt
 */
package org.agilewiki
package incDes

import blip._

object SubordinateIntListFactory
  extends SubordinateListFactory[IncDesInt](INC_DES_INT_LIST_FACTORY_ID, INC_DES_INT_FACTORY_ID)

object IncDesIntList {
  def apply(mailbox: Mailbox) = {
    SubordinateIntListFactory.newActor(mailbox).asInstanceOf[IncDesList[IncDesInt]]
  }
}

object SubordinateLongListFactory
  extends SubordinateListFactory[IncDesLong](INC_DES_LONG_LIST_FACTORY_ID, INC_DES_LONG_FACTORY_ID)

object IncDesLongList {
  def apply(mailbox: Mailbox) = {
    SubordinateLongListFactory.newActor(mailbox).asInstanceOf[IncDesList[IncDesLong]]
  }
}

object SubordinateStringListFactory
  extends SubordinateListFactory[IncDesString](INC_DES_STRING_LIST_FACTORY_ID, INC_DES_STRING_FACTORY_ID)

object IncDesStringList {
  def apply(mailbox: Mailbox) = {
    SubordinateStringListFactory.newActor(mailbox).asInstanceOf[IncDesList[IncDesString]]
  }
}

object SubordinateBooleanListFactory
  extends SubordinateListFactory[IncDesBoolean](INC_DES_BOOLEAN_LIST_FACTORY_ID, INC_DES_BOOLEAN_FACTORY_ID)

object IncDesBooleanList {
  def apply(mailbox: Mailbox) = {
    SubordinateBooleanListFactory.newActor(mailbox).asInstanceOf[IncDesList[IncDesBoolean]]
  }
}

object SubordinateBytesListFactory
  extends SubordinateListFactory[IncDesBytes](INC_DES_BYTES_LIST_FACTORY_ID, INC_DES_BYTES_FACTORY_ID)

object IncDesBytesList {
  def apply(mailbox: Mailbox) = {
    SubordinateBytesListFactory.newActor(mailbox).asInstanceOf[IncDesList[IncDesBytes]]
  }
}

object SubordinateIncDesListFactory
  extends SubordinateListFactory[IncDesIncDes](INC_DES_INCDES_LIST_FACTORY_ID, INC_DES_INCDES_FACTORY_ID)

object IncDesIncDesList {
  def apply(mailbox: Mailbox) = {
    SubordinateIncDesListFactory.newActor(mailbox).asInstanceOf[IncDesList[IncDesIncDes]]
  }
}

class SubordinateListFactory[V <: IncDes](id: FactoryId, subId: FactoryId)
  extends SubordinateCollectionFactory(id, subId) {
  override protected def instantiate = new IncDesList[V]
}