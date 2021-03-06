package org.agilewiki
package db
package transactions
package swift
package noFlushSwiftTimings

import blip._
import bind._
import seq._
import services._
import log._
import incDes._
import batch._
import org.specs.SpecificationWithJUnit

case class DoIt()

class Driver extends Actor {
  bind(classOf[DoIt], doit)

  def doit(msg: AnyRef, rf: Any => Unit) {
    val range = new Range(0, 1000)
    range.setExchangeMessenger(exchangeMessenger)
    range.setSystemServices(systemServices)
    range(LoopSafe(Looper()))(rf)
  }
}

case class Looper() extends MessageLogic {
  override def func(target: BindActor, msg: AnyRef, rf: Any => Unit)(implicit sender: ActiveActor) {
    val systemServices = target.asInstanceOf[Actor].systemServices
    val batch = Batch(systemServices)
    val chain = new Chain
    chain.op(systemServices, Unit => RecordUpdate(batch, "r1", "$", IncDesInt(null)))
    chain.op(systemServices, TransactionRequest(batch))
    target.asInstanceOf[Actor](chain) {
      rsp => rf(true)
    }
  }
}

class NoFlushSwiftTimingsTest extends SpecificationWithJUnit {
  "NoFlushSwiftTimingsTest" should {
    "time swift" in {
      val systemServices = SystemServices(new ServicesRootComponentFactory)
      val dbName = "noFlushSwiftTimings.db"
      val logDirPathname = "noFlushSwiftTimings"
      val file = new java.io.File(dbName)
      file.delete
      EmptyLogDirectory(logDirPathname)
      val properties = new Properties
      properties.put("dbPathname", dbName)
      properties.put("logDirPathname", logDirPathname)
      properties.put("flushLog", "false")
      properties.put("commitsPerWrite", "1000")
      properties.put("logBlockSize", "" + (32 * 1024))
      val db = Subsystem(
        systemServices,
        new SwiftComponentFactory,
        properties = properties,
        actorId = ActorId("db"))
      val driver = new Driver
      driver.setExchangeMessenger(db.exchangeMessenger)
      driver.setSystemServices(db)
      val batch = Batch(db)
      val chain = new Chain
      chain.op(systemServices, Register(db))
      chain.op(db, NewRecord(batch, "r1"))
      chain.op(db, TransactionRequest(batch))
      chain.op(driver, DoIt())
      Future(systemServices, chain)
      systemServices.close
    }
  }
}
