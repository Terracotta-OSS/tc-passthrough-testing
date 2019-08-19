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
import org.terracotta.entity.InvocationCallback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SampleEntityClient implements SampleEntity {
  public static final SampleEntityClientInvocationCallback CALLBACK = new SampleEntityClientInvocationCallback();

  private final EntityClientEndpoint<SampleEntityMessage, SampleEntityResponse> endpoint;

  SampleEntityClient(EntityClientEndpoint<SampleEntityMessage, SampleEntityResponse> endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public void doSomething() {
    endpoint.beginAsyncInvoke().message(new SampleEntityMessage()).invoke(CALLBACK);
  }

  @Override
  public void close() {
  }

  public enum Event {
    SENT,
    RECEIVED,
    COMPLETE,
    RETIRED,
  }

  public static class SampleEntityClientInvocationCallback implements InvocationCallback<SampleEntityResponse> {

    public final List<Object> events = new CopyOnWriteArrayList<>();

    @Override
    public void sent() {
      events.add(Event.SENT);
    }

    @Override
    public void received() {
      events.add(Event.RECEIVED);
    }

    @Override
    public void result(SampleEntityResponse response) {
      events.add(response);
    }

    @Override
    public void failure(Throwable failure) {
      events.add(failure);
    }

    @Override
    public void complete() {
      events.add(Event.COMPLETE);
    }

    @Override
    public void retired() {
      events.add(Event.RETIRED);
    }
  }
}
