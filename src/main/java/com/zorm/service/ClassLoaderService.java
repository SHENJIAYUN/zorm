package com.zorm.service;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;

public interface ClassLoaderService extends Service{
	/**
	 * Locate a class by name
	 *
	 * @param className The name of the class to locate
	 *
	 * @return The class reference
	 *
	 * @throws ClassLoadingException Indicates the class could not be found
	 */
	public <T> Class<T> classForName(String className);
	
	/**
	 * Locate a resource by name (classpath lookup)
	 *
	 * @param name The resource name.
	 *
	 * @return The located URL; may return {@code null} to indicate the resource was not found
	 */
	public URL locateResource(String name);
	
	/**
	 * Locate a resource by name (classpath lookup) and gets its stream
	 *
	 * @param name The resource name.
	 *
	 * @return The stream of the located resource; may return {@code null} to indicate the resource was not found
	 */
	public InputStream locateResourceStream(String name);

	/**
	 * Locate a series of resource by name (classpath lookup)
	 *
	 * @param name The resource name.
	 *
	 * @return The list of URL matching; may return {@code null} to indicate the resource was not found
	 */
	public List<URL> locateResources(String name);
	
	/**
	 * Discovers and instantiates implementations of the named service contract.
	 * <p/>
	 * NOTE : the terms service here is used differently than {@link Service}.  Instead here we are talking about
	 * services as defined by {@link java.util.ServiceLoader}.
	 *
	 * @param serviceContract The java type defining the service contract
	 * @param <S> The type of the service contract
	 *     
	 * @return The ordered set of discovered services.
	 */
	public <S> LinkedHashSet<S> loadJavaServices(Class<S> serviceContract);
}
