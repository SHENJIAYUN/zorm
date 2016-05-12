package com.zorm.query;

public interface BinaryOperatorNode extends OperatorNode{
	public Node getLeftHandOperand();
	public Node getRightHandOperand();
}
