package org.terracotta.passthrough;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;

public class PassThroughServerInvokeContext implements ActiveInvokeContext {
  private final ClientDescriptor descriptor;
  private final long current;
  private final long oldest;

  public PassThroughServerInvokeContext(ClientDescriptor descriptor, long current, long oldest) {
    this.descriptor = descriptor;
    this.current = current;
    this.oldest = oldest;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return descriptor;
  }

  @Override
  public long getCurrentTransactionId() {
    return current;
  }

  @Override
  public long getOldestTransactionId() {
    return oldest;
  }

  @Override
  public boolean isValidClientInformation() {
    return current >= 0;
  }

  @Override
  public ClientSourceId getClientSource() {
    return descriptor.getSourceId();
  }

  @Override
  public ClientSourceId makeClientSourceId(long opaque) {
    return new ClientSourceId() {
      @Override
      public long toLong() {
        return opaque;
      }

      @Override
      public boolean matches(ClientDescriptor cd) {
        return cd.getSourceId().toLong() == opaque;
      }
    };
  }
  
  
}
