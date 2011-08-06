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
package incDesList

import org.specs.SpecificationWithJUnit
import blip._
import seq._

class ListTest extends SpecificationWithJUnit {
  "ListTest" should {
    "Serialize/deserialize" in {
      val systemServices = SystemServices(new IncDesComponentFactory)

      val booll0 = IncDesBooleanList(new Mailbox)
      booll0.setSystemServices(systemServices)
      val bool0 = IncDesBoolean(null)
      Future(booll0, Add(null, bool0))
      Future(booll0, Length()) must be equalTo (5)
      val boolb0 = Future(booll0, Bytes()).asInstanceOf[Array[Byte]]

      val booll1 = IncDesBooleanList(new Mailbox)
      booll1.setSystemServices(systemServices)
      booll1.load(boolb0)
      Future(booll1, Length()) must be equalTo (5)
      val bool1 = Future(booll1, Get(0))
      bool1.isInstanceOf[IncDesBoolean] must beTrue

      val bytesl0 = IncDesBytesList(new Mailbox)
      bytesl0.setSystemServices(systemServices)
      val bytes0 = IncDesBytes(null)
      Future(bytesl0, Add(null, bytes0))
      Future(bytesl0, Length()) must be equalTo (8)
      val bytesb0 = Future(bytesl0, Bytes()).asInstanceOf[Array[Byte]]

      val bytesl1 = IncDesBytesList(new Mailbox)
      bytesl1.setSystemServices(systemServices)
      bytesl1.load(bytesb0)
      Future(bytesl1, Length()) must be equalTo (8)
      val bytes1 = Future(bytesl1, Get(0))
      bytes1.isInstanceOf[IncDesBytes] must beTrue

      val intl0 = IncDesIntList(new Mailbox)
      intl0.setSystemServices(systemServices)
      val int0 = IncDesInt(null)
      Future(intl0, Add(null, int0))
      Future(intl0, Length()) must be equalTo (8)
      val intb0 = Future(intl0, Bytes()).asInstanceOf[Array[Byte]]

      val intl1 = IncDesIntList(new Mailbox)
      intl1.setSystemServices(systemServices)
      intl1.load(intb0)
      Future(intl1, Length()) must be equalTo (8)
      val int1 = Future(intl1, Get(0))
      int1.isInstanceOf[IncDesInt] must beTrue

      val longl0 = IncDesLongList(new Mailbox)
      longl0.setSystemServices(systemServices)
      val long0 = IncDesLong(null)
      Future(longl0, Add(null, long0))
      Future(longl0, Length()) must be equalTo (12)
      val longb0 = Future(longl0, Bytes()).asInstanceOf[Array[Byte]]

      val longl1 = IncDesLongList(new Mailbox)
      longl1.setSystemServices(systemServices)
      longl1.load(longb0)
      Future(longl1, Length()) must be equalTo (12)
      val long1 = Future(longl1, Get(0))
      long1.isInstanceOf[IncDesLong] must beTrue

      val strl0 = IncDesStringList(new Mailbox)
      strl0.setSystemServices(systemServices)
      val str0 = IncDesString(null)
      Future(strl0, Add(null, str0))
      Future(strl0, Length()) must be equalTo (8)
      val strb0 = Future(strl0, Bytes()).asInstanceOf[Array[Byte]]

      val strl1 = IncDesStringList(new Mailbox)
      strl1.setSystemServices(systemServices)
      strl1.load(strb0)
      Future(strl1, Length()) must be equalTo (8)
      val str1 = Future(strl1, Get(0))
      str1.isInstanceOf[IncDesString] must beTrue

      val incdesl0 = IncDesIncDesList(new Mailbox)
      incdesl0.setSystemServices(systemServices)
      val incdes0 = IncDesIncDes(null)
      Future(incdesl0, ContainsKey(0)) must be equalTo(false)
      Future(incdesl0, Add(null, incdes0))
      Future(incdesl0, Length()) must be equalTo (8)
      val incdesb0 = Future(incdesl0, Bytes()).asInstanceOf[Array[Byte]]

      val incdesl1 = IncDesIncDesList(new Mailbox)
      incdesl1.setSystemServices(systemServices)
      incdesl1.load(incdesb0)
      Future(incdesl1, ContainsKey(0)) must be equalTo(true)
      Future(incdesl1, Length()) must be equalTo (8)
      val incdes1 = Future(incdesl1, Get(0))
      incdes1.isInstanceOf[IncDesIncDes] must beTrue
    }
  }
}