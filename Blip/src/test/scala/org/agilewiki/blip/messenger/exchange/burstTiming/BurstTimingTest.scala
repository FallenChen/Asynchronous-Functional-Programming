package org.agilewiki.blip.messenger
package exchange.burstTiming

import org.specs.SpecificationWithJUnit

class BurstTimingTest extends SpecificationWithJUnit {
  "BurstTimingTest" should {
    "time messages" in {
      val threadManager = new MessengerThreadManager
      val c = 10//000
      val b = 10//000
      val sender = new Sender(c, b, threadManager)
      sender.finished //about 57 nanoseconds per message
    }
  }
}
