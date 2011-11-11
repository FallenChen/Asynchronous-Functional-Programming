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
package org.agilewiki.blip.messenger

import java.util.ArrayList

/**
 * ExchangeMessenger distinguishes between request and response messages,
 * with a response assured for every request received.
 */
abstract class ExchangeMessenger(threadManager: ThreadManager,
                                 _buffered: BufferedMessenger[ExchangeMessage] = null)
  extends MessageProcessor[ExchangeMessage]
  with MessageListDestination[ExchangeMessage] {

  /**
   * The BufferedMessenger object wrapped by the ExchangeMessenger.
   */
  val bufferedMessenger = {
    if (_buffered != null) _buffered
    else new BufferedMessenger[ExchangeMessage](threadManager)
  }

  bufferedMessenger.setMessageProcessor(this)

  /**
   * The incomingMessageList method is called to process a list of messages
   * when the current thread is different
   * from the thread being used by the object being called.
   */
  final def incomingMessageList(bufferedMessages: ArrayList[ExchangeMessage]) {
    bufferedMessenger.incomingMessageList(bufferedMessages)
  }

  /**
   * The putTo message builds lists of messages to be sent to other Buffered objects.
   */
  final def putTo(messageListDestination: MessageListDestination[ExchangeMessage], message: ExchangeMessage) {
    bufferedMessenger.putTo(messageListDestination, message)
  }

  /**
   * The isEmpty method returns true when there are no messages to be processed,
   * though the results may not always be correct due to concurrency issues.
   */
  final def isEmpty = bufferedMessenger.isEmpty

  /**
   * The poll method processes any messages in the queue.
   * Once complete, any pending outgoing messages are sent.
   */
  final protected def poll = bufferedMessenger.poll

  /**
   * The flushPendingMsgs is called when there are no pending incoming messages to process.
   */
  final protected def flushPendingMsgs = bufferedMessenger.flushPendingMsgs

  /**
   * The haveMessage method is called when there may be an incoming message to be processed.
   */
  override def haveMessage {
    poll
  }

  /**
   * The processMessage method is called when there is an incoming message to process.
   */
  final override def processMessage(msg: ExchangeMessage) {
    msg match {
      case req: ExchangeRequest => exchangeReq(req)
      case rsp: ExchangeResponse => exchangeRsp(rsp)
    }
  }

  /**
   * The exchangeReq method is called when there is an incoming request to process.
   */
  protected def exchangeReq(req: ExchangeRequest)

  /**
   * The exchangeRsp method is called when there is an incoming response to process.
   */
  def exchangeRsp(rsp: ExchangeResponse)
}