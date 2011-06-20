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
package org.agilewiki
package util
package lite
package seq

import annotation.tailrec

trait SeqExtension[T, V]
  extends LiteExtension
  with SeqComparator[T] {

  addRequestHandler{
    case req: SeqCurrentReq[T] => reply(current(req.key))
    case req: SeqNextReq[T] => reply(next(req.key))
  }

  def first = current(null.asInstanceOf[T])

  def current(key: T): SeqRsp

  def next(key: T): SeqRsp

  def _fold(seed: V, fold: (V, V) => V): FoldRsp[V] = {
    val rsp = first
    if (rsp.isInstanceOf[SeqEndRsp]) return FoldRsp(seed)
    val result = rsp.asInstanceOf[SeqResultRsp[T, V]]
    _foldNext(result.key, fold(seed, result.value), fold)
  }

  @tailrec private def _foldNext(key: T, seed: V, fold: (V, V) => V): FoldRsp[V] = {
    val rsp = next(key)
    if (rsp.isInstanceOf[SeqEndRsp]) return FoldRsp(seed)
    val result = rsp.asInstanceOf[SeqResultRsp[T, V]]
    _foldNext(result.key, fold(seed, result.value), fold)
  }

  def _exists(exists: V => Boolean): ExistsRsp = {
    val rsp = first
    if (rsp.isInstanceOf[SeqEndRsp]) return ExistsRsp(false)
    val result = rsp.asInstanceOf[SeqResultRsp[T, V]]
    if (exists(result.value)) return ExistsRsp(true)
    _existsNext(result.key, exists)
  }

  @tailrec private def _existsNext(key: T, exists: V => Boolean): ExistsRsp = {
    val rsp = next(key)
    if (rsp.isInstanceOf[SeqEndRsp]) return ExistsRsp(false)
    val result = rsp.asInstanceOf[SeqResultRsp[T, V]]
    if (exists(result.value)) return ExistsRsp(true)
    _existsNext(result.key, exists)
  }

  def _find(find: V => Boolean): FindRsp = {
    val rsp = first
    if (rsp.isInstanceOf[SeqEndRsp]) return NotFoundRsp()
    val result = rsp.asInstanceOf[SeqResultRsp[T, V]]
    if (find(result.value)) return FoundRsp(result.value)
    _findNext(result.key, find)
  }

  @tailrec private def _findNext(key: T, find: V => Boolean): FindRsp = {
    val rsp = next(key)
    if (rsp.isInstanceOf[SeqEndRsp]) return NotFoundRsp()
    val result = rsp.asInstanceOf[SeqResultRsp[T, V]]
    if (find(result.value)) return FoundRsp(result.value)
    _findNext(result.key, find)
  }
}
