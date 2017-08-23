package eagren20.bletemperature;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import java.util.ArrayList;


/**
 * Author: Erik Agren
 * 6/5/2017
 * Custom array adapter from BLE_Devices
 */

class DeviceAdapter extends ArrayAdapter<BLE_Device> {


    private ArrayList<BLE_Device> device_list;
    private ArrayList<String> addresses;
    private int resourceId;

    DeviceAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<BLE_Device> objects) {
        super(context, resource, objects);
        resourceId = resource;
        device_list = objects;
        addresses = new ArrayList<>();
    }

    /**
     * Adds a device to the listview
     */
    void addDevice(BluetoothDevice device, int rssi){
        BLE_Device new_device = new BLE_Device(device, rssi);
        String address = new_device.getAddress();
        if(!addresses.contains(address)) {
            //if not currently in the list of addresses, add it
            device_list.add(new_device);
            addresses.add(address);
        }
        else{
            //if it already exists, update the rssi
            device_list.get(addresses.indexOf(address)).setRSSI(rssi);
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(resourceId, parent, false);
        }

        BLE_Device device = device_list.get(position);

        TextView nameView = (TextView) convertView.findViewById(R.id.device_name);
        TextView addressView = (TextView) convertView.findViewById(R.id.address);
        TextView rssiView = (TextView) convertView.findViewById(R.id.rssi);

        String name = device.getName();
        String address = device.getAddress();
        int rssi = device.getRSSI();

        if (name != null && name.length() > 0) {
            nameView.setText(device.getName());
        }
        else {
            nameView.setText("Unknown Name");
        }

        if (address != null && address.length() > 0) {
            addressView.setText("Address: "+ device.getAddress());
        }
        else {
            addressView.setText("Unknown Address");
        }

        //custom text messages rather than using raw rssi number, which is harder to understand
        if (rssi < -80){
            rssiView.setText("Signal strength: Poor");
        }
        else if (rssi < -67){
            rssiView.setText("Signal strength: Good");
        }
        else{
            rssiView.setText("Signal strength: Excellent");
        }

//        rssiView.setText(Integer.toString(rssi));


        CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.checkBox);
        final int new_position = position;
        //Called if checkbox is checked/unckeched
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                device_list.get(new_position).setChecked(isChecked);

            }
        });

        return convertView;

    }

    /**
     * Clears the addresses and device list for a new scan
     */
    void newScan(){

        addresses.clear();
        device_list.clear();
    }

    /**
     * Gets the number of checked devices
     */
    int getNumberChecked(){
        int numChecked = 0;
        for (BLE_Device device: device_list){
            if (device.isChecked()){
                numChecked++;
            }
        }
        return numChecked;
    }

    /**
     * Gets an arraylist of the addresses of all the currently checked devices
     */
    ArrayList<String> getCheckedAddresses(){
        ArrayList<String> checked_addresses = new ArrayList<>();
        for (BLE_Device device: device_list){
            if (device.isChecked()){
                checked_addresses.add(device.getAddress());
            }
        }
        return checked_addresses;
    }

    /**
     * Gets the names of all the currently checked devices
     */
    String[] getCheckedNames(){
        String[] checked_names = new String[device_list.size()];
        for (BLE_Device device : device_list){
            if (device.isChecked()){
                checked_names[device_list.indexOf(device)] = device.getName();
            }
        }
        return checked_names;
    }

}
