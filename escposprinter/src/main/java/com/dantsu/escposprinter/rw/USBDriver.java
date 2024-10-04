package com.dantsu.escposprinter.rw;

import  com.dantsu.escposprinter.utils.DataUtils;
import  com.dantsu.escposprinter.utils.ErrorCode;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbEndpoint;

/**
 * ��һ�㣬USB���� Χ����USBPort������
 * 
 * @author Administrator
 * 
 */
public class USBDriver {

	String description;
	

	int probe(USBPort port, USBDeviceId id[]) {
		if (null == port || null == id)
			return ErrorCode.NULLPOINTER;
		if (null == port.mUsbManager)
			return ErrorCode.NULLPOINTER;
		if (null == port.mContext)
			return ErrorCode.NULLPOINTER;
		if (null == port.mUsbDevice)
			return ErrorCode.NULLPOINTER;
		if (null == port.mPermissionIntent)
			return ErrorCode.NULLPOINTER;

		for (int i = 0; i < id.length; i++)
			if (id[i].idVendor == port.mUsbDevice.getVendorId()
					&& id[i].idProduct == port.mUsbDevice.getProductId()) {
				if (!port.mUsbManager.hasPermission(port.mUsbDevice))
					port.mUsbManager.requestPermission(port.mUsbDevice,
							port.mPermissionIntent);

				if (!port.mUsbManager.hasPermission(port.mUsbDevice))
					return ErrorCode.NOPERMISSION;

				// ö�٣��Ѷ�д���ƶ˿�ʲô�ĸ�Ū������Ȼ��set
				outer: for (int k = 0; k < port.mUsbDevice.getInterfaceCount(); k++) {
					port.mUsbInterface = port.mUsbDevice.getInterface(i);
					port.mUsbEndpointOut = null;
					port.mUsbEndpointIn = null;
					for (int j = 0; j < port.mUsbInterface.getEndpointCount(); j++) {
						UsbEndpoint endpoint = port.mUsbInterface
								.getEndpoint(j);
						if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT
								&& endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
							port.mUsbEndpointOut = endpoint;
						} else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN
								&& endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
							port.mUsbEndpointIn = endpoint;
						}

						// ����ڵ�һ���ӿھ��ҵ��˷���Ҫ��Ķ˵㣬��ôbreak;
						if ((null != port.mUsbEndpointOut)
								&& (null != port.mUsbEndpointIn))
							break outer;
					}
				}
				if (null == port.mUsbInterface)
					return ErrorCode.NULLPOINTER;
				if ((null == port.mUsbEndpointOut)
						|| (null == port.mUsbEndpointIn))
					return ErrorCode.NULLPOINTER;
				port.mUsbDeviceConnection = port.mUsbManager
						.openDevice(port.mUsbDevice);
				if (null == port.mUsbDeviceConnection)
					return ErrorCode.NULLPOINTER;
				port.mUsbDeviceConnection.claimInterface(port.mUsbInterface,
						true);
				return 0;
			}

		return ErrorCode.ERROR;
	}

	void disconnect(USBPort port) {
		if (null == port)
			return;
		if ((null != port.mUsbInterface) && (null != port.mUsbDeviceConnection)) {
			port.mUsbDeviceConnection.releaseInterface(port.mUsbInterface);
			port.mUsbDeviceConnection.close();
		}
	}

	int write(USBPort port, byte[] buffer, int offset, int count, int timeout) {
		if (null == port || null == buffer)
			return ErrorCode.NULLPOINTER;
		if (null == port.mUsbEndpointOut)
			return ErrorCode.NULLPOINTER;
		if (null == port.mUsbDeviceConnection)
			return ErrorCode.NULLPOINTER;
		if (count < 0 || offset < 0 || timeout <= 0)
			return ErrorCode.INVALPARAM;
		byte[] data = new byte[count];
		DataUtils.copyBytes(buffer, offset, data, 0, count);
		return port.mUsbDeviceConnection.bulkTransfer(port.mUsbEndpointOut,
				data, data.length, timeout);
	}

	int read(USBPort  port, byte[] buffer, int offset, int count, int timeout) {
		if (null == port || null == buffer)
			return ErrorCode.NULLPOINTER;
		if (null == port.mUsbEndpointIn)
			return ErrorCode.NULLPOINTER;
		if (null == port.mUsbDeviceConnection)
			return ErrorCode.NULLPOINTER;
		if (count < 0 || offset < 0 || timeout <= 0)
			return ErrorCode.INVALPARAM;
		byte[] data = new byte[count];
		int recnt = port.mUsbDeviceConnection.bulkTransfer(port.mUsbEndpointIn,
				data, data.length, timeout);
		DataUtils.copyBytes(data, 0, buffer, offset, recnt);
		return recnt; // ���ض�ȡ���ֽ���
	}

	int ctl(USBPort port, int requestType, int request, int value, int index,
			byte[] buffer, int length, int timeout) {
		if (null == port)
			return ErrorCode.INVALPARAM;
		if (null == port.mUsbDeviceConnection)
			return ErrorCode.INVALPARAM;

		return port.mUsbDeviceConnection.controlTransfer(requestType, request,
				value, index, buffer, length, timeout);
	}
}

class USBDeviceId {
	int idVendor;
	int idProduct;

	public USBDeviceId(int vid, int pid) {
		idVendor = vid;
		idProduct = pid;
	}
}