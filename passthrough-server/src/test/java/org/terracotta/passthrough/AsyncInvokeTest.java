package org.terracotta.passthrough;

import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.passthrough.entity.SampleEntity;
import org.terracotta.passthrough.entity.SampleEntityClientService;
import org.terracotta.passthrough.entity.SampleEntityServerService;

import java.net.URI;
import java.util.Properties;

public class AsyncInvokeTest {

  @Test
  public void name() throws Exception {
    PassthroughClusterControl stripeControl;
    stripeControl = PassthroughTestHelpers.createMultiServerStripe("stripe-1", 1, server -> {
      server.registerClientEntityService(new SampleEntityClientService());
      server.registerServerEntityService(new SampleEntityServerService());
    });
    stripeControl.waitForActive();


    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, getClass().getSimpleName());
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    Connection connection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/"), properties);

    EntityRef<SampleEntity, Object, Object> entityRef = connection.getEntityRef(SampleEntity.class, 1L, "sample-entity");
    entityRef.create(null);
    SampleEntity sampleEntity = entityRef.fetchEntity(null);

    sampleEntity.doSomething();

    Thread.sleep(1000);

    connection.close();


    stripeControl.tearDown();
  }

}
