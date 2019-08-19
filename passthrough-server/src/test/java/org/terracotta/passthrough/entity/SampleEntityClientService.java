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
