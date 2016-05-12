package com.zorm.engine;

import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;

public final class Nullability {
	private final SessionImplementor session;
	private final boolean checkNullability;

	public Nullability(SessionImplementor session) {
		this.session = session;
		this.checkNullability = session.getFactory().getSettings().isCheckNullability();
	}

	public void checkNullability(
			final Object[] values,
			final EntityPersister persister,
			final boolean isUpdate) {
		if(checkNullability){
			
		}
	}
}
