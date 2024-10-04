package com.dantsu.escposprinter.rw;


public class TTYTermios {

	public int baudrate = 9600;
	

	public FlowControl flowControl = FlowControl.NONE;
	

	public Parity parity = Parity.NONE;
	

	public StopBits stopBits = StopBits.ONE;

	public int dataBits = 8;

	public TTYTermios(int baudrate, FlowControl flowControl, Parity parity,
			StopBits stopBits, int dataBits) {
		this.baudrate = baudrate;
		this.flowControl = flowControl;
		this.parity = parity;
		this.stopBits = stopBits;
		this.dataBits = dataBits;
	}

	public enum FlowControl {
		NONE, DTR_RTS
	}
	public enum Parity {
		NONE, ODD, EVEN, SPACE, MARK
	}
	public enum StopBits {
		ONE, ONEPFIVE, TWO
	}
}
