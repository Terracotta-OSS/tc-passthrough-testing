package org.terracotta.passthrough.entity;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.PassiveSynchronizationChannel;

public class SampleActiveServerEntity implements ActiveServerEntity<SampleEntityMessage, SampleEntityResponse> {
  @Override
  public SampleEntityResponse invokeActive(ActiveInvokeContext<SampleEntityResponse> context, SampleEntityMessage message) throws EntityUserException {
    System.out.println("This is " + getClass().getName() + " and I got a message!");
    return new SampleEntityResponse();
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {

  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {

  }

  @Override
  public void loadExisting() {

  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<SampleEntityMessage> passiveSynchronizationChannel, int i) {

  }

  @Override
  public ReconnectHandler startReconnect() {
    return null;
  }

  @Override
  public void createNew() throws ConfigurationException {

  }

  @Override
  public void destroy() {

  }
}
