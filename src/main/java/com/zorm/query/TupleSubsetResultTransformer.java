package com.zorm.query;

import com.zorm.entity.ResultTransformer;

public interface TupleSubsetResultTransformer extends ResultTransformer {
	/**
	 * When a tuple is transformed, is the result a single element of the tuple?
	 *
	 * @param aliases - the aliases that correspond to the tuple
	 * @param tupleLength - the number of elements in the tuple
	 * @return true, if the transformed value is a single element of the tuple;
	 *         false, otherwise.
	 */
	boolean isTransformedValueATupleElement(String[] aliases, int tupleLength);

	/**
	 * Returns an array with the i-th element indicating whether the i-th
	 * element of the tuple is included in the transformed value.
	 *
	 * @param aliases - the aliases that correspond to the tuple
	 * @param tupleLength - the number of elements in the tuple
	 * @return array with the i-th element indicating whether the i-th
	 *         element of the tuple is included in the transformed value.
	 */
	boolean[] includeInTransform(String[] aliases, int tupleLength);
}