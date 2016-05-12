package com.zorm.session;

import java.io.Serializable;

public interface IdentifierLoadAccess {
	public Object load(Serializable id);
}
