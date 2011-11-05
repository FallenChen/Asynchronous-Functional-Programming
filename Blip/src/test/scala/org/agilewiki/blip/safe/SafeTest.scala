package org.agilewiki.blip
package safe

import org.specs.SpecificationWithJUnit

case class Print(value: Any)

case class PrintEven(value: Int)

case class AMsg()

class Driver(mailboxFactory: MailboxFactory) extends AsyncActor(mailboxFactory) {
  bind(classOf[AMsg], aMsgFunc)
  setMailbox(new ReactorMailbox)

  private def aMsgFunc(msg: AnyRef, rf: Any => Unit) {
    val safeActor = new SafeActor(mailboxFactory)
    safeActor(PrintEven(1)){rsp =>
      safeActor(PrintEven(2)){rsp => rf(null)}
    }
  }
}

case class SafePrintEven(safeActor: SafeActor)
  extends Safe {
  override def func(target: Actor, msg: AnyRef, rf: Any => Unit)(implicit sender: ActiveActor) {
    val printEven = msg.asInstanceOf[PrintEven]
    val value = printEven.value
    if (value % 2 == 0) safeActor(Print(value))(rf)
    else rf(null)
  }
}

class SafeActor(mailboxFactory: MailboxFactory) extends AsyncActor(mailboxFactory) {
  bind(classOf[Print], printFunc)
  setMailbox(new ReactorMailbox)

  private def printFunc(msg: AnyRef, rf: Any => Unit) {
    println(msg.asInstanceOf[Print].value)
    rf(null)
  }

  bindSafe(classOf[PrintEven], SafePrintEven(this))
}

class SafeTest extends SpecificationWithJUnit {
  "SafeTest" should {
    "print even numbers" in {
      val mailboxFactory = new MailboxFactory
      try {
        val driver = new Driver(mailboxFactory)
        driver.setMailbox(mailboxFactory.asyncMailbox)
        Future(driver, AMsg())
      } finally {
        mailboxFactory.close
      }
    }
  }
}
