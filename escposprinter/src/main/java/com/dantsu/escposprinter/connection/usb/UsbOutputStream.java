package com.dantsu.escposprinter.connection.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.dantsu.escposprinter.rw.PL2303Driver;
import com.dantsu.escposprinter.rw.TTYTermios;
import com.dantsu.escposprinter.rw.USBSerialPort;
import com.dantsu.escposprinter.rw.USBPort;

public class UsbOutputStream extends OutputStream {
    private UsbDeviceConnection usbConnection;
    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpoint;
    private PL2303Driver pl2303Driver;
    private USBSerialPort serialPort;
    private boolean isPL2303;

    public UsbOutputStream(UsbManager usbManager, UsbDevice usbDevice, Context context) throws IOException {
        this.usbInterface = UsbDeviceHelper.findPrinterInterface(usbDevice);
        if (this.usbInterface == null) {
            throw new IOException("Unable to find USB interface.");
        }

        this.usbEndpoint = UsbDeviceHelper.findEndpointIn(this.usbInterface);
        if (this.usbEndpoint == null) {
            throw new IOException("Unable to find USB endpoint.");
        }

        this.usbConnection = usbManager.openDevice(usbDevice);
        if (this.usbConnection == null) {
            throw new IOException("Unable to open USB connection.");
        }

        // Check if the device is a PL2303
        if (usbDevice.getVendorId() == 0x067b && usbDevice.getProductId() == 0x2303) {
            isPL2303 = true;
            this.pl2303Driver = new PL2303Driver();
            USBPort usbPort = new USBPort(usbManager, context, usbDevice, null);
            TTYTermios termios = new TTYTermios(9600, TTYTermios.FlowControl.NONE, TTYTermios.Parity.NONE, TTYTermios.StopBits.ONE, 8);
            this.serialPort = new USBSerialPort(usbPort, termios);

            int ret = this.pl2303Driver.pl2303_probe(this.serialPort);
            if (ret != 0) {
                throw new IOException("Unable to initialize PL2303 device.");
            }

            ret = this.pl2303Driver.pl2303_open(this.serialPort);
            if (ret != 0) {
                throw new IOException("Unable to open PL2303 serial connection.");
            }
        } else {
            isPL2303 = false;
        }
    }

    @Override
    public void write(int i) throws IOException {
        this.write(new byte[]{(byte) i});
    }

    @Override
    public void write(@NonNull byte[] bytes) throws IOException {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public void write(final @NonNull byte[] bytes, final int offset, final int length) throws IOException {
        if (isPL2303) {
            if (this.serialPort == null || this.pl2303Driver == null) {
                throw new IOException("Unable to connect to PL2303 device.");
            }
            int ret = this.pl2303Driver.pl2303_write(this.serialPort, bytes, offset, length,5000);
            if (ret < 0) {
                throw new IOException("Error writing to PL2303 device.");
            }
        } else {
            if (this.usbInterface == null || this.usbEndpoint == null || this.usbConnection == null) {
                throw new IOException("Unable to connect to USB device.");
            }
            if (!this.usbConnection.claimInterface(this.usbInterface, true)) {
                throw new IOException("Error during claim USB interface.");
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
            UsbRequest usbRequest = new UsbRequest();
            try {
                usbRequest.initialize(this.usbConnection, this.usbEndpoint);
                if (!usbRequest.queue(buffer, length)) {
                    throw new IOException("Error queueing USB request.");
                }
                this.usbConnection.requestWait();
            } finally {
                usbRequest.close();
            }
        }
    }

    @Override
    public void flush() throws IOException {
        // No specific flush implementation needed
    }

    @Override
    public void close() throws IOException {
        if (isPL2303) {
            if (this.pl2303Driver != null && this.serialPort != null) {
                this.pl2303Driver.pl2303_close(this.serialPort);
                this.pl2303Driver.pl2303_disconnect(this.serialPort);
            }
        }
        if (this.usbConnection != null) {
            this.usbConnection.close();
        }
        this.usbInterface = null;
        this.usbEndpoint = null;
        this.usbConnection = null;
        this.serialPort = null;
        this.pl2303Driver = null;
    }
}