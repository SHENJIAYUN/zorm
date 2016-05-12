package com.zorm;

import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.Session;
import com.zorm.type.Type;

public interface CustomEntityDirtinessStrategy {

	public void findDirty(Object entity, EntityPersister persister, Session session, DirtyCheckContext dirtyCheckContext);
	
	public void resetDirty(Object entity, EntityPersister persister, Session session);
	
	public static interface DirtyCheckContext {
		
		public void doDirtyChecking(AttributeChecker attributeChecker);
	}
	
	public static interface AttributeChecker {
	
		public boolean isDirty(AttributeInformation attributeInformation);
	}
	
	public static interface AttributeInformation {
		public EntityPersister getContainingPersister();

		public int getAttributeIndex();

		public String getName();

		public Type getType();

		public Object getCurrentValue();

	}
}
