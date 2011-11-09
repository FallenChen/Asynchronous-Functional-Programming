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
package org.agilewiki.blip

import messenger._

class Mailbox(_mailboxFactory: MailboxFactory)
  extends MessageProcessor[MailboxMsg] {
  val messenger = new BufferedMessenger[MailboxMsg](this, mailboxFactory.threadManager)
  var mailboxState: MailboxState = null

  def mailboxFactory: MailboxFactory = _mailboxFactory

  def newMailboxState = {
    mailboxState = new MailboxState
    mailboxState
  }

  def poll = messenger.poll

  def control = this

  def sendReq(targetActor: Actor,
              req: MailboxReq,
              srcMailbox: Mailbox) {
    srcMailbox.messenger.putTo(targetActor.buffered, req)
  }

  override def haveMessage {
    poll
  }

  override def processMessage(msg: MailboxMsg) {
    msg match {
      case msg: MailboxReq => msg.binding.process(this, msg)
      case msg: MailboxRsp => rsp(msg)
    }
  }

  def isMailboxEmpty = messenger.isEmpty

  def reqExceptionFunction(ex: Exception) {
    reply(ex)
  }

  def rsp(msg: MailboxRsp) {
    msg.responseFunction(msg.rsp)
  }

  def reply(content: Any) {
    val req = mailboxState.currentRequestMessage
    if (!req.active || req.responseFunction == null) {
      return
    }
    req.active = false
    val sender = req.sender
    val rsp = new MailboxRsp(
      req.responseFunction,
      req.oldRequest,
      content)
    if (sender.isInstanceOf[Actor]) {
      val senderActor = sender.asInstanceOf[Actor]
      val senderMailbox = senderActor.mailbox
      sendReply(rsp, senderMailbox)
    } else {
      messenger.putTo(sender.asInstanceOf[MessageListDestination[MailboxMsg]], rsp)
    }
  }

  protected def sendReply(rsp: MailboxRsp, senderMailbox: Mailbox) {
    messenger.putTo(senderMailbox.messenger, rsp)
  }
}
