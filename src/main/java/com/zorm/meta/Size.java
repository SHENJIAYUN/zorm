package com.zorm.meta;

import java.io.Serializable;

public class Size implements Serializable{
  public static enum LobMultiplier{
	  NONE(1),
	  K(NONE.factor * 1024),
	  M(K.factor * 1024),
	  G(M.factor * 1024);
	  
	  private long factor;
	  
	  private LobMultiplier(long factor){
		  this.factor = factor;
	  }
	  
	  public long getFactor(){
		  return factor;
	  }
  }
  
    public static final int DEFAULT_LENGTH = 255;
	public static final int DEFAULT_PRECISION = 19;
	public static final int DEFAULT_SCALE = 2;

	private long length = DEFAULT_LENGTH;
	private int precision = DEFAULT_PRECISION;
	private int scale = DEFAULT_SCALE;
	private LobMultiplier lobMultiplier = LobMultiplier.NONE;
	
	public Size() {}
	
	/**
	 * Complete constructor.
	 *
	 * @param precision numeric precision
	 * @param scale numeric scale
	 * @param length type length
	 * @param lobMultiplier LOB length multiplier
	 */
	public Size(int precision, int scale, long length, LobMultiplier lobMultiplier) {
		this.precision = precision;
		this.scale = scale;
		this.length = length;
		this.lobMultiplier = lobMultiplier;
	}

	public static Size precision(int precision) {
		return new Size( precision, -1, -1, null );
	}

	public static Size precision(int precision, int scale) {
		return new Size( precision, scale, -1, null );
	}

	public static Size length(long length) {
		return new Size( -1, -1, length, null );
	}

	public static Size length(long length, LobMultiplier lobMultiplier) {
		return new Size( -1, -1, length, lobMultiplier );
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}

	public long getLength() {
		return length;
	}

	public LobMultiplier getLobMultiplier() {
		return lobMultiplier;
	}

	public void initialize(Size size) {
		this.precision = size.precision;
		this.scale =  size.scale;
		this.length = size.length;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public void setLobMultiplier(LobMultiplier lobMultiplier) {
		this.lobMultiplier = lobMultiplier;
	}
	
	
}
