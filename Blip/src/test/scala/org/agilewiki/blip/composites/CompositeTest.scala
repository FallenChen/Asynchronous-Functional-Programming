package org.agilewiki.blip
package composites

import org.specs.SpecificationWithJUnit

case class Set(value: Int)

case class Get()

class SaverComponentFactory extends ComponentFactory {
  override def instantiate(actor: Actor) = new SaverComponent(actor)
}

class SaverComponent(actor: Actor) extends Component(actor) {
  var i = 0

  bind(classOf[Set], setFunc)

  private def setFunc(msg: AnyRef, rf: Any => Unit) {
    i = msg.asInstanceOf[Set].value
    rf(null)
  }

  bind(classOf[Get], getFunc)

  private def getFunc(msg: AnyRef, rf: Any => Unit) {
    rf(i)
  }
}

case class Times2()

class DoubleComponentFactory extends ComponentFactory {
  addDependency(classOf[SaverComponentFactory])

  override def instantiate(actor: Actor) = new DoubleComponent(actor)
}

class DoubleComponent(actor: Actor) extends Component(actor) {
  val saver = actor.component(classOf[SaverComponentFactory]).asInstanceOf[SaverComponent]

  bind(classOf[Times2], doubleFunc)

  private def doubleFunc(msg: AnyRef, rf: Any => Unit) {
    saver.i *= 2
    rf(null)
  }
}

class DoubleFactory extends Factory(null) {
  include(new DoubleComponentFactory)
}

class CompositeTest extends SpecificationWithJUnit {
  "SimpleActor" should {
    "double" in {
      val doubleFactory = new DoubleFactory
      val double = doubleFactory.newActor(new ReactorMailbox)
      Future(double, Set(21))
      Future(double, Times2())
      println(Future(double, Get()))
    }
  }
}
