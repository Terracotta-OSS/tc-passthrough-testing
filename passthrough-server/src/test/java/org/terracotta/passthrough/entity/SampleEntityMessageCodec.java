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

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

public class SampleEntityMessageCodec implements MessageCodec<SampleEntityMessage, SampleEntityResponse> {
  @Override
  public byte[] encodeMessage(SampleEntityMessage entityMessage) throws MessageCodecException {
    return new byte[0];
  }

  @Override
  public SampleEntityMessage decodeMessage(byte[] bytes) throws MessageCodecException {
    return new SampleEntityMessage();
  }

  @Override
  public byte[] encodeResponse(SampleEntityResponse response) throws MessageCodecException {
    return new byte[0];
  }

  @Override
  public SampleEntityResponse decodeResponse(byte[] bytes) throws MessageCodecException {
    return new SampleEntityResponse();
  }
}
