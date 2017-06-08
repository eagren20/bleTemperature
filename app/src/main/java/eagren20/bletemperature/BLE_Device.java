package eagren20.bletemperature;

import android.bluetooth.BluetoothDevice;

/**
 * Created by eagre on 6/7/2017.
 */

public class BLE_Device {

    private BluetoothDevice device;
    private int rssi;
    private boolean checked;

    public BLE_Device(BluetoothDevice device, int rssi) {
        this.device = device;
        this.rssi = rssi;
        checked = false;
    }

    public String getAddress() {
        return device.getAddress();
    }

    public String getName() {
        return device.getName();
    }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    public int getRSSI() {
        return rssi;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean new_value) {
            this.checked = new_value;
    }
}