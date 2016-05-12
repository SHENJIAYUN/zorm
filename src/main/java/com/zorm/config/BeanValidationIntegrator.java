package com.zorm.config;

import com.zorm.integrator.Integrator;
import com.zorm.meta.MetadataImplementor;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionFactoryServiceRegistry;

public class BeanValidationIntegrator implements Integrator {

	public void integrate(Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		
	}




	@Override
	public void integrate(com.zorm.meta.MetadataImplementor metadata,
			com.zorm.session.SessionFactoryImplementor sessionFactory,
			com.zorm.session.SessionFactoryServiceRegistry serviceRegistry) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disintegrate(
			com.zorm.session.SessionFactoryImplementor sessionFactory,
			com.zorm.session.SessionFactoryServiceRegistry serviceRegistry) {
		// TODO Auto-generated method stub
		
	}

}
