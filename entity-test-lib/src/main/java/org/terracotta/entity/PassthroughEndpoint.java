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
package org.terracotta.entity;

import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityServerException;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.CompletableFuture.completedFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Used for basic testing (not part of the in-process testing framework) of an entity, to act like the client end-point.
 */
public class PassthroughEndpoint<M extends EntityMessage, R extends EntityResponse> implements
  TxIdAwareClientEndpoint<M, R> {
  private final ClientDescriptor clientDescriptor = new FakeClientDescriptor();
  private ActiveServerEntity<M, R> entity;
  private MessageCodec<M, R> codec;
  private byte[] configuration;
  private EndpointDelegate delegate;
  private final ClientCommunicator clientCommunicator = new TestClientCommunicator();
  private boolean isOpen;
  private AtomicLong idGenerator = new AtomicLong(0);
  private volatile long eldest = -1L;
  private ConcurrencyStrategy<M> concurrencyStrategy;
  private InvokeMonitor monitor;

  public PassthroughEndpoint() {
    // We start in the open state.
    this.isOpen = true;
  }

  public void attach(ActiveServerEntity<M, R> entity,
                     MessageCodec<M, R> codec,
                     ConcurrencyStrategy<M> concurrencyStrategy,
                     byte[] config) {
    this.entity = entity;
    this.concurrencyStrategy = concurrencyStrategy;
    this.codec = codec;
    this.configuration = config;
    entity.connected(clientDescriptor);
  }

  @Override
  public byte[] getEntityConfiguration() {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen();
    return configuration;
  }

  @Override
  public void setDelegate(EndpointDelegate delegate) {
    // This is harmless while closed but shouldn't be called so check open.
    checkEndpointOpen();
    Assert.assertNull(this.delegate);
    this.delegate = delegate;
  }

  @Override
  public InvocationBuilder<M, R> beginInvoke() {
    // We can't create new invocations when the endpoint is closed.
    checkEndpointOpen();
    return new InvocationBuilderImpl();
  }

  @Override
  public AsyncInvocationBuilder<M, R> beginAsyncInvoke() {
    throw new UnsupportedOperationException("implement me");
  }

  private class FakeClientDescriptor implements ClientDescriptor {
    @Override
    public ClientSourceId getSourceId() {
      return null;
    }
  }

  private class InvocationBuilderImpl implements InvocationBuilder<M, R> {
    private M request = null;

    @Override
    public InvocationBuilder<M, R> ackSent() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackReceived() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackCompleted() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> ackRetired() {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> replicate(boolean requiresReplication) {
      // Replication ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> message(M message) {
      this.request = message;
      return this;
    }

    @Override
    public InvocationBuilder<M, R> blockGetOnRetire(boolean shouldBlock) {
      // ACKs ignored in this implementation.
      return this;
    }

    @Override
    public InvocationBuilder<M, R> monitor(InvokeMonitor<R> consumer) {
      monitor = consumer;
      return this;
    }

    @Override
    public InvocationBuilder<M, R> withExecutor(Executor exctr) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Deprecated @Override
    public InvocationBuilder<M, R> asDeferredResponse() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public InvokeFuture<R> invoke() throws MessageCodecException {
      // Note that the passthrough end-point wants to preserve the semantics of a single-threaded server, no matter how
      // complicated the caller is (since multiple threads often are used to simulate multiple clients or multiple threads
      // using one client).
      // We will synchronize on the entity instance so it will only ever see one caller at a time, no matter how many
      // end-points connect to it.
      synchronized (entity) {
        byte[] result = null;
        EntityException error = null;
        try {
          result = sendInvocation(codec.encodeMessage(request), monitor);
        } catch (EntityException ee) {
          error = ee;
        }
        monitor = null;
        return new ImmediateInvokeFuture<>(codec.decodeResponse(result), error);
      }
    }

    @Override
    public InvokeFuture<R> invokeWithTimeout(long time, TimeUnit units) throws InterruptedException, TimeoutException, MessageCodecException {
      return invoke();
    }
    
    private byte[] sendInvocation(byte[] payload, InvokeMonitor<R> monitor) throws EntityException {
      byte[] result = null;
      try {
        M message = codec.decodeMessage(payload);
        int key = concurrencyStrategy.concurrencyKey(message);
        R response = entity.invokeActive(new PassThroughEntityActiveInvokeContext<R>(clientDescriptor,
                                                                                  key,
                                                                                  idGenerator.incrementAndGet(),
                                                                                  eldest, monitor), message);
        result = codec.encodeResponse(response);
      } catch (Exception e) {
        throw new EntityServerException(null, null, null, e);
      }
      return result;
    }
  }

  public ClientCommunicator clientCommunicator() {
    return clientCommunicator;
  }

  private class TestClientCommunicator implements ClientCommunicator {
    @Override
    public void sendNoResponse(ClientDescriptor clientDescriptor, EntityResponse message) {
      if (clientDescriptor == PassthroughEndpoint.this.clientDescriptor) {
        if (null != PassthroughEndpoint.this.delegate) {
          try {
            // We will encode and decode the message, to simulate how this would function over a network.
            @SuppressWarnings("unchecked")
            byte[] payload = PassthroughEndpoint.this.codec.encodeResponse((R)message);
            R fromServer = PassthroughEndpoint.this.codec.decodeResponse(payload);
            PassthroughEndpoint.this.delegate.handleMessage(fromServer);
          } catch (MessageCodecException e) {
            // Unexpected in this test.
            Assert.fail();
          }
        }
      }
    }

    @Override
    public void closeClientConnection(ClientDescriptor clientDescriptor) {
      close();
    }
  }

  @Override
  public void close() {
    // We can't close twice.
    checkEndpointOpen();
    this.isOpen = false;
    // In a real implementation, this is where a call to the PlatformService, to clean up, would be.
  }

  @Override
  public Future<Void> release() {
    close();
    return completedFuture(null);
  }

  private void checkEndpointOpen() {
    if (!this.isOpen) {
      throw new IllegalStateException("Endpoint closed");
    }
  }

  public long getCurrentId() {
    return idGenerator.get();
  }

  public long resetEldestId() {
    eldest = idGenerator.get();
    return eldest;
  }
}
