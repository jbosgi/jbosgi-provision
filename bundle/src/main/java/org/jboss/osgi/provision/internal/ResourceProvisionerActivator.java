/*
 * #%L
 * JBossOSGi Provision: Bundle
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.jboss.osgi.provision.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jboss.osgi.provision.AbstractResourceProvisioner;
import org.jboss.osgi.provision.XResourceProvisioner;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.resolver.XResource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The Provision Service
 *
 * @author thomas.diesler@jboss.com
 * @since 06-May-2013
 */
public class ResourceProvisionerActivator implements BundleActivator {

    private ServiceTracker<XResolver, XResolver> resolverTracker;
    private ServiceTracker<XRepository, XRepository> repositoryTracker;
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    @Override
    public void start(final BundleContext context) throws Exception {
        resolverTracker = new ServiceTracker<XResolver, XResolver>(context, XResolver.class, null) {
            @Override
            public XResolver addingService(ServiceReference<XResolver> reference) {
                XResolver resolver = super.addingService(reference);
                createProvisionService(context, resolver, repositoryTracker.getService());
                return resolver;
            }
        };

        repositoryTracker = new ServiceTracker<XRepository, XRepository>(context, XRepository.class, null) {
            @Override
            public XRepository addingService(ServiceReference<XRepository> reference) {
                XRepository repository = super.addingService(reference);
                createProvisionService(context, resolverTracker.getService(), repository);
                return repository;
            }
        };

        resolverTracker.open();
        repositoryTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (resolverTracker != null)
            resolverTracker.close();
        if (repositoryTracker != null)
            repositoryTracker.close();
        for (ServiceRegistration<?> reg : registrations) {
            reg.unregister();
        }
    }

    private void createProvisionService(final BundleContext context, final XResolver resolver, final XRepository repository) {
        if (resolver != null && repository != null) {
            XResourceProvisioner provisioner = new AbstractResourceProvisioner(resolver, repository);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("type", XResource.TYPE_BUNDLE);
            registrations.add(context.registerService(XResourceProvisioner.class, provisioner, props));
        }
    }
}
