package com.zorm.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.zorm.config.AvailableSettings;
import com.zorm.exception.ClassLoadingException;

public class ClassLoaderServiceImpl implements ClassLoaderService {

	private final ClassLoader classClassLoader;
	private final ClassLoader resourcesClassLoader;
	
	public ClassLoaderServiceImpl(){
		this(ClassLoaderServiceImpl.class.getClassLoader());
	}
	
	public ClassLoaderServiceImpl(ClassLoader classLoader) {
       this(classLoader,classLoader,classLoader,classLoader);
	}

	public ClassLoaderServiceImpl(ClassLoader applicationClassLoader,
			ClassLoader resourcesClassLoader, ClassLoader zormClassLoader,
			ClassLoader environmentClassLoader) {
      if(zormClassLoader == null){
    	  zormClassLoader = ClassLoaderServiceImpl.class.getClassLoader();
      }
      if(environmentClassLoader == null || applicationClassLoader==null){
    	  //获取系统类加载器
    	  ClassLoader sysClassLoader = locateSystemClassLoader();
    	  //Returns the context ClassLoader for this Thread
    	  ClassLoader tccl = locateTCCL();
    	  if(environmentClassLoader==null){
    		  environmentClassLoader=sysClassLoader!=null?sysClassLoader:zormClassLoader;
    	  }
    	  if(applicationClassLoader==null){
    		  applicationClassLoader=tccl!=null?tccl:zormClassLoader;
    	  }
      }
      if(resourcesClassLoader==null){
    	  resourcesClassLoader=applicationClassLoader;
      }
      
      final LinkedHashSet<ClassLoader> classLoadingClassLoaders = new LinkedHashSet<ClassLoader>();
      classLoadingClassLoaders.add(applicationClassLoader);
      classLoadingClassLoaders.add(zormClassLoader);
      classLoadingClassLoaders.add(environmentClassLoader);
      
      this.classClassLoader = new ClassLoader(null) {
	    @Override
	    protected Class<?> findClass(String name) throws ClassNotFoundException {
            for(ClassLoader loader : classLoadingClassLoaders){
            	try{
            		return loader.loadClass(name);
            	}
            	catch(Exception ignore){}
            }
	    	throw new ClassNotFoundException("Could not load requested class : " + name );
	    }
      };
      
      this.resourcesClassLoader = resourcesClassLoader;
	}
	
	public static ClassLoaderServiceImpl fromConfigSettings(Map configVales){
		return new ClassLoaderServiceImpl(
				(ClassLoader)configVales.get(AvailableSettings.APP_CLASSLOADER),
				(ClassLoader)configVales.get(AvailableSettings.RESOURCES_CLASSLOADER), 
				(ClassLoader)configVales.get(AvailableSettings.ZORM_CLASSLOADER),
				(ClassLoader)configVales.get(AvailableSettings.ENVIRONMENT_CLASSLOADER));
	}

	private static ClassLoader locateTCCL() {
        try{
        	return Thread.currentThread().getContextClassLoader();
        }
        catch(Exception e){
        	return null;
        }
	}

	private static ClassLoader locateSystemClassLoader() {
      try{
    	  return ClassLoader.getSystemClassLoader();
      }
      catch(Exception e){
    	  return null;
      }
	}

	@SuppressWarnings("unchecked")
	public <T> Class<T> classForName(String className) {
         try{
        	 return (Class<T>) Class.forName(className,true,classClassLoader);
         }
         catch (Exception e) {
 			throw new ClassLoadingException( "Unable to load class [" + className + "]", e );
 		}
	 }

	public URL locateResource(String name) {
        try{
        	return new URL(name);
        }
        catch(Exception ignore){}
        try{
        	return resourcesClassLoader.getResource(name);
        }
        catch(Exception ignore){}
        return null;
	}

	public InputStream locateResourceStream(String name) {
		try {
			return new URL( name ).openStream();
		}
		catch ( Exception ignore ) {
		}

		try {
			return resourcesClassLoader.getResourceAsStream( name );
		}
		catch ( Exception ignore ) {
		}

		return null;
	}

	public List<URL> locateResources(String name) {
		ArrayList<URL> urls = new ArrayList<URL>();
		try {
			Enumeration<URL> urlEnumeration = resourcesClassLoader.getResources( name );
			if ( urlEnumeration != null && urlEnumeration.hasMoreElements() ) {
				while ( urlEnumeration.hasMoreElements() ) {
					urls.add( urlEnumeration.nextElement() );
				}
			}
		}
		catch ( Exception ignore ) {
		}

		return urls;
	}

	public <S> LinkedHashSet<S> loadJavaServices(Class<S> serviceContract) {
		final ClassLoader serviceLoaderClassLoader = new ClassLoader(null) {
			final ClassLoader[] classLoaderArray = new ClassLoader[] {
					// first look on the hibernate class loader
					getClass().getClassLoader(),
					// next look on the resource class loader
					resourcesClassLoader,
					// finally look on the combined class class loader
					classClassLoader
			};

			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				final HashSet<URL> resourceUrls = new HashSet<URL>();

				for ( ClassLoader classLoader : classLoaderArray ) {
					final Enumeration<URL> urls = classLoader.getResources( name );
					while ( urls.hasMoreElements() ) {
						resourceUrls.add( urls.nextElement() );
					}
				}

				return new Enumeration<URL>() {
					final Iterator<URL> resourceUrlIterator = resourceUrls.iterator();
					@Override
					public boolean hasMoreElements() {
						return resourceUrlIterator.hasNext();
					}

					@Override
					public URL nextElement() {
						return resourceUrlIterator.next();
					}
				};
			}

			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				for ( ClassLoader classLoader : classLoaderArray ) {
					try {
						return classLoader.loadClass( name );
					}
					catch (Exception ignore) {
					}
				}

				throw new ClassNotFoundException( "Could not load requested class : " + name );
			}
		};

		final ServiceLoader<S> loader = ServiceLoader.load( serviceContract, serviceLoaderClassLoader );
		final LinkedHashSet<S> services = new LinkedHashSet<S>();
		for ( S service : loader ) {
			services.add( service );
		}

		return services;
	}

}
