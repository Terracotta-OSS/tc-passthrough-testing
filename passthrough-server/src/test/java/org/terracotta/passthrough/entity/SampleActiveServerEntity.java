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
package org.terracotta.passthrough.entity;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.PassiveSynchronizationChannel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SampleActiveServerEntity implements ActiveServerEntity<SampleEntityMessage, SampleEntityResponse> {

  public static final List<SampleEntityMessage> INVOCATIONS = new CopyOnWriteArrayList<>();


  @Override
  public SampleEntityResponse invokeActive(ActiveInvokeContext<SampleEntityResponse> context, SampleEntityMessage message) throws EntityUserException {
    INVOCATIONS.add(message);
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
