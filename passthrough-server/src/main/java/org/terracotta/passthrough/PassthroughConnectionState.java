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

import java.util.HashMap;
import java.util.Map;


/**
 * Maintains the association that a connection has to its server and the in-flight messages associated with it.
 * The reason why this is managed out-of-line is that reconnect represents very specific ordering concerns and thread
 * interaction concerns, which are far more easily managed within a distinct object.
 * Note that all the methods in this object are synchronized so they can not block for any reason other than waiting.  That
 * is to say that nothing can perform a blocking operation while holding the monitor.
 */
public class PassthroughConnectionState {
  private PassthroughServerProcess serverProcess;
  private final Map<Long, PassthroughAckProcessor> inFlightMessages;
  // We store the reconnecting server just to assert details of correct usage.
  private PassthroughServerProcess reconnectingServerProcess;
  
  // Transaction IDs are managed here, as well.
  private long nextTransactionID;
  
  public PassthroughConnectionState(PassthroughServerProcess initialServerProcess) {
    this.serverProcess = initialServerProcess;
    this.inFlightMessages = new HashMap<>();
    this.nextTransactionID = 1;
  }
  
  public boolean isServerThread() {
    return serverProcess.isServerThread();
  }

  public synchronized PassthroughWait sendNormal(PassthroughConnection sender, PassthroughMessage message, boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted, boolean shouldWaitForRetired, boolean forceGetToBlockOnRetire, PassthroughMonitor monitor) {
    // This uses the normal server process so wait for it to become available.
    while (null == this.serverProcess) {
      try {
        wait();
      } catch (InterruptedException e) {
        // The only reason we would interrupt is to kill the test.
        throw new RuntimeException(e);
      }
    }
    long oldestTransactionID = this.nextTransactionID;
    for (long oneID : this.inFlightMessages.keySet()) {
      if (oneID < oldestTransactionID) {
        oldestTransactionID = oneID;
      }
    }
    return createAndSend(this.serverProcess, sender, message,
        oldestTransactionID, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted, 
        shouldWaitForRetired, forceGetToBlockOnRetire, monitor);
  }

  private PassthroughWait createAndSend(PassthroughServerProcess target,
                                                PassthroughConnection sender, PassthroughMessage message, long oldestTransactionID,
                                                boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted,
                                                boolean shouldWaitForRetired, boolean forceGetToBlockOnRetire,
                                                PassthroughMonitor monitor
  ) {
    PassthroughWait waiter = new PassthroughWait(shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted, shouldWaitForRetired, forceGetToBlockOnRetire, monitor);
    long transactionID = this.nextTransactionID;
    this.nextTransactionID += 1;
    message.setTransactionTracking(transactionID, oldestTransactionID);
    this.inFlightMessages.put(transactionID, waiter);
    if (shouldWaitForSent) {
      waiter.sent();
    }
    byte[] raw = message.asSerializedBytes();
    waiter.saveRawMessageForResend(raw);
    target.sendMessageToServer(sender, raw);
    return waiter;
  }

  public <M extends EntityMessage, R extends EntityResponse> PassthroughAsyncAckProcessor sendAsync(PassthroughConnection sender, PassthroughMessage message, boolean shouldWaitForSent, MessageCodec<M, R> messageCodec, InvocationCallback<R> invocationCallback) {
    // This uses the normal server process so wait for it to become available.
    while (null == this.serverProcess) {
      try {
        wait();
      } catch (InterruptedException e) {
        // The only reason we would interrupt is to kill the test.
        throw new RuntimeException(e);
      }
    }
    long oldestTransactionID = this.nextTransactionID;
    for (long oneID : this.inFlightMessages.keySet()) {
      if (oneID < oldestTransactionID) {
        oldestTransactionID = oneID;
      }
    }

    return createAndSendAsync(this.serverProcess, sender, message,
        oldestTransactionID, shouldWaitForSent, messageCodec, invocationCallback);
  }

