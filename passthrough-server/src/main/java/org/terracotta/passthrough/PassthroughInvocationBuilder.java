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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.terracotta.entity.InvokeMonitor;
import org.terracotta.exception.EntityServerException;


/**
 * Used by the client-side PassthroughEntityClientEndpoint to build the invocation which will be sent to the server.
 * Note that this isn't where the invocation ack is tracked, just the object which builds that message and ack tracking
 * mechanism (by requesting it in the underlying PassthroughConnection).
 */
public class PassthroughInvocationBuilder<M extends EntityMessage, R extends EntityResponse> implements InvocationBuilder<M, R> {
  private final PassthroughConnection connection;
  private final String entityClassName;
  private final String entityName;
  private final long clientInstanceID;
  private final MessageCodec<M, R> messageCodec;
  
  private boolean shouldWaitForSent;
  private boolean shouldWaitForReceived;
  private boolean shouldWaitForCompleted;
  private boolean shouldWaitForRetired;
  private boolean shouldReplicate;
  private boolean shouldBlockGetUntilRetire;
  private boolean deferredResponse;
  private M request;
  private InvokeMonitor<R> monitor;
  private Executor executor;
  
  public PassthroughInvocationBuilder(PassthroughConnection connection, String entityClassName, String entityName, long clientInstanceID, MessageCodec<M, R> messageCodec) {
    this.connection = connection;
    this.entityClassName = entityClassName;
    this.entityName = entityName;
    this.clientInstanceID = clientInstanceID;
    this.messageCodec = messageCodec;
    
    // We want to replicate, by default.
    this.shouldReplicate = true;
    
    // By default, get() will block on RETIRED.
    this.shouldBlockGetUntilRetire = true;
  }

  @Override
  public InvocationBuilder<M, R> ackSent() {
    this.shouldWaitForSent = true;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> ackReceived() {
    this.shouldWaitForReceived = true;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> ackCompleted() {
    this.shouldWaitForCompleted = true;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> ackRetired() {
    this.shouldWaitForRetired = true;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> replicate(boolean requiresReplication) {
    this.shouldReplicate = requiresReplication;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> message(M message) {
    this.request = message;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> monitor(InvokeMonitor<R> consumer) {
    this.monitor = consumer;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> withExecutor(Executor exctr) {
    this.executor = exctr;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> asDeferredResponse() {
    this.deferredResponse = true;
    this.shouldBlockGetUntilRetire = true;
    return this;
  }

  @Override
  public InvocationBuilder<M, R> blockGetOnRetire(boolean shouldBlock) {
    this.shouldBlockGetUntilRetire = shouldBlock;
    return this;
  }

  @Override
  public InvokeFuture<R> invokeWithTimeout(long time, TimeUnit units) throws InterruptedException, TimeoutException, MessageCodecException {
    return invoke();
  }

  @Override
  public InvokeFuture<R> invoke() throws MessageCodecException {
    final PassthroughMessage message = PassthroughMessageCodec.createInvokeMessage(this.entityClassName, this.entityName, this.clientInstanceID, messageCodec.encodeMessage(this.request), this.shouldReplicate);
    final Future<byte[]> invokeFuture = this.connection.invokeActionAndWaitForAcks(message, 
        this.shouldWaitForSent, this.shouldWaitForReceived, this.shouldWaitForCompleted, 
        this.shouldWaitForRetired, this.shouldBlockGetUntilRetire, this.deferredResponse, new PassthroughMonitor<>(messageCodec, monitor, executor));
    return new InvokeFuture<R>() {
      @Override
      public boolean isDone() {
        return invokeFuture.isDone();
      }

      @Override
      public R get() throws InterruptedException, EntityException {
        try {
          return messageCodec.decodeResponse(invokeFuture.get());
        } catch (MessageCodecException e) {
          throw new EntityServerException(null, null, null, e);
        } catch (ExecutionException e) {
          throw (EntityException)e.getCause();
        }
      }

      @Override
      public R getWithTimeout(long timeout, TimeUnit unit) throws InterruptedException, EntityException, TimeoutException {
        try {
          return messageCodec.decodeResponse(invokeFuture.get(timeout, unit));
        } catch (MessageCodecException e) {
          throw new EntityServerException(null, null, null, e);
        } catch (ExecutionException e) {
          throw (EntityException)e.getCause();
        }
      }

      @Override
      public void interrupt() {
        invokeFuture.cancel(true);
      }
    };
  }

}
