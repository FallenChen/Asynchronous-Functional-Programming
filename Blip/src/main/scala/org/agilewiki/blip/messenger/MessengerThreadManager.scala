/*
 * Copyright 2011 Bill La Forge
 *
 * This file is part of AgileWiki and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (LGPL) as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * or navigate to the following url http://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Note however that only Scala, Java and JavaScript files are being covered by LGPL.
 * All other files are covered by the Common Public License (CPL).
 * A copy of this license is also included and can be
 * found as well at http://www.opensource.org/licenses/cpl1.0.txt
 */
package org.agilewiki.blip.messenger

import java.util.concurrent.{ConcurrentLinkedQueue, Semaphore, ThreadFactory}
import annotation.tailrec
import java.util.ArrayList

/**
 * The MessengerThreadManager starts a number of threads (12 by default)
 * for processing Runnable tasks.
 * By default, the MessengerThreadFactory is used to create threads,
 * though it is easily replaced by any class which implements java.util.ThreadFactory.
 */
class MessengerThreadManager(threadCount: Int = 12,
                             threadFactory: ThreadFactory = new MessengerThreadFactory)
  extends ThreadManager with Runnable {

  val taskRequest = new Semaphore(0)
  val tasks = new ConcurrentLinkedQueue[Runnable]
  var closing = false
  val threads = new java.util.ArrayList[Thread]()

  init

  /**
   * The init method is called in the constructor and is used to start threadCount threads.
   */
  private def init {
    var c = 0
    while (c < threadCount) {
      c += 1
      val t = threadFactory.newThread(this)
      threads.add(t)
      t.start
    }
  }

  /**
   * The run method is used by all the threads.
   * This method wakes up a thread when there is a task to be processed
   * and stops idle threads after the close method has been called.
   */
  @tailrec final override def run {
    taskRequest.acquire
    if (closing) return
    val task = tasks.poll
    try {
      task.run
    } catch {
      case t: Throwable => t.printStackTrace()
    }
    run
  }

  /**
   * The process method is used to request the processing of a Runnable task.
   * This method adds the task to a concurrent queue of tasks to be processed
   * and then wakes up a task.
   */
  override def process(task: Runnable) {
    tasks.add(task)
    taskRequest.release
  }

  /**
   * The close method is used to stop all the threads as they become idle.
   * This method sets a flag to indicate that the threads should stop
   * and then wakes up all the threads. Then it waits for all the threads to die.
   */
  def close {
    closing = true
    var c = 0
    while (c < threadCount) {
      c += 1
      taskRequest.release
    }
    c = 0
    val ct = Thread.currentThread()
    while (c < threadCount) {
      val t = threads.get(c)
      if (ct != t) t.join()
      c += 1
    }
  }
}
