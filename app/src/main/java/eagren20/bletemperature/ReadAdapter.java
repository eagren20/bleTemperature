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
 * Created by eagre on 6/7/2017.
 */


public class ReadAdapter extends ArrayAdapter<String> {

    Context context;
    int resourceId;
    ArrayList<String> addresses;


    public ReadAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<String> objects) {
        super(context, resource, objects);

        this.context = context;
        this.resourceId = resource;
        addresses = objects;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(resourceId, parent, false);
        }

        String address = addresses.get(position);

        TextView temperature = (TextView) convertView.findViewById(R.id.temperature);
        TextView name = (TextView) convertView.findViewById(R.id.read_device_name);


        name.setText("Address: " + address);
        temperature.setText("Not implemented");

        return convertView;

    }


}