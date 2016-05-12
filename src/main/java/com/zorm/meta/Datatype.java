package com.zorm.meta;

public class Datatype {
	private final int typeCode;
	private final String typeName;
	private final Class javaType;
	private final int hashCode;

	public Datatype(int typeCode, String typeName, Class javaType) {
		this.typeCode = typeCode;
		this.typeName = typeName;
		this.javaType = javaType;
		this.hashCode = generateHashCode();
	}

    private int generateHashCode() {
        int result = typeCode;
        if ( typeName != null ) {
            result = 31 * result + typeName.hashCode();
        }
        if ( javaType != null ) {
            result = 31 * result + javaType.hashCode();
        }
        return result;
    }

    public int getTypeCode() {
		return typeCode;
	}

	public String getTypeName() {
		return typeName;
	}

	public Class getJavaType() {
		return javaType;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Datatype datatype = (Datatype) o;

		return typeCode == datatype.typeCode
				&& javaType.equals( datatype.javaType )
				&& typeName.equals( datatype.typeName );

	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return super.toString() + "[code=" + typeCode + ", name=" + typeName + ", javaClass=" + javaType.getName() + "]";
	}
}
