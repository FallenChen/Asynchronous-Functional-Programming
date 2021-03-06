package org.agilewiki.blip
package ioc

import org.specs.SpecificationWithJUnit
import bind._

case class Today()

class Sayings(actor: Actor) extends Component(actor) {
  bind(classOf[Today], today)

  def today(msg: AnyRef, rf: Any => Unit) {
    rf("Today is the first day of the rest of your life.")
  }
}

class SayingsFactory extends ComponentFactory {
  override protected def instantiate(actor: Actor) = new Sayings(actor)
}

case class SaySomething()

class SayIt extends Actor {
  bind(classOf[SaySomething], saySomething)

  def saySomething(msg: AnyRef, rf: Any => Unit) {
    systemServices(Today())(rf)
  }
}

class IOCTest extends SpecificationWithJUnit {
  "SimpleActor" should {
    "print" in {
      val systemServices = SystemServices(new SayingsFactory)
      try {
        val sayIt = new SayIt
        sayIt.setExchangeMessenger(systemServices.newSyncMailbox)
        println(Future(sayIt, SaySomething()))
      } finally {
        systemServices.close
      }
    }
  }
}
