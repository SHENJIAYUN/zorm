package com.zorm.jdbc;

public class TypeInfo {
  private final String typeName;
  private final int jdbcTypeCode;
  private final String[] createParams;
  private final boolean unsigned;
  private final int precision;
  private final short minimumScale;
  private final short maximumScale;
  private final boolean fixedPrecisionScale;
  private final String literalPrefix;
  private final String literalSuffix;
  private final boolean caseSensitive;
  private final TypeSearchability searchability;
  private final TypeNullability nullability;
  
  public TypeInfo(
			String typeName,
			int jdbcTypeCode,
			String[] createParams,
			boolean unsigned,
			int precision,
			short minimumScale,
			short maximumScale,
			boolean fixedPrecisionScale,
			String literalPrefix,
			String literalSuffix,
			boolean caseSensitive,
			TypeSearchability searchability,
			TypeNullability nullability) {
		this.typeName = typeName;
		this.jdbcTypeCode = jdbcTypeCode;
		this.createParams = createParams;
		this.unsigned = unsigned;
		this.precision = precision;
		this.minimumScale = minimumScale;
		this.maximumScale = maximumScale;
		this.fixedPrecisionScale = fixedPrecisionScale;
		this.literalPrefix = literalPrefix;
		this.literalSuffix = literalSuffix;
		this.caseSensitive = caseSensitive;
		this.searchability = searchability;
		this.nullability = nullability;
	}

public String getTypeName() {
	return typeName;
}

public int getJdbcTypeCode() {
	return jdbcTypeCode;
}

public String[] getCreateParams() {
	return createParams;
}

public boolean isUnsigned() {
	return unsigned;
}

public int getPrecision() {
	return precision;
}

public short getMinimumScale() {
	return minimumScale;
}

public short getMaximumScale() {
	return maximumScale;
}

public boolean isFixedPrecisionScale() {
	return fixedPrecisionScale;
}

public String getLiteralPrefix() {
	return literalPrefix;
}

public String getLiteralSuffix() {
	return literalSuffix;
}

public boolean isCaseSensitive() {
	return caseSensitive;
}

public TypeSearchability getSearchability() {
	return searchability;
}

public TypeNullability getNullability() {
	return nullability;
}
  
  
}
