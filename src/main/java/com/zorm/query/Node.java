package com.zorm.query;

import antlr.Token;
import antlr.collections.AST;

import com.zorm.session.SessionFactoryImplementor;
import com.zorm.util.StringHelper;

public class Node extends antlr.CommonAST{
	private String filename;
	private int line;
	private int column;
	private int textLength;

	public Node() {
		super();
	}

	public Node(Token tok) {
		super(tok);  // This will call initialize(tok)!
	}

	/**
	 * Retrieve the text to be used for rendering this particular node.
	 *
	 * @param sessionFactory The session factory
	 * @return The text to use for rendering
	 */
	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		return getText();
	}

	@Override
    public void initialize(Token tok) {
		super.initialize(tok);
		filename = tok.getFilename();
		line = tok.getLine();
		column = tok.getColumn();
		String text = tok.getText();
		textLength = StringHelper.isEmpty(text) ? 0 : text.length();
	}

	@Override
    public void initialize(AST t) {
		super.initialize( t );
		if ( t instanceof Node ) {
			Node n = (Node)t;
			filename = n.filename;
			line = n.line;
			column = n.column;
			textLength = n.textLength;
		}
	}

	public String getFilename() {
		return filename;
	}

	@Override
    public int getLine() {
		return line;
	}

	@Override
    public int getColumn() {
		return column;
	}

	public int getTextLength() {
		return textLength;
	}
}
