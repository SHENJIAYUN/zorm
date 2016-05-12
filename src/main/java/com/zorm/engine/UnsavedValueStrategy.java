package com.zorm.engine;

public interface UnsavedValueStrategy {
	public Boolean isUnsaved(Object test);

	public Object getDefaultValue(Object currentValue);
}
