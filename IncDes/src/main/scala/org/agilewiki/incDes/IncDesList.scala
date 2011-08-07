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
import seq._
import java.util.ArrayList

class SubordinateListFactory[V <: IncDes](id: FactoryId, subId: FactoryId)
  extends SubordinateCollectionFactory(id, subId) {
  override protected def instantiate = new IncDesList[V]
}

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

class IncDesList[V <: IncDes] extends IncDesCollection[Int, V] {
  private var i = new ArrayList[IncDes]
  private var len = 0

  bind(classOf[Add[V]], add)
  bind(classOf[Insert[V]], insert)

  override def isDeserialized = i != null

  override def load(_data: MutableData) {
    super.load(_data)
    len = _data.readInt
    if (len > 0) _data.skip(len)
    i = null
  }

  override protected def serialize(_data: MutableData) {
    if (i == null) throw new IllegalStateException
    else {
      _data.writeInt(len)
      val it = i.iterator
      while (it.hasNext) {
        val j = it.next
        j.save(_data)
      }
    }
  }

  override def length = intLength + len

  def deserialize {
    if (i != null) return
    i = new ArrayList[IncDes]
    val m = data.mutable
    m.skip(intLength)
    val limit = m.offset + len
    while (m.offset < limit) {
      val sub = newSubordinate
      sub.load(m)
      sub.partness(this, i.size - 1, this)
      i.add(sub)
    }
  }

  override def change(transactionContext: TransactionContext, lenDiff: Int, what: IncDes, rf: Any => Unit) {
    len += lenDiff
    changed(transactionContext, lenDiff, what, rf)
  }

  def preprocess(tc: TransactionContext, v: V) {
    if (v == null) throw new IllegalArgumentException("may not be null")
    if (v.container != null) throw new IllegalArgumentException("already in use")
    val vfactory = v.factory
    if (vfactory == null) throw new IllegalArgumentException("factory is null")
    val fid = vfactory.id
    if (fid == null) throw new IllegalArgumentException("factory id is null")
    if (fid.value != subFactory.id.value)
      throw new IllegalArgumentException("incorrect factory id: " + fid.value)
    if (mailbox != v.mailbox) {
      if (v.mailbox == null && !v.opened) v.setMailbox(mailbox)
      else throw new IllegalStateException("uses a different mailbox")
    }
    if (v.systemServices == null && !v.opened) v.setSystemServices(systemServices)
    deserialize
  }

  def add(msg: AnyRef, rf: Any => Unit) {
    val s = msg.asInstanceOf[Add[V]]
    val tc = s.transactionContext
    val v = s.value
    preprocess(tc, v)
    this(Writable(tc)) {
      rsp => {
        i.add(v)
        change(tc, v.length, this, rf)
      }
    }
  }

  def insert(msg: AnyRef, rf: Any => Unit) {
    val s = msg.asInstanceOf[Insert[V]]
    val tc = s.transactionContext
    val v = s.value
    preprocess(tc, v)
    val index = s.index
    if (index < 0 || index > i.size) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+i.size)
    this(Writable(tc)) {
      rsp => {
        i.add(index, v)
        change(tc, v.length, this, rf)
      }
    }
  }

  def get(msg: AnyRef, rf: Any => Unit) {
    deserialize
    val key = msg.asInstanceOf[Get[Int]].key
    if (key < 0 || key >= i.size) rf(null)
    else rf(i.get(key))
  }

  def containsKey(msg: AnyRef, rf: Any => Unit) {
    deserialize
    val key = msg.asInstanceOf[ContainsKey[Int]].key
    rf(key >= 0 && key < i.size)
  }

  def size(msg: AnyRef, rf: Any => Unit) {
    deserialize
    rf(i.size)
  }

  def remove(msg: AnyRef, rf: Any => Unit) {
    deserialize
    val s = msg.asInstanceOf[Remove[Int]]
    val key = s.key
    if (key < 0 || key >= i.size) {
      rf(null)
      return
    }
    val tc = s.transactionContext
    this(Writable(tc)) {
      rsp => {
        val r = i.remove(key)
        val l = r.length
        r.clearContainer
        change(tc, -l, this, {
          rsp => rf(r)
        })
      }
    }
  }

  def seq(msg: AnyRef, rf: Any => Unit) {
    deserialize
    rf(new ListSeq[IncDes](i))
  }
}