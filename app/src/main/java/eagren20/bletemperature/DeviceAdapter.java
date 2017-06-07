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

public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {


    ArrayList<BluetoothDevice> device_list;
    Context context;
    int resourceId;

    public DeviceAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<BluetoothDevice> objects) {
        super(context, resource, objects);
        this.context = context;
        resourceId = resource;
        device_list = objects;
    }

    public void addDevice(BluetoothDevice device){

        if(!device_list.contains(device)) {
            device_list.add(device);
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(resourceId, parent, false);
        }

        BluetoothDevice device = device_list.get(position);

        TextView nameView = (TextView) convertView.findViewById(R.id.device_name);
        TextView addressView = (TextView) convertView.findViewById(R.id.address);

        String name = device.getName();
        String address = device.getAddress();

        if (name != null && name.length() > 0) {
            nameView.setText(device.getName());
        }
        else {
            nameView.setText("Unknown Name");
        }

        if (address != null && address.length() > 0) {
            addressView.setText(device.getAddress());
        }
        else {
            addressView.setText("Unknown Address");
        }


        return convertView;

    }
}
