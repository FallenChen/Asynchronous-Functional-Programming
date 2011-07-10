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

class Actor(_mailbox: Mailbox, _factory: Factory) extends Responder with MsgSrc {
  override def mailbox = _mailbox

  override def factory = _factory

  private val _activeActor = ActiveActor(this)

  implicit def activeActor: ActiveActor = _activeActor

  private var actorId: ActorId = null

  override def id = actorId

  def id(_id: ActorId) {
    if (actorId != null) throw new UnsupportedOperationException
    actorId = _id
  }

  private var _isSingleton = false

  def isSingleton = _isSingleton

  def singleton {
    id(ActorId(factoryId.value))
    _isSingleton = true
  }

  var _systemServices: SystemServices = null

  def systemServices(systemServices: SystemServices) {_systemServices = systemServices}

  override def systemServices: SystemServices = _systemServices

  def apply(msg: AnyRef)
           (responseFunction: Any => Unit)
           (implicit srcActor: ActiveActor) {
    val srcMailbox = {
      if (srcActor == null) null
      else srcActor.actor.mailbox
    }
    if (srcMailbox == null && mailbox != null) throw new UnsupportedOperationException(
      "An immutable actor can only send to another immutable actor."
    )
    if (safeMessageFunctions.containsKey(msg.getClass)) sendSafe(msg, responseFunction, srcActor)
    else if (mailbox == null || mailbox == srcMailbox) sendSynchronous(msg, responseFunction)
    else srcMailbox.send(this, msg)(responseFunction)
  }

  def sendSynchronous(msg: AnyRef, responseFunction: Any => Unit) {
    val reqFunction = messageFunctions.get(msg.getClass)
    if (reqFunction == null) throw new UnsupportedOperationException(msg.getClass.getName)
    reqFunction(msg, responseFunction)
  }

  def sendSafe(msg: AnyRef, responseFunction: Any => Unit, srcActor: ActiveActor) {
    val safeReqFunction = safeMessageFunctions.get(msg.getClass)
    if (safeReqFunction == null) throw new UnsupportedOperationException(msg.getClass.getName)
    safeReqFunction(msg, responseFunction, activeActor)
  }

  override def response(msg: MailboxRsp) {
    mailbox.response(msg)
  }

  def addComponent(componentFactory: ComponentFactory) = {
    val component = componentFactory.instantiate
    val componentMsgFunctions = component.messageFunctions
    var it = componentMsgFunctions.keySet.iterator
    while (it.hasNext) {
      val k = it.next
      if (messageFunctions.containsKey(k)) {
        throw new IllegalArgumentException("bind conflict on actor " +
          getClass.getName +
          "message " +
          k.getName)
      }
      val v = componentMsgFunctions.get(k)
      messageFunctions.put(k, v)
    }
    val safeComponentMsgFunctions = component.safeMessageFunctions
    var its = safeComponentMsgFunctions.keySet.iterator
    while (its.hasNext) {
      val k = its.next
      if (safeMessageFunctions.containsKey(k)) {
        throw new IllegalArgumentException("bindSafe conflict on actor " +
          getClass.getName +
          "message " +
          k.getName)
      }
      val v = safeComponentMsgFunctions.get(k)
      safeMessageFunctions.put(k, v)
    }
    component
  }
}
