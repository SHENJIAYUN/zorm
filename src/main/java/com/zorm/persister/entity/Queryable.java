package com.zorm.persister.entity;

import com.zorm.persister.entity.Queryable.Declarer;

public interface Queryable extends Loadable, PropertyMapping, Joinable{

	public boolean isExplicitPolymorphism();

	public String getMappedSuperclass();

	public int getSubclassPropertyTableNumber(String propertyPath);

	public String identifierSelectFragment(String tableAlias, String suffix);

	public String propertySelectFragment(String tableAlias, String suffix,
			boolean allProperties);
	
	public static class Declarer {
		public static final Declarer CLASS = new Declarer( "class" );
		public static final Declarer SUBCLASS = new Declarer( "subclass" );
		public static final Declarer SUPERCLASS = new Declarer( "superclass" );
		private final String name;
		public Declarer(String name) {
			this.name = name;
		}
		public String toString() {
			return name;
		}
	}

	public Declarer getSubclassPropertyDeclarer(String propertyName);

	public boolean isMultiTable();
}
