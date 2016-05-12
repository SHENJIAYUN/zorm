package com.zorm.integrator;

import com.zorm.service.Service;

public interface IntegratorService extends Service{
	/**
	 * Retrieve all integrators.
	 *
	 * @return All integrators.
	 */
	public Iterable<Integrator> getIntegrators();
}
