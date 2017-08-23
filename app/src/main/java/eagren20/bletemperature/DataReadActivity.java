package eagren20.bletemperature;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Author: Erik Agren
 * 06/2017
 * Reads data from all selected sensors simultaneously
 */

public class DataReadActivity extends AppCompatActivity {

    private final static String TAG = DataReadActivity.class.getSimpleName();

    //various views in the activity
    private ListView list;
    private TextView header;
    private Button bottomButton;
    //used to add devices to the listview
    private ReadAdapter adapter;
    //the list of names/addresses to read from
    private ArrayList<String> addresses;
    private String[] deviceNames;
    //1 if services have been discovered for the device at that index, 0 if not
    private int[] servicesArray;
    //Bluetooth Gatts for each device
    private BluetoothGatt[] gattArray;
    //used for various bluetooth operations
    private BluetoothAdapter mBluetoothAdapter;
    //Current data for each device
    private float[] dataArray;

    private int numDevices;
    private BluetoothDevice[] deviceArray;
    private DBHelper database;

    private boolean reading;
    //SharedPreferences, used in this case to remember the number of devices
    private SharedPreferences sharedpreferences;
    private SharedPreferences.Editor editor;

    //various static fields
    private static final float DEVICE_DISCONNECTED = -1.00f;
    private static final float DEVICE_CONNECTED = -2.00f;
    private static final int STATE_DISCONNECTED = 0;
    private static final int REQUEST_ENABLE_BT = 1;

    private static final long CHECK_PERIOD = 1000;
    private static final long CHECK_CONNECTION_DELAY = 690;
    private static final long CHECK_SERVICES_DELAY = 1100;

    public static final String EXTRAS_DATABASE_STRING = "DB_STRING";
    public static final String SP_numDevices = "SP_num";
    public static final String PREFERNCES = "SP_preferences";

    private static final String HEADER_CONNECTING_MSG = "Connecting to all devices...";
    private static final String HEADER_READINGS_MSG = "Temperature readings:";
    private static final String HEADER_DISCOVERING_MSG = "Discovering Services...";
    private static final String HEADER_DIFF_NUMBER_MSG = "Attempting to read data from a " +
            "different number of sensors: Must clear database before continuing";
    private static final String HEADER_DEFAULT_MSG = "Devices:";
    private static final String BOTTOM_RECONNECT_MSG = "Reconnect";
    private static final String NAMEVIEW_NONAME = "No Name";
    private static final String TEMP_DISCONNECTED = "Disconnected";
    private static final String TEMP_WAITING = "Connected: Waiting...";

    public final static UUID HT_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    public final static UUID INTERMEDIATE_TEMP_UUID = UUID.fromString("00002A1E-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Callback for when something happens to a bluetooth gatt object (state change, characteristic
     * change, etc)
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote
         * GATT server.
         *
         * @param gatt     GATT client
         * @param status   Status of the connect or disconnect operation.
         *                 {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         * @param newState Returns the new connection state. Can be one of
         *                 {@link BluetoothProfile#STATE_DISCONNECTED} or
         *                 {@link BluetoothProfile#STATE_CONNECTED}
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //super.onConnectionStateChange(gatt, status, newState);
            int index = addresses.indexOf(gatt.getDevice().getAddress());
            gattArray[index] = gatt;
            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.w(TAG, "GATT Connection Failed");
            } else {
                if (newState == STATE_DISCONNECTED) {
                    //the device is disconnected, so update the connection status
                    Message msg = Message.obtain(handler);
                    msg.obj = DEVICE_DISCONNECTED;
                    msg.arg1 = index;
                    msg.what = UPDATE_CONNECTION;
                    msg.sendToTarget();

                } else {
                    //the device successfully connected, so update the connection status
                    Message msg = Message.obtain(handler);
                    msg.obj = DEVICE_CONNECTED;
                    msg.arg1 = index;
                    msg.what = UPDATE_CONNECTION;
                    msg.sendToTarget();
                }
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors
         * for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#discoverServices}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //super.onServicesDiscovered(gatt, status);
            int index = addresses.indexOf(gatt.getDevice().getAddress());

            if (status == BluetoothGatt.GATT_SUCCESS) {
                gattArray[index] = gatt;
                servicesArray[index] = 1;
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt           GATT client the characteristic is associated with
         * @param characteristic Characteristic that has been updated as a result
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            //update UI with newly received data
            int index = addresses.indexOf(gatt.getDevice().getAddress());
//            Log.d(TAG, "oCC received, device " + Integer.toString(index));
            Date curDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss:SS");
            Reading reading = new Reading(characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1),
                    format.format(curDate));
            //create a new message to send to the main thread's message queue
            Message msg = Message.obtain(handler);
            msg.obj = reading;
            msg.what = UPDATE_DATA;
            msg.arg1 = index;
            msg.sendToTarget();
        }
    };

    // setup UI handler
    private final static int UPDATE_DATA = 0;
    private final static int UPDATE_CONNECTION = 1;
    //Getting a warning that suggests making the Handler static due to the possibility of memory
    //leaks, but it seems like that can only happen when posting messages with a long delay so I
    //don't think it's a big deal
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;

            if (msg.obj != null) {
                switch (what) {
                    case UPDATE_DATA:
                        updateData(msg.obj, msg.arg1);
                        break;
                    case UPDATE_CONNECTION:
                        updateConnection(msg.obj, msg.arg1);
                        break;
                }
            } else {
                Log.e(TAG, "device " + deviceNames[msg.arg1] + " sent a null value");
            }

        }
    };

    /**
     * Updates the data being displayed in the listview and adds the new data to the database
     */
    private void updateData(Object obj, int index) {
        final Reading reading = (Reading) obj;
        float data = reading.getData();
        String timestamp = reading.getTimestamp();
        dataArray[index] = data;
        adapter.notifyDataSetChanged();

//        Log.d(TAG, "preparing to add row, device " + Integer.toString(index));
        // Add the new data to DB table. The index is added to the device name
        // because it makes the names unique in the case of a duplicate
        database.addDataRow(deviceNames[index], data, timestamp);
    }

    /**
     * Updates the connection status of a device in the listview
     */
    private void updateConnection(Object obj, int index) {
        final float connectionStatus = (Float) obj;
        dataArray[index] = connectionStatus;
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read_activity);

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        addresses = new ArrayList<>();
        database = new DBHelper(this);

        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.UP);

        //get the addresses and devicenames from the previous activity
        Bundle bundle = this.getIntent().getExtras();
        addresses = bundle.getStringArrayList(MainActivity.EXTRAS_CHECKED_ADDRESSES);
        assert addresses != null;
        numDevices = addresses.size();
        deviceNames = bundle.getStringArray(MainActivity.EXTRAS_DEVICE_NAMES);
        //add the index to the device name as a unique identifier
        assert deviceNames != null;
        for (int i = 0; i < numDevices; i++){
            if (deviceNames[i] == null){
                deviceNames[i] = "No Name";
            }
            deviceNames[i] = deviceNames[i]+Integer.toString(i);
        }

