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

import org.terracotta.entity.AsyncInvocationBuilder;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvocationCallback;
import org.terracotta.entity.InvokeMonitor;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Used by the client-side PassthroughEntityClientEndpoint to build the invocation which will be sent to the server.
 * Note that this isn't where the invocation ack is tracked, just the object which builds that message and ack tracking
 * mechanism (by requesting it in the underlying PassthroughConnection).
 */
public class PassthroughAsyncInvocationBuilder<M extends EntityMessage, R extends EntityResponse> implements AsyncInvocationBuilder<M, R> {
  private final PassthroughConnection connection;
  private final String entityClassName;
  private final String entityName;
  private final long clientInstanceID;
  private final MessageCodec<M, R> messageCodec;

  private boolean shouldReplicate;
  private M request;
  private long enqueueingBlockTime;
  private TimeUnit enqueueingBlockTimeUnit;

  public PassthroughAsyncInvocationBuilder(PassthroughConnection connection, String entityClassName, String entityName, long clientInstanceID, MessageCodec<M, R> messageCodec) {
    this.connection = connection;
    this.entityClassName = entityClassName;
    this.entityName = entityName;
    this.clientInstanceID = clientInstanceID;
    this.messageCodec = messageCodec;
    
    // We want to replicate, by default.
    this.shouldReplicate = true;
  }

  @Override
  public AsyncInvocationBuilder<M, R> replicate(boolean b) {
    this.shouldReplicate = b;
    return this;
  }

  @Override
  public AsyncInvocationBuilder<M, R> message(M m) {
    this.request = m;
    return this;
  }

  @Override
  public AsyncInvocationBuilder<M, R> blockEnqueuing(long time, TimeUnit unit) {
    this.enqueueingBlockTime = time;
    this.enqueueingBlockTimeUnit = unit;
    return this;
  }

  @Override
  public void invoke(InvocationCallback<R> invocationCallback) throws RejectedExecutionException {
    try {
      PassthroughMessage message = PassthroughMessageCodec.createInvokeMessage(this.entityClassName, this.entityName, this.clientInstanceID, messageCodec.encodeMessage(this.request), this.shouldReplicate);
      boolean shouldWaitForSent = enqueueingBlockTimeUnit != null;
      this.connection.invokeAsyncAction(message,
          shouldWaitForSent, messageCodec, invocationCallback);
    } catch (MessageCodecException e) {
      invocationCallback.failure(e);
    }
  }

}
