package com.dantsu.escposprinter.rw;

/**
 * ����ֻ��2�����캯����2��������������û���������� ������������������new��ʱ��ָ����Ҳ������new��ʱ���ÿգ��Ժ�ͨ�����ʳ�Ա��ָ��
 * 
 * @author Administrator
 * 
 */
public class USBSerialPort {

	public USBPort port;
	public TTYTermios termios;

	public USBSerialPort(USBPort port, TTYTermios termios) {
		this.port = port;
		this.termios = termios;
	}
}
