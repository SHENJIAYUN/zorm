package com.zorm.engine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.zorm.action.CascadingAction;
import com.zorm.exception.MappingException;
import com.zorm.util.ArrayHelper;

public abstract class CascadeStyle implements Serializable{
	private static final long serialVersionUID = -4442436843136477992L;
	
	public abstract boolean doCascade(CascadingAction action) ;
	
	@SuppressWarnings("serial")
	public static final CascadeStyle NONE = new CascadeStyle() {
		public boolean doCascade(CascadingAction action) {
			return false;
		}

		public String toString() {
			return "STYLE_NONE";
		}
	};
	
	@SuppressWarnings("serial")
	public static final CascadeStyle ALL = new CascadeStyle() {
		public boolean doCascade(CascadingAction action) {
			return true;
		}

		public String toString() {
			return "STYLE_ALL";
		}
	};
	
	static final Map<String, CascadeStyle> STYLES = new HashMap<String, CascadeStyle>();
	
	static{
		STYLES.put("all", ALL);
		STYLES.put("none", NONE);
	}
	
	public boolean reallyDoCascade(CascadingAction action) {
		return doCascade( action );
	}
	
	public boolean hasOrphanDelete() {
		return false;
	}
	
	public static final class MultipleCascadeStyle extends CascadeStyle {
		private final CascadeStyle[] styles;

		public MultipleCascadeStyle(CascadeStyle[] styles) {
			this.styles = styles;
		}

		public boolean doCascade(CascadingAction action) {
			for ( CascadeStyle style : styles ) {
				if ( style.doCascade( action ) ) {
					return true;
				}
			}
			return false;
		}

		public boolean reallyDoCascade(CascadingAction action) {
			for ( CascadeStyle style : styles ) {
				if ( style.reallyDoCascade( action ) ) {
					return true;
				}
			}
			return false;
		}

		public boolean hasOrphanDelete() {
			for ( CascadeStyle style : styles ) {
				if ( style.hasOrphanDelete() ) {
					return true;
				}
			}
			return false;
		}

		public String toString() {
			return ArrayHelper.toString( styles );
		}
	}

	public static CascadeStyle getCascadeStyle(String cascade) {
		CascadeStyle style = STYLES.get( cascade );
		if ( style == null ) {
			throw new MappingException( "Unsupported cascade style: " + cascade );
		}
		else {
			return style;
		}
	}



}
