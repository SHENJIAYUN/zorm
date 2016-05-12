package com.zorm.id;

import java.util.UUID;

import com.zorm.session.SessionImplementor;

public class StandardRandomStrategy implements UUIDGenerationStrategy{
  public static final StandardRandomStrategy INSTANCE = new StandardRandomStrategy();
  
	public int getGeneratedVersion() {
		// a "random" strategy
		return 4;
	}
	
	public UUID generateUUID(SessionImplementor session) {
		return UUID.randomUUID();
	}
}
