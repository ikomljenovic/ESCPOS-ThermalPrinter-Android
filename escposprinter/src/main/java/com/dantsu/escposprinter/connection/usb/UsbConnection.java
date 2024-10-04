package com.dantsu.escposprinter.connection.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;

public class UsbConnection extends DeviceConnection {

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private Context context;
    public UsbConnection(UsbManager usbManager, UsbDevice usbDevice, Context context) {
        super();
        this.usbManager = usbManager;
        this.usbDevice = usbDevice;
        this.context=context;
    }

    public UsbDevice getDevice() {
        return this.usbDevice;
    }

    public UsbConnection connect() throws EscPosConnectionException {
        Log.d("Fuuu", "Attempting to connect to USB device");
        if (this.isConnected()) {
            Log.d("Fuuu", "Already connected to USB device");
            return this;
        }

        try {
            this.outputStream = new UsbOutputStream(this.usbManager, this.usbDevice, this.context);
            this.data = new byte[0];
            Log.d("Fuuu", "Successfully connected to USB device");
        } catch (IOException e) {
            Log.e("Fuuu", "Failed to connect to USB device: " + e.getMessage());
            this.outputStream = null;
            throw new EscPosConnectionException("Unable to connect to USB device.");
        }
        return this;
    }

    public UsbConnection disconnect() {
        Log.d("Fuuu", "Disconnecting from USB device");
        this.data = new byte[0];
        if (this.isConnected()) {
            try {
                this.outputStream.close();
                Log.d("Fuuu", "Successfully disconnected from USB device");
            } catch (IOException e) {
                Log.e("Fuuu", "Error while disconnecting from USB device: " + e.getMessage());
            }
            this.outputStream = null;
        } else {
            Log.d("Fuuu", "Already disconnected from USB device");
        }
        return this;
    }

    public void send() throws EscPosConnectionException {
        this.send(0);
    }

    public void send(int addWaitingTime) throws EscPosConnectionException {
        Log.d("Fuuu", "Sending data to USB device");
        try {
            this.outputStream.write(this.data);
            this.data = new byte[0];
            Log.d("Fuuu", "Data sent successfully to USB device");
            if (addWaitingTime > 0) {
                Log.d("Fuuu", "Waiting for " + addWaitingTime + "ms");
                Thread.sleep(addWaitingTime);
            }
        } catch (IOException e) {
            Log.e("Fuuu", "Error sending data to USB device: " + e.getMessage());
            throw new EscPosConnectionException(e.getMessage());
        } catch (InterruptedException e) {
            Log.e("Fuuu", "Sleep interrupted while waiting after sending data: " + e.getMessage());
        }
    }
}