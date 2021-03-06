package org.agilewiki.blip
package exchange
package echoTiming

import annotation.tailrec
import messenger._
import java.util.concurrent.Semaphore

class Sender(c: Int, threadManager: ThreadManager)
  extends Exchange(threadManager)
  with ExchangeMessengerActor {

  val done = new Semaphore(0)
  val echo = new Echo(threadManager)
  var count = 0
  var i = 0
  var t0 = 0L

  count = c
  i = c
  t0 = System.currentTimeMillis
  echo.sendReq(echo, new ExchangeRequest(this, processResponse), this)
  flushPendingMsgs

  def finished {
    done.acquire
  }

  override def exchangeMessenger = this

  override def processRequest {}

  private def dummy(rsp: Any) {
    processResponse(rsp)
  }

  @tailrec private def processResponse(rsp: Any) {
    if (i < 1) {
      val t1 = System.currentTimeMillis
      if (t1 != t0) println("msgs per sec = " + (count * 2L * 1000L / (t1 - t0)))
      threadManager.close
      done.release
      return
    }
    var async = false
    var sync = false
    i -= 1
    var rsp: Any = null
    echo.sendReq(echo, new ExchangeRequest(this, {
      msg => {
        rsp = msg
        if (async) dummy(rsp)
        else sync = true
      }
    }), this)
    if (!sync) {
      async = true
      return
    }
    processResponse(rsp)
  }
}
