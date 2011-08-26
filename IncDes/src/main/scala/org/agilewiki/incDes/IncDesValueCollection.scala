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
import services._
import seq._

class IncDesValueCollectionFactory(id: FactoryId, valueId: FactoryId)
  extends IncDesFactory(id) {
  var valueFactory: IncDesFactory = null

  override def configure(systemServices: Actor, factoryRegistryComponentFactory: FactoryRegistryComponentFactory) {
    valueFactory = factoryRegistryComponentFactory.getFactory(valueId).asInstanceOf[IncDesFactory]
  }
}

abstract class IncDesValueCollection[K, V <: IncDesItem[V1], V1]
  extends IncDesCollection[K] {

  bind(classOf[Get[V]], get)
  bind(classOf[GetValue[K]], getValue)
  bind(classOf[ValuesSeq], valuesSeq)
  bind(classOf[FlatValuesSeq], flatValuesSeq)

  def get(msg: AnyRef, rf: Any => Unit)

  def getValue(msg: AnyRef, rf: Any => Unit)

  def valueFactory = factory.asInstanceOf[IncDesValueCollectionFactory].valueFactory

  def newValue = {
    val v = valueFactory.newActor(mailbox).asInstanceOf[V]
    v.setMailbox(mailbox)
    v.setSystemServices(systemServices)
    v
  }

  def preprocess(tc: TransactionContext, v: V) {
    if (v == null) throw new IllegalArgumentException("may not be null")
    if (v.container != null) throw new IllegalArgumentException("already in use")
    val vfactory = v.factory
    if (vfactory == null) throw new IllegalArgumentException("factory is null")
    val fid = vfactory.id
    if (fid == null) throw new IllegalArgumentException("factory id is null")
    if (fid.value != valueFactory.id.value)
      throw new IllegalArgumentException("incorrect factory id: " + fid.value)
    if (mailbox != v.mailbox) {
      if (v.mailbox == null && !v.opened) v.setMailbox(mailbox)
      else throw new IllegalStateException("uses a different mailbox")
    }
    if (v.systemServices == null) v.setSystemServices(systemServices)
  }

  def valuesSeq(msg: AnyRef, rf: Any => Unit) {
    seq(Seq(), {
      r1 => {
        val s = r1.asInstanceOf[Sequence[K, V]]
        val m = new MapSafeSeq[K, V, V1](s, new MapValueSafe[K, V, V1])
        rf(m)
      }
    })
  }

  def flatValuesSeq(msg: AnyRef, rf: Any => Unit) {
    seq(Seq(), {
      r1 => {
        val s = r1.asInstanceOf[Sequence[K, V]]
        val m = new FlatmapSafeSeq[K, V, V1](s, new MapValueSafe[K, V, V1])
        rf(m)
      }
    })
  }
}

class MapValueSafe[K, V <: IncDesItem[V1], V1]
  extends Safe {
  override def func(target: Actor, msg: AnyRef, rf: Any => Unit)(implicit sender: ActiveActor) {
    val item = msg.asInstanceOf[KVPair[K, V]].value
    item(Value()) {
      r1 => {
        val v = r1.asInstanceOf[V1]
        rf(v)
      }
    }
  }
}
