package com.zorm.config;

import java.util.Map;

import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.config.annotations.EntityBinder;
import com.zorm.mapping.Collection;
import com.zorm.mapping.Join;
import com.zorm.mapping.PersistentClass;

/**
 * This factory is here ot build a PropertyHolder and prevent .mapping interface adding
 *
 * @author Emmanuel Bernard
 */
public final class PropertyHolderBuilder {
	private PropertyHolderBuilder() {
	}

	public static PropertyHolder buildPropertyHolder(
			XClass clazzToProcess,
			PersistentClass persistentClass,
			EntityBinder entityBinder,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		return new ClassPropertyHolder(
				persistentClass, clazzToProcess, entityBinder, mappings, inheritanceStatePerClass
		);
	}

	public static PropertyHolder buildPropertyHolder(
			PersistentClass persistentClass,
			Map<String, Join> joins,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		return new ClassPropertyHolder( persistentClass, null, joins, mappings, inheritanceStatePerClass );
	}

	public static PropertyHolder buildPropertyHolder(
			Collection collection,
			String path,
			XClass clazzToProcess,
			XProperty property,
			PropertyHolder parentPropertyHolder,
			Mappings mappings) {
		return new CollectionPropertyHolder(
				collection, path, clazzToProcess, property, parentPropertyHolder, mappings
		);
	}

}
