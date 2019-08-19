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

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;

import java.util.Collections;
import java.util.Set;

public class SampleEntityServerService implements EntityServerService<SampleEntityMessage, SampleEntityResponse> {
  @Override
  public long getVersion() {
    return 1L;
  }

  @Override
  public boolean handlesEntityType(String s) {
    return "org.terracotta.passthrough.entity.SampleEntity".equals(s);
  }

  @Override
  public ActiveServerEntity<SampleEntityMessage, SampleEntityResponse> createActiveEntity(ServiceRegistry serviceRegistry, byte[] bytes) throws ConfigurationException {
    return new SampleActiveServerEntity();
  }

  @Override
  public PassiveServerEntity<SampleEntityMessage, SampleEntityResponse> createPassiveEntity(ServiceRegistry serviceRegistry, byte[] bytes) throws ConfigurationException {
    return null;
  }

  @Override
  public ConcurrencyStrategy<SampleEntityMessage> getConcurrencyStrategy(byte[] bytes) {
    return new NoConcurrencyStrategy<>();
  }

  @Override
  public MessageCodec<SampleEntityMessage, SampleEntityResponse> getMessageCodec() {
    return new SampleEntityMessageCodec();
  }

  @Override
  public SyncMessageCodec<SampleEntityMessage> getSyncMessageCodec() {
    return null;
  }

  static class NoConcurrencyStrategy<M extends EntityMessage> implements ConcurrencyStrategy<M> {
    NoConcurrencyStrategy() {
    }

    public int concurrencyKey(M payload) {
      return 0;
    }

    public Set<Integer> getKeysForSynchronization() {
      return Collections.emptySet();
    }
  }

}
