package com.zorm;

public interface Filter {

	public String getName();
	public Filter setParameter(String name, Object value);
	public void validate();

}
