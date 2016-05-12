package com.zorm.entity;

import com.zorm.FetchMode;
import com.zorm.engine.CascadeStyle;
import com.zorm.tuple.Property;
import com.zorm.type.Type;

public class StandardProperty extends Property{
	
	 private final boolean lazy;
	    private final boolean insertable;
	    private final boolean updateable;
		private final boolean insertGenerated;
		private final boolean updateGenerated;
	    private final boolean nullable;
	    private final boolean dirtyCheckable;
	    private final boolean versionable;
		private final FetchMode fetchMode;
		private CascadeStyle cascadeStyle;
	
	 public StandardProperty(
	            String name,
	            String node,
	            Type type,
	            boolean lazy,
	            boolean insertable,
	            boolean updateable,
	            boolean insertGenerated,
	            boolean updateGenerated,
	            boolean nullable,
	            boolean checkable,
	            boolean versionable,
	            CascadeStyle cascadeStyle,
	            FetchMode fetchMode) {
	        super(name, node, type);
	        this.lazy = lazy;
	        this.insertable = insertable;
	        this.updateable = updateable;
	        this.insertGenerated = insertGenerated;
		    this.updateGenerated = updateGenerated;
	        this.nullable = nullable;
	        this.dirtyCheckable = checkable;
	        this.versionable = versionable;
	        this.cascadeStyle =  cascadeStyle;
		    this.fetchMode = fetchMode;
	    }

	public boolean isLazy() {
		return lazy;
	}

	  public boolean isInsertable() {
	        return insertable;
	    }

	    public boolean isUpdateable() {
	        return updateable;
	    }

		public boolean isInsertGenerated() {
			return insertGenerated;
		}

		public boolean isUpdateGenerated() {
			return updateGenerated;
		}

	    public boolean isNullable() {
	        return nullable;
	    }

	    public boolean isDirtyCheckable(boolean hasUninitializedProperties) {
	        return isDirtyCheckable() && ( !hasUninitializedProperties || !isLazy() );
	    }

	    public boolean isDirtyCheckable() {
	        return dirtyCheckable;
	    }

	    public boolean isVersionable() {
	        return versionable;
	    }

	    public CascadeStyle getCascadeStyle() {
	        return cascadeStyle;
	    }

		public FetchMode getFetchMode() {
			return fetchMode;
		}
}
