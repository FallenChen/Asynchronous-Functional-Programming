package org.agilewiki.blip
package exchange
package exchangeMessenger.echoTiming

import messenger._
import java.util.concurrent.Semaphore

class Sender(c: Int, threadManager: ThreadManager)
  extends ExchangeMessenger(threadManager)
  with ExchangeMessengerActor {

  val done = new Semaphore(0)
  val echo = new Echo(threadManager)
  var count = 0
  var i = 0
  var t0 = 0L

  count = c
  i = c
  t0 = System.currentTimeMillis
  echo.sendReq(echo, new ExchangeMessengerRequest(this, processResponse), this)
  flushPendingMsgs

  def finished {
    done.acquire
  }

  override def exchangeMessenger = this

  override def processRequest {}

  def processResponse(rsp: Any) {
    if (i > 0) {
      i -= 1
      echo.sendReq(echo, new ExchangeMessengerRequest(this, processResponse), this)
    } else {
      val t1 = System.currentTimeMillis
      if (t1 != t0) println("msgs per sec = " + (count * 2L * 1000L / (t1 - t0)))
      threadManager.close
      done.release
    }
  }
}
