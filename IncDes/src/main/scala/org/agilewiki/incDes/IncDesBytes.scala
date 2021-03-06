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
import bind._

class IncDesBytesFactory(id: FactoryId)
  extends IncDesFactory(id) {
  override protected def instantiate = new IncDesBytes
}

object IncDesBytes {
  def apply(mailbox: Mailbox) = {
    new IncDesBytesFactory(INC_DES_BYTES_FACTORY_ID).newActor(mailbox).asInstanceOf[IncDesBytes]
  }
}

class IncDesBytes extends IncDesItem[Array[Byte]] {
  private var i: Array[Byte] = null
  private var len = -1

  override def value(msg: AnyRef, rf: Any => Unit) {
    if (dser) {
      rf(i)
      return
    }
    if (!isSerialized) throw new IllegalStateException
    if (len == -1) i = null
    else {
      val m = data.mutable
      m.skip(intLength)
      i = m.readBytes(len)
    }
    dser = true
    rf(i)
  }

  override def set(msg: AnyRef, rf: Any => Unit) {
    val s = msg.asInstanceOf[Set[Array[Byte]]]
    val v = s.value
    val tc = s.transactionContext
    writable(tc) {
      rsp1 => {
        val ol = length
        i = v
        dser = true
        change(tc, length - ol, this, rf)
      }
    }
  }

  override def length = {
    if (dser)
      if (i == null) intLength
      else intLength + i.length
    else if (len == -1) intLength
    else intLength + len
  }

  override protected def serialize(_data: MutableData) {
    if (!dser) throw new IllegalStateException
    if (i == null) _data.writeInt(-1)
    else {
      _data.writeInt(i.length)
      _data.writeBytes(i)
    }
  }

  override def load(_data: MutableData) {
    super.load(_data)
    len = _data.readInt
    if (len > 0) _data.skip(len)
    dser = false
  }
}
