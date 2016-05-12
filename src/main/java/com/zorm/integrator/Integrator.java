package com.zorm.integrator;

import com.zorm.config.Configuration;
import com.zorm.meta.MetadataImplementor;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionFactoryServiceRegistry;

public interface Integrator {
	/**
	 * Perform integration.
	 *
	 * @param configuration The configuration used to create the session factory
	 * @param sessionFactory The session factory being created
	 * @param serviceRegistry The session factory's service registry
	 */
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry);

	/**
     * Perform integration.
     *
     * @param metadata The metadata used to create the session factory
     * @param sessionFactory The session factory being created
     * @param serviceRegistry The session factory's service registry
     */
    public void integrate( MetadataImplementor metadata,
                           SessionFactoryImplementor sessionFactory,
                           SessionFactoryServiceRegistry serviceRegistry );

	/**
	 * Tongue-in-cheek name for a shutdown callback.
	 *
	 * @param sessionFactory The session factory being closed.
	 * @param serviceRegistry That session factory's service registry
	 */
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry);
}
