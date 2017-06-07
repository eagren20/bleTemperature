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
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by eagre on 6/5/2017.
 */

public class DeviceAdapter extends ArrayAdapter<BLE_Device> {


    ArrayList<BLE_Device> device_list;
    ArrayList<String> addresses;
    Context context;
    int resourceId;

    public DeviceAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<BLE_Device> objects) {
        super(context, resource, objects);
        this.context = context;
        resourceId = resource;
        device_list = objects;
        addresses = new ArrayList<>();
    }

    public void addDevice(BluetoothDevice device, int rssi){
        BLE_Device new_device = new BLE_Device(device, rssi);
        String address = new_device.getAddress();
        if(!addresses.contains(address)) {
            device_list.add(new_device);
            addresses.add(address);
        }
        else{
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

        if (rssi < -80){
            rssiView.setText("Signal strength: Poor");
        }
        else if (rssi < -70){
            rssiView.setText("Signal strength: Good");
        }
        else{
            rssiView.setText("Signal strength: Excellent");
        }


        return convertView;

    }

    public void newScan(){
        addresses.clear();
    }

}
