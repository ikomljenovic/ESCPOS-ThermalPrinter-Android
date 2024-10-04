package com.dantsu.escposprinter.rw;

/**
 * Χ��USBSerialPortչ��
 * 
 * @author Administrator
 * 
 */
public abstract class USBSerialDriver extends USBDriver {

	abstract int attach(USBSerialPort serial);

	abstract int release(USBSerialPort serial);

	abstract int open(USBSerialPort serial);

	abstract int close(USBSerialPort serial);

	abstract int set_termios(USBSerialPort serial, TTYTermios termiosnew);

}
