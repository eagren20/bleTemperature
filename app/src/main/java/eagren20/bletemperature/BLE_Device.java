package eagren20.bletemperature;

import android.bluetooth.BluetoothDevice;

/**
 * Author: Erik Agren
 * 6/7/2017
 * Object representing a scanned device and its properties
 */

class BLE_Device {


    private BluetoothDevice device;
    //signal strength
    private int rssi;
    //whether or not the device was checked
    private boolean checked;

    BLE_Device(BluetoothDevice device, int rssi) {
        this.device = device;
        this.rssi = rssi;
        checked = false;
    }

    //Various getters and setters
    String getAddress() {
        return device.getAddress();
    }

    public String getName() {
        return device.getName();
    }

    void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    int getRSSI() {
        return rssi;
    }

    boolean isChecked() {
        return checked;
    }

    void setChecked(boolean new_value) {
            this.checked = new_value;
    }
}