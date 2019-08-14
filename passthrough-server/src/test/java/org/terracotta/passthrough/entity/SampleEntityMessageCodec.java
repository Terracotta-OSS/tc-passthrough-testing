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
