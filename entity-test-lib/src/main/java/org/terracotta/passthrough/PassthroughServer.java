package org.terracotta.passthrough;

import java.util.List;
import java.util.Vector;

import org.junit.Assert;
import org.terracotta.connection.Connection;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceProvider;


public class PassthroughServer {
  private final PassthroughServerProcess serverProcess;
  private boolean hasStarted;
  private final List<EntityClientService<?, ?>> entityClientServices;
  
  public PassthroughServer() {
    this.serverProcess = new PassthroughServerProcess();
    this.entityClientServices = new Vector<>();
  }

  public void registerServerEntityService(ServerEntityService<?, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.serverProcess.registerEntityService(service);
  }

  public void registerClientEntityService(EntityClientService<?, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.entityClientServices.add(service);
  }

  public Connection connectNewClient() {
    Assert.assertTrue(this.hasStarted);
    return new PassthroughConnection(this.serverProcess, this.entityClientServices);
  }
  
  public void start() {
    this.hasStarted = true;
    this.serverProcess.start();
  }

  public <T> void registerServiceProviderForType(Class<T> clazz, ServiceProvider serviceProvider) {
    this.serverProcess.registerServiceProviderForType(clazz, serviceProvider);
  }
}
