package org.agilewiki.blip
package seq
package tailSeq

import org.specs.SpecificationWithJUnit

class TailSeqTest extends SpecificationWithJUnit {
  "TailSeqTest" should {
    "tail" in {
      val tail = new TailSeq(Range(0, 1000000), 999998)
      Future(tail, Loop((key: Int, value: Int) => println(key+" "+value)))
    }
  }
}