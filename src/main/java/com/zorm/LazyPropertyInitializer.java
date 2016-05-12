package com.zorm;

import java.io.Serializable;

public interface LazyPropertyInitializer {
	public static final Serializable UNFETCHED_PROPERTY = new Serializable() {
		public String toString() {
			return "<lazy>";
		}
		public Object readResolve() {
			return UNFETCHED_PROPERTY;
		}
	};
}
