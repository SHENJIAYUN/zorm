package com.zorm.service;

import java.util.Map;

public interface ConfigurationService extends Service{
	public Map getSettings();

	public <T> T getSetting(String name, Converter<T> converter);
	public <T> T getSetting(String name, Converter<T> converter, T defaultValue);
	public <T> T getSetting(String name, Class<T> expected, T defaultValue);

	/**
	 * Cast <tt>candidate</tt> to the instance of <tt>expected</tt> type.
	 *
	 * @param expected The type of instance expected to return.
	 * @param candidate The candidate object to be casted.
	 * @return The instance of expected type or null if this cast fail.
	 */
	public <T> T cast(Class<T> expected, Object candidate);
	public static interface Converter<T> {
		public T convert(Object value);
	}
}
