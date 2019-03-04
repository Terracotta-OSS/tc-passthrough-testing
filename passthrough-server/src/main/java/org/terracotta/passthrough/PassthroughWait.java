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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.RuntimeEntityException;


/**
 * Used by the client-side message processing to handle the synchronous nature of the messaging system.  This expects the
 * client code's thread to block on acks or completion, and be unblocked by the client-send message processing thread
 * processing the corresponding acks and completion messages.
 */
public class PassthroughWait implements Future<byte[]> {
  // Save the information used to reset this object on resend.
  private byte[] rawMessageForResend;
  private final boolean shouldWaitForReceived;
  private final boolean shouldWaitForCompleted;
  private final boolean shouldWaitForRetired;
  // Note that the point where we wait for acks isn't exposed outside the InvokeFuture interface so this set of waiting
  // threads only applies to those threads waiting to get a response.
  private final Set<Thread> waitingThreads;
  
  // The active state of the instance after the send.
  private boolean waitingForSent;
  private boolean waitingForReceive;
  private boolean waitingForComplete;
  private boolean waitingForRetired;
  private boolean forceGetToBlockOnRetire;
  private boolean canGetReturn;
  private byte[] response;
  private EntityException checkedException;
  private RuntimeEntityException uncheckedException;
  private final PassthroughMonitor monitor;

  public PassthroughWait(boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted, boolean shouldWaitForRetired, boolean forceGetToBlockOnRetire, PassthroughMonitor monitor) {
    this.shouldWaitForReceived = shouldWaitForReceived;
    this.shouldWaitForCompleted = shouldWaitForCompleted;
    this.shouldWaitForRetired = shouldWaitForRetired;
    this.waitingThreads = new HashSet<Thread>();
    
    this.waitingForSent = shouldWaitForSent;
    this.waitingForReceive = shouldWaitForReceived;
    this.waitingForComplete = shouldWaitForCompleted;
    this.waitingForRetired = shouldWaitForRetired;
    this.forceGetToBlockOnRetire = forceGetToBlockOnRetire;
    this.canGetReturn = false;
    this.response = null;
    this.monitor = monitor;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    this.interrupt();
    return true;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }
  
  public synchronized void waitForAck() {
    boolean interrupted = false;
    while (this.waitingForSent || this.waitingForReceive || this.waitingForComplete || this.waitingForRetired) {
      try {
        wait();
      } catch (InterruptedException e) {
        // Note that we can't be interrupted when waiting for acks 
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  public synchronized void interrupt() {
    for (Thread waitingThread : this.waitingThreads) {
      waitingThread.interrupt();
    }
  }

  @Override
  public synchronized boolean isDone() {
    return this.canGetReturn;
  }

  @Override
  public byte[] get() throws InterruptedException, ExecutionException {
    try {
      return waitForCompletion(0, TimeUnit.MILLISECONDS);
    } catch (TimeoutException te) {
      Assert.unexpected(te);
      return null;
    } catch (EntityException e) {
      throw new ExecutionException(e);
    }
  }
  
  private synchronized byte[] waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException, EntityException, TimeoutException {
    Thread callingThread = Thread.currentThread();
    boolean didAdd = this.waitingThreads.add(callingThread);
    // We can't have already been waiting.
    Assert.assertTrue(didAdd);
    long end = (timeout > 0) ? System.currentTimeMillis() + unit.toMillis(timeout) : Long.MAX_VALUE;
    try {
      while (!this.canGetReturn && System.currentTimeMillis() < end) {
        this.wait(unit.toMillis(timeout));
      }
      if (!this.canGetReturn) {
        throw new TimeoutException();
      }
    } finally {
      // We will hit this path on interrupt, for example.
      this.waitingThreads.remove(callingThread);
    }
    if (null != this.checkedException) {
      throw this.checkedException;
    }
    if (null != this.uncheckedException) {
      throw this.uncheckedException;
    }
    return this.response;
  }

  public byte[] get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    try {
      return waitForCompletion(timeout, unit);
    } catch (EntityException e) {
      throw new ExecutionException(e);
    }
  }

  public synchronized void sent() {
    this.waitingForSent = false;
    notifyAll();
  }

  public synchronized void handleAck() {
    this.waitingForReceive = false;
    notifyAll();
  }

  public synchronized void handleComplete(byte[] result, EntityException error) {
    this.waitingForComplete = false;
    this.response = result;
    this.checkedException = error;
    if (!this.forceGetToBlockOnRetire) {
      this.canGetReturn = true;
    }
    notifyAll();
  }
  
  public synchronized void handleMonitor(byte[] result) {
    if (result != null) monitor.sendResponse(result);
  }
  
  public synchronized void handleRetire() {
    this.waitingForRetired = false;
    // The retire is the last message so we want to set us to done.
    this.canGetReturn = true;
    notifyAll();
  }

  public void saveRawMessageForResend(byte[] raw) {
    this.rawMessageForResend = raw;
  }

  /**
   * Resets the ACK wait state for the receiver and returns the raw message for the caller to re-send.
   */
  public synchronized byte[] resetAndGetMessageForResend() {
    this.waitingForReceive = this.shouldWaitForReceived;
    this.waitingForComplete = this.shouldWaitForCompleted;
    this.waitingForRetired = this.shouldWaitForRetired;
    this.canGetReturn = false;
    this.response = null;
    this.checkedException = null;
    return this.rawMessageForResend;
  }

  public synchronized void forceDisconnect() {
    this.waitingForComplete = false;
    this.uncheckedException = new ConnectionClosedException("Connection closed");
    this.canGetReturn = true;
    notifyAll();
  }

  public void blockGetOnRetire() {
    this.forceGetToBlockOnRetire = true;
  }
}
