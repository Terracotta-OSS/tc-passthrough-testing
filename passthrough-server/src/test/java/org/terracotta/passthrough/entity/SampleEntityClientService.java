package org.terracotta.passthrough.entity;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;

public class SampleEntityClientService implements EntityClientService<SampleEntity, Object, SampleEntityMessage, SampleEntityResponse, Object> {
  @Override
  public boolean handlesEntityType(Class<SampleEntity> cls) {
    return cls.equals(SampleEntity.class);
  }

  @Override
  public byte[] serializeConfiguration(Object configuration) {
    return new byte[0];
  }

  @Override
  public Object deserializeConfiguration(byte[] configuration) {
    return null;
  }

  @Override
  public SampleEntity create(EntityClientEndpoint<SampleEntityMessage, SampleEntityResponse> endpoint, Object userData) {
    return new SampleEntityClient(endpoint);
  }

  @Override
  public MessageCodec<SampleEntityMessage, SampleEntityResponse> getMessageCodec() {
    return new SampleEntityMessageCodec();
  }
}
