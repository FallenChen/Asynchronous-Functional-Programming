package org.agilewiki
package incDes
package blocks
package simpleNoLogDataStore

import blip._

object GetRootStringRequest {
  def apply() = (new GetRootStringRequestFactory).newActor(null).
    asInstanceOf[IncDesString]

  def process(db: Actor) {
    val je = apply()
    val chain = new Chain
    chain.op(db, TransactionRequest(je))
    chain
  }
}

class GetRootStringRequestFactory extends Factory(new FactoryId("GetRootStringRequest")) {
  include(new GetRootStringRequestComponentFactory)

  override protected def instantiate = new IncDesString
}

class GetRootStringRequestComponentFactory extends ComponentFactory {
  addDependency(classOf[QueryRequestComponentFactory])

  override def instantiate(actor: Actor) = new GetRootStringRequestComponent(actor)
}

class GetRootStringRequestComponent(actor: Actor)
  extends Component(actor) {
  bind(classOf[Process], process)

  private def process(msg: AnyRef, rf: Any => Unit) {
    val transactionContext = msg.asInstanceOf[Process].transactionContext
    val results = new Results
    val chain = new Chain(results)
    chain.op(systemServices, DbRoot(), "dbRoot")
    chain.op(Unit => results("dbRoot"), Value(), "incDesString")
    chain.op(Unit => results("incDesString"), Value(), "value")
    actor(chain) {
      rsp => {
        rf(results("value"))
      }
    }
 }
}
