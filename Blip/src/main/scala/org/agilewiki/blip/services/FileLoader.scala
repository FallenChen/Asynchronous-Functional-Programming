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
package blip
package services

import java.io.{DataInputStream, FileInputStream}
import bind._

class FileLoader
  extends Actor {
  bind(classOf[LoadFile], loadFile)

  def loadFile(msg: AnyRef, rf: Any => Unit) {
    val file = msg.asInstanceOf[LoadFile].file
    val size = file.length
    val fis = new FileInputStream(file)
    val dis = new DataInputStream(fis)
    val bytes = new Array[Byte](size.asInstanceOf[Int])
    dis.readFully(bytes)
    rf(bytes)
  }
}

class FileLoaderComponent(actor: Actor)
  extends Component(actor) {
  val fileLoader = new FileLoader
  bindMessageLogic(classOf[LoadFile], new Forward(fileLoader))

  override def open {
    super.open
    fileLoader.setExchangeMessenger(systemServices.newAsyncMailbox)
  }
}

class FileLoaderComponentFactory extends ComponentFactory {
  override def instantiate(actor: Actor) = new FileLoaderComponent(actor)
}
