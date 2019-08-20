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
package org.terracotta.passthrough;

import org.awaitility.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.passthrough.entity.SampleActiveServerEntity;
import org.terracotta.passthrough.entity.SampleEntity;
import org.terracotta.passthrough.entity.SampleEntityClient;
import org.terracotta.passthrough.entity.SampleEntityClientService;
import org.terracotta.passthrough.entity.SampleEntityMessage;
import org.terracotta.passthrough.entity.SampleEntityResponse;
import org.terracotta.passthrough.entity.SampleEntityServerService;

import java.net.URI;
import java.util.Properties;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AsyncInvokeTest {

  private PassthroughClusterControl stripeControl;

  @Before
  public void setUp() throws Exception {
    stripeControl = PassthroughTestHelpers.createMultiServerStripe("stripe-1", 1, server -> {
      server.registerClientEntityService(new SampleEntityClientService());
      server.registerServerEntityService(new SampleEntityServerService());
    });
    stripeControl.waitForActive();
  }

  @After
  public void tearDown() throws Exception {
    if (stripeControl != null) {
      stripeControl.tearDown();
      stripeControl = null;
    }
  }

  @Test
  public void testEnd2End() throws Exception {
    try (Connection connection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/"), new Properties())) {
      EntityRef<SampleEntity, Object, Object> entityRef = connection.getEntityRef(SampleEntity.class, 1L, "sample-entity");
      entityRef.create(null);
      SampleEntity sampleEntity = entityRef.fetchEntity(null);

      sampleEntity.doSomething();

      // wait for all async events
      await().atMost(Duration.FIVE_SECONDS).until(SampleEntityClient.CALLBACK.events::size, is(5));

      // assert all events individually
      int i = 0;
      assertThat(SampleEntityClient.CALLBACK.events.get(i++), is(SampleEntityClient.Event.SENT));
      assertThat(SampleEntityClient.CALLBACK.events.get(i++), is(SampleEntityClient.Event.RECEIVED));
      assertThat(SampleEntityClient.CALLBACK.events.get(i++), instanceOf(SampleEntityResponse.class));
      assertThat(SampleEntityClient.CALLBACK.events.get(i++), is(SampleEntityClient.Event.COMPLETE));
      assertThat(SampleEntityClient.CALLBACK.events.get(i++), is(SampleEntityClient.Event.RETIRED));

      // check that the server received the message
      assertThat(SampleActiveServerEntity.INVOCATIONS.size(), is(1));
      assertThat(SampleActiveServerEntity.INVOCATIONS.get(0), instanceOf(SampleEntityMessage.class));
    }
  }

}
