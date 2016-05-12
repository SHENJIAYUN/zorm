package com.zorm.config;

public interface ObjectNameSource {
	/**
	 * Retrieve the name explicitly provided by the user.
	 *
	 * @return The explicit name.
	 */
	public String getExplicitName();
	
	/**
	 * Retrieve the logical name for this object.  Usually this is the name under which
	 * the "thing" is registered with the {@link Mappings}.
	 * 
	 * @return The logical name.
	 */
	public String getLogicalName();
}
