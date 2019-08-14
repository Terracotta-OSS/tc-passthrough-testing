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
