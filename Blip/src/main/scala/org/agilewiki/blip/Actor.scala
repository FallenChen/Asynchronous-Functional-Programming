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

import bind._
import exchange._
import seq.NavSetSeq
import annotation.tailrec

class Actor
  extends SystemServicesGetter
  with BindActor {

  private var _factory: Factory = null
  val components = new java.util.LinkedHashMap[Class[_ <: ComponentFactory], Component]
  private var componentList: java.util.ArrayList[Component] = null
  private var transactionActivityLevel = 0
  private lazy val pendingTransactions = new java.util.ArrayDeque[MailboxReq]
  private lazy val activeTransactions = new java.util.HashSet[MailboxReq]

  bind(classOf[Chain], eval)
  bind(classOf[SelfReq], self)
  bind(classOf[LeftReq], left)
  bind(classOf[RightReq], right)

  override protected def opener = {
    if (systemServices == null && exchangeMessenger != null)
      setSystemServices(mailboxFactory.asInstanceOf[BlipMailboxFactory].systemServices)
    if (!components.isEmpty) {
      componentList = new java.util.ArrayList[Component](components.values)
      var i = 0
      while (i < componentList.size) {
        componentList.get(i).open
        i += 1
      }
    }
    super.opener
  }

  def close {
    if (componentList == null) return
    var i = componentList.size
    while (i > 0) {
      i -= 1
      componentList.get(i).close
    }
  }

  def setFactory(factory: Factory) {
    if (isOpen) throw new IllegalStateException
    _factory = factory
  }

  override def exchangeMessenger = super.exchangeMessenger.asInstanceOf[Mailbox]

  def component(componentFactoryClass: Class[_ <: ComponentFactory]) = {
    val c = components.get(componentFactoryClass)
    if (c == null) throw new IllegalArgumentException("Component not found: " +
      componentFactoryClass.getName)
    c
  }

  def factory = _factory

  protected var _systemServices: SystemServices = null

  def setSystemServices(systemServices: SystemServices) {
    if (isOpen) throw new IllegalStateException
    _systemServices = systemServices
  }

  override def systemServices: SystemServices = _systemServices

  lazy val messageClasses = {
    val smf = new java.util.TreeSet[Class[_ <: AnyRef]](
      new ClassComparator
    )
    smf.addAll(messageLogics.keySet)
    val seq = new NavSetSeq(smf)
    seq.setExchangeMessenger(exchangeMessenger)
    seq
  }

  lazy val componentFactoryClasses = {
    val smf = new java.util.TreeSet[Class[_ <: ComponentFactory]](
      new ClassComparator
    )
    smf.addAll(components.keySet)
    val seq = new NavSetSeq(smf)
    seq.setExchangeMessenger(exchangeMessenger)
    seq
  }

  def isInvalid = transactionActivityLevel < 0

  def isActive = transactionActivityLevel > 0

  def isIdle = transactionActivityLevel == 0

  def maxLevel = {
    var level = 0
    val it = activeTransactions.iterator
    while (it.hasNext) {
      val currentRequest = it.next
      val l = currentRequest.binding.asInstanceOf[BoundTransaction].level
      if (l > level) level = l
    }
    level
  }

  def addPendingTransaction(mailboxReq: MailboxReq) {
    pendingTransactions.addLast(mailboxReq)
    runPendingTransaction
  }

  @tailrec final def runPendingTransaction {
    val mailboxReq = pendingTransactions.peekFirst
    if (mailboxReq == null) return
    if (!isTransactionCompatible(mailboxReq)) return
    pendingTransactions.removeFirst
    addActiveTransaction(mailboxReq)
    val transaction = mailboxReq.binding.asInstanceOf[BoundTransaction]
    transaction.processTransaction(exchangeMessenger, mailboxReq)
    runPendingTransaction
  }

  def isTransactionCompatible(mailboxReq: MailboxReq) = {
    mailboxReq.binding.asInstanceOf[BoundTransaction].maxCompatibleLevel >= transactionActivityLevel
  }

  def addActiveTransaction(mailboxReq: MailboxReq) {
    val l = mailboxReq.binding.asInstanceOf[BoundTransaction].level
    if (l > transactionActivityLevel) transactionActivityLevel = l
    activeTransactions.add(mailboxReq)
  }

  def removeActiveTransaction {
    val mailboxReq = exchangeMessenger.curReq
    val l = mailboxReq.binding.asInstanceOf[BoundTransaction].level
    activeTransactions.remove(mailboxReq)
    if (l == transactionActivityLevel) {
      transactionActivityLevel = maxLevel
    }
  }

  def eval(msg: AnyRef, rf: Any => Unit) {
    val chain = msg.asInstanceOf[Chain]
    eval(chain, 0, null)(rf)
  }

  private def _eval(chain: Chain, pos: Int, last: Any)(rf: Any => Unit) {
    eval(chain, pos, last)(rf)
  }

  @tailrec final def eval(chain: Chain, pos: Int, _last: Any)(rf: Any => Unit) {
    if (pos >= chain.size) {
      rf(_last)
      return
    }
    var async = false
    var sync = false
    var last: Any = null
    val op = chain.get(pos)
    val target = op.actor().asInstanceOf[Actor]
    val msg = op.msg().asInstanceOf[AnyRef]
    if (target != null && msg != null) {
      target(msg) {
        rsp => {
          last = rsp
          val key = op.result
          if (key != null) {
            chain.results.put(key, rsp)
          }
          if (async) _eval(chain, pos + 1, last)(rf)
          else sync = true
        }
      }
      if (!sync) {
        async = true
        return
      }
    }
    eval(chain, pos + 1, last)(rf)
  }

  def self(msg: AnyRef, rf: Any => Unit) {
    val value = msg.asInstanceOf[SelfReq].value
    rf(value)
  }

  def left(msg: AnyRef, rf: Any => Unit) {
    val (l, r) = msg.asInstanceOf[LeftReq].tuple
    rf(l)
  }

  def right(msg: AnyRef, rf: Any => Unit) {
    val (l, r) = msg.asInstanceOf[RightReq].tuple
    rf(r)
  }

  override def newRequest(rf: Any => Unit,
                          data: AnyRef,
                          bound: QueuedLogic,
                          src: ExchangeMessengerSource) =
    new MailboxReq(this, rf, data, bound, src)

  def factoryId = {
    if (factory == null) null
    else factory.id
  }
}
