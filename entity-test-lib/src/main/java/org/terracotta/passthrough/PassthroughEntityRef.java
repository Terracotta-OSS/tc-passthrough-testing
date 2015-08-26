package org.terracotta.passthrough;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;


public class PassthroughEntityRef<T extends Entity, C> implements EntityRef<T, C> {
  private final PassthroughConnection passthroughConnection;
  private final Class<T> clazz;
  private final long version;
  private final String name;
  
  public PassthroughEntityRef(PassthroughConnection passthroughConnection, Class<T> clazz, long version, String name) {
    this.passthroughConnection = passthroughConnection;
    this.clazz = clazz;
    this.version = version;
    this.name = name;
  }

  @Override
  public T fetchEntity() {
    long clientInstanceID = this.passthroughConnection.getNewInstanceID();
    PassthroughMessage getMessage = PassthroughMessageCodec.createFetchMessage(this.clazz, this.name, clientInstanceID, this.version);
    Future<byte[]> received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
    // Wait for the config on the response.
    byte[] rawConfig = null;
    try {
      rawConfig = received.get();
    } catch (InterruptedException e) {
      Assert.fail(e);
    } catch (ExecutionException e) {
      // This is the actual failure case.
      throw new IllegalStateException(e);
    }
    return this.passthroughConnection.createEntityInstance(this.clazz, this.name, clientInstanceID, this.version, rawConfig);
  }

  @Override
  public String getName() {
    return this.name;
  }
}
