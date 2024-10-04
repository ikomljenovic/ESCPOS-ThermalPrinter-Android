package com.dantsu.escposprinter.connection.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.dantsu.escposprinter.rw.PL2303Driver;
import com.dantsu.escposprinter.rw.TTYTermios;
import com.dantsu.escposprinter.rw.USBSerialPort;
import com.dantsu.escposprinter.rw.USBPort;

public class UsbOutputStream extends OutputStream {
    private static final String TAG = "Fuuu";
    private UsbDeviceConnection usbConnection;
    private static final String ACTION_USB_PERMISSION = "com.dantsu.escposprinter.USB_PERMISSION";

    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpoint;
    private PL2303Driver pl2303Driver;
    private USBSerialPort serialPort;
    private boolean isPL2303;

    public UsbOutputStream(UsbManager usbManager, UsbDevice usbDevice, Context context) throws IOException {
        Log.d(TAG, "Initializing UsbOutputStream");
        this.usbInterface = UsbDeviceHelper.findPrinterInterface(usbDevice);
        if (this.usbInterface == null) {
            Log.e(TAG, "Unable to find USB interface");
            throw new IOException("Unable to find USB interface.");
        }
        Log.d(TAG, "USB interface found");

        this.usbEndpoint = UsbDeviceHelper.findEndpointIn(this.usbInterface);
        if (this.usbEndpoint == null) {
            Log.e(TAG, "Unable to find USB endpoint");
            throw new IOException("Unable to find USB endpoint.");
        }
        Log.d(TAG, "USB endpoint found");

        this.usbConnection = usbManager.openDevice(usbDevice);
        if (this.usbConnection == null) {
            Log.e(TAG, "Unable to open USB connection");
            throw new IOException("Unable to open USB connection.");
        }
        Log.d(TAG, "USB connection opened");

        // Check if the device is a PL2303
        if (usbDevice.getVendorId() == 4070 && usbDevice.getProductId() == 33054) {
            Log.d(TAG, "Device identified as PL2303");
            isPL2303 = true;
            this.pl2303Driver = new PL2303Driver();
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

            USBPort usbPort = new USBPort(usbManager, context, usbDevice, permissionIntent);TTYTermios termios = new TTYTermios(115200, TTYTermios.FlowControl.NONE, TTYTermios.Parity.NONE, TTYTermios.StopBits.ONE, 8);
            this.serialPort = new USBSerialPort(usbPort, termios);
            if (this.serialPort==null){
                Log.e(TAG,"usb serial port is null");
                throw new IOException("UNable to init serial port");
            }
            Log.e(TAG, "Inited USB and TTY, trying to probe serial port");
            int ret = this.pl2303Driver.pl2303_probe(this.serialPort);
            if (ret != 0) {
                Log.e(TAG, "Unable to initialize PL2303 device. Error code: " + ret);
                throw new IOException("Unable to initialize PL2303 device.");
            }
            Log.d(TAG, "PL2303 device probed successfully");

            ret = this.pl2303Driver.pl2303_open(this.serialPort);
            if (ret != 0) {
                Log.e(TAG, "Unable to open PL2303 serial connection. Error code: " + ret);
                throw new IOException("Unable to open PL2303 serial connection.");
            }
            Log.d(TAG, "PL2303 serial connection opened successfully");
        } else {
            Log.d(TAG, "Device is not a PL2303");
            isPL2303 = false;
        }
        Log.d(TAG, "UsbOutputStream initialization completed");
    }

    @Override
    public void write(int i) throws IOException {
        Log.v(TAG, "Writing single byte: " + i);
        this.write(new byte[]{(byte) i});
    }

    @Override
    public void write(@NonNull byte[] bytes) throws IOException {
        Log.v(TAG, "Writing " + bytes.length + " bytes");
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public void write(final @NonNull byte[] bytes, final int offset, final int length) throws IOException {
        Log.d(TAG, "Writing " + length + " bytes starting at offset " + offset);
        if (isPL2303) {
            if (this.serialPort == null || this.pl2303Driver == null) {
                Log.e(TAG, "Unable to connect to PL2303 device");
                throw new IOException("Unable to connect to PL2303 device.");
            }
            Log.d(TAG, "Writing to PL2303 device");
            int ret = this.pl2303Driver.pl2303_write(this.serialPort, bytes, offset, length, 5000);
            if (ret < 0) {
                Log.e(TAG, "Error writing to PL2303 device. Error code: " + ret);
                throw new IOException("Error writing to PL2303 device.");
            }
            Log.d(TAG, "Successfully wrote " + ret + " bytes to PL2303 device");
        } else {
            if (this.usbInterface == null || this.usbEndpoint == null || this.usbConnection == null) {
                Log.e(TAG, "Unable to connect to USB device");
                throw new IOException("Unable to connect to USB device.");
            }
            Log.d(TAG, "Writing to USB device");
            if (!this.usbConnection.claimInterface(this.usbInterface, true)) {
                Log.e(TAG, "Error during claim USB interface");
                throw new IOException("Error during claim USB interface.");
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
            UsbRequest usbRequest = new UsbRequest();
            try {
                usbRequest.initialize(this.usbConnection, this.usbEndpoint);
                if (!usbRequest.queue(buffer, length)) {
                    Log.e(TAG, "Error queueing USB request");
                    throw new IOException("Error queueing USB request.");
                }
                Log.d(TAG, "USB request queued, waiting for response");
                this.usbConnection.requestWait();
                Log.d(TAG, "USB request completed");
            } finally {
                usbRequest.close();
                Log.d(TAG, "USB request closed");
            }
        }
    }

    @Override
    public void flush() throws IOException {
        Log.d(TAG, "Flush called (no implementation)");
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, "Closing UsbOutputStream");
        if (isPL2303) {
            if (this.pl2303Driver != null && this.serialPort != null) {
                Log.d(TAG, "Closing PL2303 connection");
                this.pl2303Driver.pl2303_close(this.serialPort);
                this.pl2303Driver.pl2303_disconnect(this.serialPort);
                Log.d(TAG, "PL2303 connection closed and disconnected");
            }
        }
        if (this.usbConnection != null) {
            Log.d(TAG, "Closing USB connection");
            this.usbConnection.close();
        }
        this.usbInterface = null;
        this.usbEndpoint = null;
        this.usbConnection = null;
        this.serialPort = null;
        this.pl2303Driver = null;
        Log.d(TAG, "UsbOutputStream fully closed");
    }
}