  private <M extends EntityMessage, R extends EntityResponse> PassthroughAsyncAckProcessor createAndSendAsync(PassthroughServerProcess target,
                                                                                                         PassthroughConnection sender, PassthroughMessage message, long oldestTransactionID,
                                                                                                         boolean shouldWaitForSent, MessageCodec<M, R> messageCodec, InvocationCallback<R> invocationCallback) {
    PassthroughAsyncAckProcessor waiter = new PassthroughAsyncAckProcessor(messageCodec, (InvocationCallback<EntityResponse>) invocationCallback);
    long transactionID = this.nextTransactionID;
    this.nextTransactionID += 1;
    message.setTransactionTracking(transactionID, oldestTransactionID);
    this.inFlightMessages.put(transactionID, waiter);
    if (shouldWaitForSent) {
      waiter.sent();
    }
    byte[] raw = message.asSerializedBytes();
    waiter.saveRawMessageForResend(raw);
    target.sendMessageToServer(sender, raw);
    return waiter;
  }


  public synchronized boolean isConnected(PassthroughServerProcess sender) {
    return (sender == this.serverProcess) || (sender == this.reconnectingServerProcess);
  }

  public synchronized PassthroughWait sendAsReconnect(PassthroughConnection sender, PassthroughMessage message, boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted, boolean shouldWaitForRetired, boolean forceGetToBlockOnRetire) {
    // This is similar to the normal send but only happens in the reconnect state and creates a waiter in that in-flight set.
    Assert.assertTrue(null != this.reconnectingServerProcess);
    // We won't bother clearing transactions on re-send.
    long oldestTransactionID = 0;
    return createAndSend(this.reconnectingServerProcess, sender, message, oldestTransactionID, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted, shouldWaitForRetired, forceGetToBlockOnRetire, null);
  }

  public synchronized Map<Long, PassthroughAckProcessor> enterReconnectState(PassthroughServerProcess newServerProcess) {
    Assert.assertTrue(null == this.serverProcess);
    Assert.assertTrue(null == this.reconnectingServerProcess);

    this.reconnectingServerProcess = newServerProcess;
    return this.inFlightMessages;
  }

  public synchronized void sendAsResend(PassthroughConnection sender, long transactionID, PassthroughAckProcessor waiter) {
    // This is similar to the normal send but only happens in the reconnect state and creates a waiter in that in-flight set.
    Assert.assertTrue(null != this.reconnectingServerProcess);
    byte[] raw = waiter.resetAndGetMessageForResend();
    this.inFlightMessages.put(transactionID, waiter);
    // We always want to block on retire, when doing a re-send.
    waiter.blockGetOnRetire();
    this.reconnectingServerProcess.sendMessageToServer(sender, raw);
  }

  public synchronized PassthroughAckProcessor getWaiterForTransaction(PassthroughServerProcess sender, long transactionID) {
    PassthroughAckProcessor waiter = this.inFlightMessages.get(transactionID);
    Assert.assertTrue(null != waiter);
    return waiter;
  }

  public synchronized PassthroughAckProcessor removeWaiterForTransaction(PassthroughServerProcess sender, long transactionID) {
    PassthroughAckProcessor waiter = this.inFlightMessages.remove(transactionID);
    Assert.assertTrue(null != waiter);
    return waiter;
  }

  public synchronized void finishReconnectState() {
    Assert.assertTrue(null == this.serverProcess);
    Assert.assertTrue(null != this.reconnectingServerProcess);
    
    this.serverProcess = this.reconnectingServerProcess;
    this.reconnectingServerProcess = null;
    notifyAll();
  }

  public synchronized void enterDisconnectedState() {
    Assert.assertTrue(null != this.serverProcess);

    this.serverProcess = null;
  }

  public synchronized void forceClose() {
    for (PassthroughAckProcessor waiter : this.inFlightMessages.values()) {
      waiter.forceDisconnect();
    }
  }
}
