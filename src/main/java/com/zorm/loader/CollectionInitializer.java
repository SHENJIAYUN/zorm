package com.zorm.loader;

import java.io.Serializable;
import com.zorm.session.SessionImplementor;

public interface CollectionInitializer {
	public void initialize(Serializable id, SessionImplementor session);
}
