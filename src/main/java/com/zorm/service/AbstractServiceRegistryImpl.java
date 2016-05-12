package com.zorm.service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.zorm.exception.ServiceDependencyException;
import com.zorm.exception.ServiceException;
import com.zorm.exception.UnknownServiceException;
import com.zorm.util.CollectionHelper;

public abstract class AbstractServiceRegistryImpl implements ServiceRegistryImplementor,ServiceBinding.ServiceLifecycleOwner{

	private final ServiceRegistryImplementor parent;
	
	private final ConcurrentHashMap<Class,ServiceBinding> serviceBindingMap = CollectionHelper.concurrentMap( 20 );
	
	private final List<ServiceBinding> serviceBindingList = CollectionHelper.arrayList( 20 );
	
	public AbstractServiceRegistryImpl(BootstrapServiceRegistry bootstrapServiceRegistry) {
		if ( ! ServiceRegistryImplementor.class.isInstance( bootstrapServiceRegistry ) ) {
			throw new IllegalArgumentException( "Boot-strap registry was not " );
		}
		this.parent = (ServiceRegistryImplementor) bootstrapServiceRegistry;
	}

	public AbstractServiceRegistryImpl(ServiceRegistryImplementor parent) {
       this.parent = parent;
	}

	@SuppressWarnings({ "unchecked" })
	protected <R extends Service> void createServiceBinding(ServiceInitiator<R> initiator) {
		serviceBindingMap.put( initiator.getServiceInitiated(), new ServiceBinding( this, initiator ) );
	}
	
	protected <R extends Service> void createServiceBinding(ProvidedService<R> providedService) {
		ServiceBinding<R> binding = locateServiceBinding( providedService.getServiceRole(), false );
		if ( binding == null ) {
			binding = new ServiceBinding<R>( this, providedService.getServiceRole(), providedService.getService() );
			serviceBindingMap.put( providedService.getServiceRole(), binding );
		}
		registerService( binding, providedService.getService() );
	}
	
	protected <R extends Service> void registerService(ServiceBinding<R> serviceBinding, R service) {
		serviceBinding.setService( service );
		synchronized ( serviceBindingList ) {
			serviceBindingList.add( serviceBinding );
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole, boolean checkParent) {
		ServiceBinding<R> serviceBinding = serviceBindingMap.get( serviceRole );
		if ( serviceBinding == null && checkParent && parent != null ) {
			// look in parent
			serviceBinding = parent.locateServiceBinding( serviceRole );
		}
		return serviceBinding;
	}
	@Override
	public ServiceRegistry getParentServiceRegistry() {
		return null;
	}

	@Override
	public <R extends Service> R getService(Class<R> serviceRole) {
        final ServiceBinding<R> serviceBinding = locateServiceBinding(serviceRole);
        if(serviceBinding==null){
        	throw new UnknownServiceException(serviceRole);
        }
        R service = serviceBinding.getService();
        if(service==null){
        	service = initializeService(serviceBinding);
        }
		return service;
	}

	private <R extends Service> R initializeService(ServiceBinding<R> serviceBinding){
		//阶段1：创建服务
		R service = createService(serviceBinding);
		if(service==null){
			return null;
		}
		
		//阶段2：注入服务
		serviceBinding.getLifecycleOwner().injectDependencies(serviceBinding);
		
		//阶段3：配置服务
		serviceBinding.getLifecycleOwner().configureService(serviceBinding);
		
		//阶段4：启动服务
		serviceBinding.getLifecycleOwner().startService(serviceBinding);
		
		return service;
	}
	
	@Override
	public <R extends Service> void injectDependencies(ServiceBinding<R> serviceBinding){
		final R service = serviceBinding.getService();
		applyInjections(service);
		if ( ServiceRegistryAwareService.class.isInstance( service ) ) {
			( (ServiceRegistryAwareService) service ).injectServices( this );
		}
	}
	
	private <R extends Service> void applyInjections(R service){
		try {
			for ( Method method : service.getClass().getMethods() ) {
				InjectService injectService = method.getAnnotation( InjectService.class );
				if ( injectService == null ) {
					continue;
				}

				processInjection( service, method, injectService );
			}
		}
		catch (NullPointerException e) {
		}
	}
	
	private <T extends Service> void processInjection(T service, Method injectionMethod, InjectService injectService) {
		if ( injectionMethod.getParameterTypes() == null || injectionMethod.getParameterTypes().length != 1 ) {
			throw new ServiceDependencyException(
					"Encountered @InjectService on method with unexpected number of parameters"
			);
		}

		Class dependentServiceRole = injectService.serviceRole();
		if ( dependentServiceRole == null || dependentServiceRole.equals( Void.class ) ) {
			dependentServiceRole = injectionMethod.getParameterTypes()[0];
		}


		final Service dependantService = getService( dependentServiceRole );
		if ( dependantService == null ) {
			if ( injectService.required() ) {
				throw new ServiceDependencyException(
						"Dependency [" + dependentServiceRole + "] declared by service [" + service + "] not found"
				);
			}
		}
		else {
			try {
				injectionMethod.invoke( service, dependantService );
			}
			catch ( Exception e ) {
				throw new ServiceDependencyException( "Cannot inject dependency service", e );
			}
		}
	}
	
	protected <R extends Service> R createService(ServiceBinding<R> serviceBinding){
		final ServiceInitiator<R> serviceInitiator = serviceBinding.getServiceInitiator();
		if(serviceInitiator==null){
			throw new UnknownServiceException(serviceBinding.getServiceRole());
		}
		
		try{
			//返回的是服务的实现
			R service = serviceBinding.getLifecycleOwner().initiateService(serviceInitiator);
		    //将service绑定到serviceBinding中
			registerService(serviceBinding, service);
		    return service;
		}
		catch(ServiceException e){
			throw e;
		}
		catch(Exception e){
			throw new ServiceException( "Unable to create requested service [" + serviceBinding.getServiceRole().getName() + "]", e );
		}
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> binding) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <R extends Service> void startService(ServiceBinding<R> serviceBinding) {
		if(Startable.class.isInstance(serviceBinding.getService())){
			((Startable)serviceBinding.getService()).start();
		}
	}

	@Override
	public <R extends Service> void stopService(ServiceBinding<R> binding) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <R extends Service> ServiceBinding<R> locateServiceBinding(
			Class<R> serviceRole) {
		return locateServiceBinding(serviceRole,true);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
