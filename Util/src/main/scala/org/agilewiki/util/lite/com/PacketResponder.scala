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
package com

import java.util.{UUID, HashMap}

object PacketResponder {
  def apply(systemContext: SystemComposite, remoteServer: String, hostPort: HostPort) = {
    val udpSender = Udp(systemContext).udpSender
    val reactor = new ContextReactor(systemContext)
    val packetResponder = new PacketResponder(reactor, hostPort)
    val packetResender = new PacketResender(
      reactor,
      Uuid(remoteServer),
      packetResponder,
      udpSender)
    packetResponder.outsideActor = packetResender
    packetResponder
  }
}

class PacketResponder(reactor: ContextReactor, hostPort: HostPort)
  extends LiteActor(reactor) {
  var outsideActor: LiteActor = null
  val requestsSent = new HashMap[String, LiteReqMsg]
  private val liteManager = Lite(systemContext).liteManager
  private val defaultReactor = new ContextReactor(systemContext)

  addRequestHandler {
    case packet: PacketReq => forwardOutgoingRequest(packet)
    case packet: IncomingPacketReq if !packet.isReply => incomingRequest(packet)
    case packet: IncomingPacketReq if packet.isReply => incomingReply(packet)
    case packet: OutgoingPacketReq => timeout(packet)
  }

  def forwardOutgoingRequest(packet: PacketReq) {
    val msgUuid = UUID.randomUUID.toString
    requestsSent.put(msgUuid, liteReactor.currentRequestMessage)
    val externalPacket = OutgoingPacketReq(
      false,
      Uuid(msgUuid),
      hostPort,
      packet.server,
      packet.actorName,
      packet.payload)
    send(outsideActor, externalPacket) {
      case rsp: OutgoingPacketRsp =>
    }
  }

  def incomingRequest(packet: IncomingPacketReq) {
    val req = PacketReq(packet.server, packet.actorName, packet.payload)
    packet.actorName match {
      case rn: ClassName => send(
        liteManager,
        CreateReq(new ContextReactor(systemContext), rn)) {
        case rsp: CreateRsp => {
          val actor = rsp.actor
          send(actor, req) {
            case rsp: DataOutputStack => sendRsp(packet, rsp, actor)
            case error: ErrorRsp => sendErrorRsp(packet, error, actor)
          }
        }
        case rsp: ErrorRsp => sendErrorRsp(packet, rsp, liteManager)
      }
      case rn: Uuid => send(liteManager, MapGetReq(rn)) {
        case rsp: MapGetRsp => {
          val actor = rsp.actor
          if (actor != null) {
            send(rsp.actor, req) {
              case rsp: DataOutputStack => sendRsp(packet, rsp, actor)
              case error: ErrorRsp => sendErrorRsp(packet, error, actor)
            }
          } else throw new IllegalArgumentException
        }
      }
    }
  }

  private def sendRsp(incomingPacket: IncomingPacketReq,
                      outputPayload: DataOutputStack,
                      actor: InternalAddressActor) {
    outputPayload.writeByte(false.asInstanceOf[Byte])
    val actorName = actor.getUuid
    val externalPacket = OutgoingPacketReq(
      true,
      incomingPacket.msgUuid,
      hostPort,
      incomingPacket.server,
      actor.getUuid,
      outputPayload)
    send(outsideActor, externalPacket) {
      case rsp => reply(rsp)
    }
  }

  private def sendErrorRsp(incomingPacket: IncomingPacketReq,
                           error: ErrorRsp,
                           actor: LiteActor) {
    val outputPayload = DataOutputStack()
    outputPayload.writeUTF(error.target.toString)
    outputPayload.writeUTF(error.source.toString)
    outputPayload.writeUTF(error.text)
    outputPayload.writeByte(true.asInstanceOf[Byte])
    val externalPacket = OutgoingPacketReq(
      true,
      incomingPacket.msgUuid,
      hostPort,
      incomingPacket.server,
      ClassName(actor.getClass),
      outputPayload)
    send(outsideActor, externalPacket) {
      case rsp => reply(rsp)
    }
  }

  def incomingReply(packet: IncomingPacketReq) {
    val liteReqMsg = requestsSent.remove(packet.msgUuid.value)
    val inputPayload = packet.inputPayload
    val isError = inputPayload.readByte.asInstanceOf[Boolean]
    var rsp: AnyRef = null
    if (isError){
      val txt = inputPayload.readUTF
      val src = ClassName(inputPayload.readUTF)
      val target = ClassName(inputPayload.readUTF)
      rsp = new ErrorRsp(txt, src, target)
    } else {
      rsp = inputPayload
    }
    val actor = liteReqMsg.sender.asInstanceOf[LiteActor]
    val liteRspMsg = new LiteRspMsg(0, liteReqMsg.responseProcess, liteReqMsg, rsp)
    actor.liteReactor.response(liteRspMsg)
  }

  def timeout(packet: OutgoingPacketReq) {
    val liteReqMsg = requestsSent.remove(packet.msgUuid.value)
    val actor = liteReqMsg.sender.asInstanceOf[LiteActor]
    val error = new ErrorRsp(
      "timeout",
      ClassName(this.getClass),
      ClassName(outsideActor.getClass))
    val liteRspMsg = new LiteRspMsg(0, liteReqMsg.responseProcess, liteReqMsg, error)
    actor.liteReactor.response(liteRspMsg)
  }
}