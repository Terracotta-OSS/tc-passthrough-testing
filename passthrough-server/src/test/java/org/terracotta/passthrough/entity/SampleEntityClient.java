package org.terracotta.passthrough.entity;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationCallback;

public class SampleEntityClient implements SampleEntity {
  private final EntityClientEndpoint<SampleEntityMessage, SampleEntityResponse> endpoint;

  public SampleEntityClient(EntityClientEndpoint<SampleEntityMessage, SampleEntityResponse> endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public void doSomething() {
    endpoint.beginAsyncInvoke().message(new SampleEntityMessage()).invoke(new InvocationCallback<SampleEntityResponse>() {
      @Override
      public void sent() {
        System.out.println("sample entity - sent ack!");
      }

      @Override
      public void received() {
        System.out.println("sample entity - received ack!");
      }

      @Override
      public void result(SampleEntityResponse response) {
        System.out.println("sample entity - result!");
      }

      @Override
      public void failure(Throwable failure) {
        System.out.println("sample entity - failure");
      }

      @Override
      public void complete() {
        System.out.println("sample entity - complete ack!");
      }

      @Override
      public void retired() {
        System.out.println("sample entity - retired ack!");
      }
    });
  }

  @Override
  public void close() {
  }
}
