package com.zorm.mapping;

public interface PersistentClassVisitor {
	Object accept(RootClass class1);
	Object accept(Subclass subclass);
	Object accept(SingleTableSubclass subclass);
}
