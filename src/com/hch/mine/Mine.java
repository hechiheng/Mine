package com.hch.mine;

public class Mine {

	private int i, j, value;
	private boolean show;
	private boolean sign;

	public Mine(int i, int j) {
		this.i = i;
		this.j = j;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	public boolean isShow() {
		return show;
	}

	public void setShow(boolean show) {
		this.show = show;
	}

	public boolean isSign() {
		return sign;
	}

	public void setSign(boolean sign) {
		this.sign = sign;
	}

}
