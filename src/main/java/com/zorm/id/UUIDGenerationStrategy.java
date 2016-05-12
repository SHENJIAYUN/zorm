package com.zorm.id;

import java.io.Serializable;
import java.util.UUID;

import com.zorm.session.SessionImplementor;

public interface UUIDGenerationStrategy extends Serializable{

	public int getGeneratedVersion();
	public UUID generateUUID(SessionImplementor session);
}