//        test addresses/device names. Hardcode here and comment out previous block if
//        bypassing scanning

//        addresses.add("A0:E6:F8:4C:2B:63");
//        addresses.add("A0:E6:F8:53:DD:75");
//        addresses.add("A0:E6:F8:4C:2B:53");
//        numDevices = addresses.size();
//        deviceNames = new String[numDevices];
//        deviceNames[0] = "sensor 631";
//        deviceNames[1] = "sensor 752";
//        deviceNames[2] = "sensor 533";

        //initialize a bunch of stuff
        bottomButton = (Button) findViewById(R.id.startButton);
        reading = false;
        header = (TextView) findViewById(R.id.read_header);
        list = (ListView) findViewById(R.id.read_list);

        sharedpreferences = getSharedPreferences(PREFERNCES, Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();

        dataArray = new float[numDevices];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = DEVICE_DISCONNECTED;
        }
        gattArray = new BluetoothGatt[numDevices];
        deviceArray = new BluetoothDevice[numDevices];
        servicesArray = new int[numDevices];

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return;
        }

        adapter = new ReadAdapter(this, R.layout.read_row, addresses);
        list.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.read_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_data) {
            //Data option from menu was selected so start a DatabaseActivity
            Intent intent = new Intent(this, DatabaseActivity.class);
            Bundle bundle = new Bundle();
            int numDevices = addresses.size();
            bundle.putInt(EXTRAS_DATABASE_STRING, numDevices);
            bundle.putStringArray(MainActivity.EXTRAS_DEVICE_NAMES, deviceNames);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = DEVICE_DISCONNECTED;
        }
        unregisterReceiver(mReceiver);
        close();
        super.onStop();
    }

    @Override
    protected void onResume() {
        //Register filter for bluetooth state changes
        IntentFilter iFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, iFilter);
        checkNumDevices();
        super.onResume();
    }

    /**
     * Called once at the beginning of reading, loop through each address and connect to it
     */
    private void startConnection() {
        editor.putInt(SP_numDevices, numDevices);
        editor.commit();

        header.setText(HEADER_CONNECTING_MSG);
        for (String address : addresses) {
            if (mBluetoothAdapter == null || address == null) {
                Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.");
                return;
            }
            connect(address);
        }
        //Check if all devices have been connected to after CHECK_PERIOD amount of time
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkConnections();
            }
        }, CHECK_PERIOD);

    }

    /**
     * Connects to an addresses, getting a BluetoothGatt for it
     * @param address The address to connect to
     */
    private void connect(String address) {
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        BluetoothGatt mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        int index = addresses.indexOf(address);
        gattArray[index] = mBluetoothGatt;
        deviceArray[index] = device;
//        Log.d(TAG, "Trying to create a new connection to address " + address);
    }

    /**
     * Called when the bottom button is clicked.
     */
    public void buttonClick(View v) {

        if (!reading) {
            reading = true;
            bottomButton.setText(BOTTOM_RECONNECT_MSG);
            startConnection();
        } else {
            reading = false;
            this.recreate();
        }

    }

    /**
     * Checks that all devices have been connected to. Repeats every CHECK_PERIOD millisconds
     */
    private void checkConnections() {
        Toast.makeText(getApplicationContext(), "Verifying connections...", Toast.LENGTH_SHORT).show();
        boolean all_connected = true;
        for (int i = 0; i < dataArray.length; i++) {
            if (dataArray[i] == DEVICE_DISCONNECTED) {
                deviceArray[i].connectGatt(this, true, mGattCallback);
                all_connected = false;
            }
        }

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                checkConnections();
            }
        };

        //if all devices have been connected to, move on to checking that services have been
        //discovered for each device
        if (all_connected) {
            handler.removeCallbacks(r);
            header.setText(HEADER_DISCOVERING_MSG);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkServicesDiscovered();
                }

            }, CHECK_CONNECTION_DELAY);

        } else {
            handler.postDelayed(r, CHECK_PERIOD);
        }
    }

    /**
     * Checks that services have been discovered for each device. Repeats every CHECK_SERVICES_DELAY
     * milliseconds
     */
    private void checkServicesDiscovered() {
        Toast.makeText(getApplicationContext(), "Discovering services...", Toast.LENGTH_SHORT).show();
        boolean allServicesDiscovered = true;
        for (int i = 0; i < servicesArray.length; i++) {
            if (servicesArray[i] == 0) {
                allServicesDiscovered = false;
                gattArray[i].discoverServices();
            }
        }
        //If services have been discovered for every device, enable notifications for all devices
        if (allServicesDiscovered) {
            enableNotifications();
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkServicesDiscovered();
                }
            }, CHECK_SERVICES_DELAY);
        }
    }

    /**
     * Enables notifications for the Intermediate_temp of every device
     */
    private void enableNotifications() {
        for (BluetoothGatt gatt : gattArray) {
            //turn on notifications for intermediate temperature
            BluetoothGattCharacteristic characteristic = gatt.getService(HT_SERVICE_UUID).
                    getCharacteristic(INTERMEDIATE_TEMP_UUID);

            gatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

        }
        header.setText(HEADER_READINGS_MSG);
    }

    /**
     * Closes all BluetoothGatts
     */
    private void close() {
        for (BluetoothGatt gatt : gattArray) {
            if (gatt == null) {
                continue;
            }
            gatt.disconnect();
            gatt.close();
//            Log.d(TAG, "GATTs closed");
        }
    }

    /**
     * Checks that the user is not attempting to read from a different number of devices than
     * the number that was used in the data that currently exists in the database
     */
    private void checkNumDevices() {
        int prev_numDevices = sharedpreferences.getInt(SP_numDevices, -1);
        if (prev_numDevices != -1) {
            //check to make sure that the db is clear if there is a
            // different number of devices from last time
            if (prev_numDevices != numDevices && !database.isEmpty()) {
                //database must be cleared before collecting data
                header.setText(HEADER_DIFF_NUMBER_MSG);
                bottomButton.setEnabled(false);
                list.setVisibility(View.GONE);

            } else {
                header.setText(HEADER_DEFAULT_MSG);
                bottomButton.setEnabled(true);
                list.setVisibility(View.VISIBLE);
            }
        }

    }

    /**
     * Represents a data reading from a sensor
     */
    private class Reading {
        Float data;
        String timestamp;

        Reading(Float data, String timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        public Float getData() {
            return data;
        }

        String getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Adapter used to work with the activity's listview
     */
    private class ReadAdapter extends ArrayAdapter<String> {

        Context context;
        int resourceId;

        ReadAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<String> objects) {
            super(context, resource, objects);

            this.context = context;
            this.resourceId = resource;

        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(resourceId, parent, false);
                TextView nameView = (TextView) convertView.findViewById(R.id.read_device_name);
                if (deviceNames[position] == null) {
                    nameView.setText(NAMEVIEW_NONAME);
                    deviceNames[position] = "No Name";
                } else {
                    String name = DatabaseActivity.removeUID(deviceNames[position]);
                    nameView.setText(name);
                }
            }

            String data;
            TextView temperature = (TextView) convertView.findViewById(R.id.temperature);
            final float currentData = dataArray[position];
            if (currentData == DEVICE_DISCONNECTED) {
                temperature.setText(TEMP_DISCONNECTED);
            } else if (currentData == DEVICE_CONNECTED) {
                temperature.setText(TEMP_WAITING);
            } else {
                data = Float.toString(dataArray[position]);
                temperature.setText("Temperature: " + data + "\u00B0" + "C");
            }
            return convertView;
        }
    }

    /**
     * Broadcast receiver that detects changes to the devices bluetooth
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    Toast.makeText(getApplicationContext(), "BT turning off", Toast.LENGTH_SHORT).show();
                }
                if (state == BluetoothAdapter.STATE_OFF) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    };
}