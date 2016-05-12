package com.zorm;

import com.zorm.session.SessionImplementor;
import com.zorm.type.descriptor.LobCreator;

public final class Zorm {
	private Zorm() {
		throw new UnsupportedOperationException();
	}
	
	public static LobCreator getLobCreator(SessionImplementor session) {
		return session.getFactory()
				.getJdbcServices()
				.getLobCreator( session );
	}
}
