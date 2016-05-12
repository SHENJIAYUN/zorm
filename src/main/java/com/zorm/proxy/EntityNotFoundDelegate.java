package com.zorm.proxy;

import java.io.Serializable;

public interface EntityNotFoundDelegate {
	public void handleEntityNotFound(String entityName, Serializable id);
}
