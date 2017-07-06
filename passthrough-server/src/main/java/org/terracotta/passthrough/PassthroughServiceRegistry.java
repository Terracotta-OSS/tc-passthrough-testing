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

import java.util.List;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;

import java.util.ArrayList;
import java.util.Collections;
import org.terracotta.entity.ServiceException;


/**
 * The registry of services available on a PassthroughServer.
 */
public class PassthroughServiceRegistry implements ServiceRegistry {
  private final String entityClassName;
  private final String entityName;
  private final long consumerID;
  private final List<ServiceProvider> serviceProviders;
  private final List<ServiceProvider> overrideServiceProviders;
  private final List<PassthroughImplementationProvidedServiceProvider> implementationProvidedServiceProviders;
  private final DeferredEntityContainer owningEntityContainer;
  
  public PassthroughServiceRegistry(String entityClassName, String entityName, long consumerID,
      List<ServiceProvider> serviceProviders, List<ServiceProvider> overrideServiceProviders,
      List<PassthroughImplementationProvidedServiceProvider> implementationProvidedServiceProviders, DeferredEntityContainer container) {
    this.entityClassName = entityClassName;
    this.entityName = entityName;
    this.consumerID = consumerID;

    this.serviceProviders = Collections.unmodifiableList(new ArrayList<ServiceProvider>(serviceProviders));
    this.overrideServiceProviders = Collections.unmodifiableList(new ArrayList<ServiceProvider>(overrideServiceProviders));

    this.implementationProvidedServiceProviders = Collections.unmodifiableList(implementationProvidedServiceProviders);
    
    this.owningEntityContainer = container;
  }

  @Override
  public <T> T getService(ServiceConfiguration<T> configuration) throws ServiceException {
    List<T> services = getServices(configuration);

    switch (services.size()) {
      case 0:
        return null;
      case 1:
        return services.get(0);
      default:
        throw new ServiceException("multiple services defined");
    }
  }

  @Override
  public <T> List<T> getServices(ServiceConfiguration<T> configuration) {
    List<T> overrides = getOverrides(configuration);
    if (!overrides.isEmpty()) {
      return overrides;
    }

    List<T> services = new ArrayList<T>();
    services.addAll(getBuiltIns(configuration));
    services.addAll(getExternals(configuration));

    return services;
  }

  private <T> List<T> getBuiltIns(ServiceConfiguration<T> configuration) {
    List<T> services = new ArrayList<T>();

    for (PassthroughImplementationProvidedServiceProvider provider : this.implementationProvidedServiceProviders) {
      if (provider.getProvidedServiceTypes().contains(configuration.getServiceType())) {
        T service = provider.getService(this.entityClassName, this.entityName, this.consumerID, this.owningEntityContainer, configuration);
        if (service != null) {
          services.add(service);
        }
      }
    }

    return services;
  }

  private <T> List<T> getExternals(ServiceConfiguration<T> configuration) {
    return getUserServices(this.serviceProviders, configuration);
  }

  private <T> List<T> getOverrides(ServiceConfiguration<T> configuration) {
    return getUserServices(this.overrideServiceProviders, configuration);
  }

  private <T> List<T> getUserServices(List<ServiceProvider> providers, ServiceConfiguration<T> configuration) {
    List<T> services = new ArrayList<T>();

    for (ServiceProvider provider : providers) {
      if (provider.getProvidedServiceTypes().contains(configuration.getServiceType())) {
        T service = provider.getService(this.consumerID, configuration);
        if (service != null) {
          services.add(service);
        }
      }
    }

    return services;
  }
  
  public long getConsumerID() {
    return consumerID;
  }
}
