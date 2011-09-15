package org.agilewiki
package db
package simpleNoLogDataStore

import blip._
import services._
import org.specs.SpecificationWithJUnit

class SimpleNoLogDataStoreTest extends SpecificationWithJUnit {
  "SimpleNoLogDataStoreTest" should {
    "update & query" in {
      val systemServices = SystemServices(new ServicesRootComponentFactory)
      val dbName = "SimpleNoLog.db"
      val file = new java.io.File(dbName)
      file.delete
      val properties = new Properties
      properties.put("dbPathname", dbName)
      val db = Subsystem(
        systemServices,
        new SimpleNoLogDataStoreComponentFactory,
        properties = properties,
        actorId = ActorId("db"))
      val results = new Results
      val chain = new Chain(results)
      chain.op(systemServices, Register(db))
      chain.op(db, SetRootStringRequest.process(db, "Hello world!"), "timestamp")
      chain.op(db, GetRootStringRequest.process(db), "string")
      Future(systemServices, chain)
      println(results)
      systemServices.close
    }
  }
}