package com.zorm.integrator;

import java.util.LinkedHashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.config.BeanValidationIntegrator;
import com.zorm.service.ClassLoaderService;

public class IntegratorServiceImpl implements IntegratorService{

	private static final Log log = LogFactory.getLog(IntegratorServiceImpl.class);
	private final LinkedHashSet<Integrator> integrators = new LinkedHashSet<Integrator>();
	
	public IntegratorServiceImpl(LinkedHashSet<Integrator> providedIntegrators, ClassLoaderService classLoaderService) {
       addIntegrator(new BeanValidationIntegrator());

       for ( Integrator integrator : providedIntegrators ) {
    		addIntegrator( integrator );
    	}
	}
	
	private void addIntegrator(Integrator integrator) {
 		log.debug("Adding Integrator [%s]."+integrator.getClass().getName() );
 		integrators.add(integrator);
	}

	public Iterable<Integrator> getIntegrators() {
		return integrators;
	}

}
