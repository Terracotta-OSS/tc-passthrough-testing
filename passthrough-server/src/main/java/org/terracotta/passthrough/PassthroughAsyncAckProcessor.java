/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvocationCallback;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;


/**
 * Used by the client-side message processing to handle the synchronous nature of the messaging system.  This expects the
 * client code's thread to block on acks or completion, and be unblocked by the client-send message processing thread
 * processing the corresponding acks and completion messages.
 */
public class PassthroughAsyncAckProcessor implements PassthroughAckProcessor {

  private final MessageCodec<? extends EntityMessage, ? extends EntityResponse> messageCodec;
  private final InvocationCallback<EntityResponse> invocationCallback;

  private volatile byte[] rawMessage;
  private volatile boolean sent = false;

  public <R extends EntityResponse, M extends EntityMessage> PassthroughAsyncAckProcessor(MessageCodec<M, R> messageCodec, InvocationCallback<EntityResponse> invocationCallback) {
    this.messageCodec = messageCodec;
    this.invocationCallback = invocationCallback;
  }

  @Override
  public void sent() {
    this.sent = true;
    invocationCallback.sent();
  }

  @Override
  public void handleAck() {
    if (!sent) {
      sent();
    }
    invocationCallback.received();
  }

  @Override
  public void handleComplete(byte[] result, EntityException error) {
    try {
      if (error == null) {
        EntityResponse entityResponse = messageCodec.decodeResponse(result);
        invocationCallback.result(entityResponse);
      } else {
        invocationCallback.failure(error);
      }
    } catch (MessageCodecException e) {
      invocationCallback.failure(e);
    }
  }

  @Override
  public void handleMonitor(byte[] result) {
    // TODO no-op?
  }

  @Override
  public void handleRetire() {
    invocationCallback.retired();
  }

  @Override
  public void saveRawMessageForResend(byte[] raw) {
    this.rawMessage = raw;
  }

  @Override
  public byte[] resetAndGetMessageForResend() {
    return rawMessage;
  }

  @Override
  public void forceDisconnect() {
    // TODO implement me
  }

  @Override
  public void blockGetOnRetire() {
    // TODO no-op?
  }

  public boolean isDone() {
    return sent;
  }
}
