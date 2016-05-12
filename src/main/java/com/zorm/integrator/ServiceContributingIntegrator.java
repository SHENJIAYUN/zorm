package com.zorm.integrator;

import com.zorm.service.ServiceRegistryBuilder;

public interface ServiceContributingIntegrator extends Integrator{
	/**
	 * Allow the integrator to alter the builder of {@link org.hibernate.service.ServiceRegistry}, presumably to
	 * register services into it.
	 *
	 * @param serviceRegistryBuilder The build to prepare.
	 */
	public void prepareServices(ServiceRegistryBuilder serviceRegistryBuilder);
}
