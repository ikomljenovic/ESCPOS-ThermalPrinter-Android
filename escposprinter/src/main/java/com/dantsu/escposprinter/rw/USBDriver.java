package com.dantsu.escposprinter.rw;

import  com.dantsu.escposprinter.utils.DataUtils;
import  com.dantsu.escposprinter.utils.ErrorCode;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

/**
 * ��һ�㣬USB���� Χ����USBPort������
 * 
 * @author Administrator
 * 
 */
public class USBDriver {

	String description;


	int probe(USBPort port, USBDeviceId id[]) {
		Log.d("USB DRIVER", "Starting probe function");
		// Log the full USBDeviceId[] list

		if (port == null || id == null) {
			Log.e("USB DRIVER", "Port or device ID array is null. Returning ErrorCode.NULLPOINTER");
			return ErrorCode.NULLPOINTER;
		}
		if (port.mUsbManager == null) {
			Log.e("USB DRIVER", "port.mUsbManager is null. Returning ErrorCode.NULLPOINTER");
			return ErrorCode.NULLPOINTER;
		}
		if (port.mContext == null) {
			Log.e("USB DRIVER", "port.mContext is null. Returning ErrorCode.NULLPOINTER");
			return ErrorCode.NULLPOINTER;
		}
		if (port.mUsbDevice == null) {
			Log.e("USB DRIVER", "port.mUsbDevice is null. Returning ErrorCode.NULLPOINTER");
			return ErrorCode.NULLPOINTER;
		}
		if (port.mPermissionIntent == null) {
			Log.e("USB DRIVER", "port.mPermissionIntent is null. Returning ErrorCode.NULLPOINTER");
			return ErrorCode.NULLPOINTER;
		}
		Log.d("USB DRIVER", "Logging all USBDeviceId entries:");
		for (int x = 0; x < id.length; x++) {
			Log.d("USB DRIVER", "USBDeviceId[" + x + "] -> Vendor ID: " + id[x].idVendor + ", Product ID: " + id[x].idProduct);
		}
		Log.d("USB DRIVER", "Vendor ID=" + port.mUsbDevice.getVendorId() + " Product ID=" + port.mUsbDevice.getProductId());

		// Loop through the array of USB device IDs to check if we have a matching device
		for (int i = 0; i < id.length; i++) {
			Log.d("USB DRIVER", "Checking device ID #" + i + " - Vendor ID: " + id[i].idVendor + ", Product ID: " + id[i].idProduct);

			if (id[i].idVendor == port.mUsbDevice.getVendorId() && id[i].idProduct == port.mUsbDevice.getProductId()) {
				Log.d("USB DRIVER", "Matching device found.");

				// Check permission
				Log.d("USB DRIVER", "Checking USB permission for device.");
				boolean hasPermission = port.mUsbManager.hasPermission(port.mUsbDevice);
				Log.d("USB DRIVER", "Has permission: " + hasPermission);

				if (!hasPermission) {
					Log.d("USB DRIVER", "Requesting permission for device.");
					port.mUsbManager.requestPermission(port.mUsbDevice, port.mPermissionIntent);
				}

				hasPermission = port.mUsbManager.hasPermission(port.mUsbDevice);
				if (!hasPermission) {
					Log.e("USB DRIVER", "No permission to access the USB device. Returning ErrorCode.NOPERMISSION");
					return ErrorCode.NOPERMISSION;
				}

				// Loop through the interfaces and endpoints
				Log.d("USB DRIVER", "Looking for USB interfaces and endpoints.");
				outer:
				for (int k = 0; k < port.mUsbDevice.getInterfaceCount(); k++) {
					Log.d("USB DRIVER", "Checking interface #" + k);
					port.mUsbInterface = port.mUsbDevice.getInterface(k);
					port.mUsbEndpointOut = null;
					port.mUsbEndpointIn = null;

					for (int j = 0; j < port.mUsbInterface.getEndpointCount(); j++) {
						Log.d("USB DRIVER", "Checking endpoint #" + j);
						UsbEndpoint endpoint = port.mUsbInterface.getEndpoint(j);
						Log.d("USB DRIVER", "Endpoint direction: " + endpoint.getDirection() + ", type: " + endpoint.getType());

						if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT && endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
							port.mUsbEndpointOut = endpoint;
							Log.d("USB DRIVER", "Found USB OUT bulk transfer endpoint.");
						} else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN && endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
							port.mUsbEndpointIn = endpoint;
							Log.d("USB DRIVER", "Found USB IN bulk transfer endpoint.");
						}

						// If both endpoints are found, break the loop
						if (port.mUsbEndpointOut != null && port.mUsbEndpointIn != null) {
							Log.d("USB DRIVER", "Both IN and OUT endpoints found. Exiting the loop.");
							break outer;
						}
					}
				}

				if (port.mUsbInterface == null) {
					Log.e("USB DRIVER", "No USB interface found. Returning ErrorCode.NULLPOINTER");
					return ErrorCode.NULLPOINTER;
				}

				if (port.mUsbEndpointOut == null || port.mUsbEndpointIn == null) {
					Log.e("USB DRIVER", "Required endpoints (IN/OUT) are missing. Returning ErrorCode.NULLPOINTER");
					return ErrorCode.NULLPOINTER;
				}

				// Open the USB device connection
				Log.d("USB DRIVER", "Opening USB device connection.");
				port.mUsbDeviceConnection = port.mUsbManager.openDevice(port.mUsbDevice);

				if (port.mUsbDeviceConnection == null) {
					Log.e("USB DRIVER", "Failed to open USB device connection. Returning ErrorCode.NULLPOINTER");
					return ErrorCode.NULLPOINTER;
				}

				Log.d("USB DRIVER", "Claiming interface.");
				port.mUsbDeviceConnection.claimInterface(port.mUsbInterface, true);

				Log.d("USB DRIVER", "Probe function completed successfully.");
				return 0; // Success
			}
		}

		Log.e("USB DRIVER", "No matching USB device found. Returning ErrorCode.ERROR");
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