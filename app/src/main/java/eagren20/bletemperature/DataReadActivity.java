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
import android.os.Handler;
import android.os.Message;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class DataReadActivity extends AppCompatActivity {

    private final static String TAG = DataReadActivity.class.getSimpleName();

    private DecimalFormat df;
    private ListView list;
    private ReadAdapter adapter;
    private ArrayList<String> addresses;
    private String[] deviceNames;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private Float[] dataArray;
    private BluetoothGatt[] gattArray;
    private BluetoothDevice[] deviceArray;
    private DBHelper database;
//    private Semaphore[] semaphores;

    private static final float DEVICE_DISCONNECTED = -1.00f;
    private static final float DEVICE_CONNECTED = -2.00f;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int REQUEST_ENABLE_BT = 1;

    private final static int HIDE_MSB_8BITS_OUT_OF_32BITS = 0x00FFFFFF;
    private final static int HIDE_MSB_8BITS_OUT_OF_16BITS = 0x00FF;
    private final static int SHIFT_LEFT_8BITS = 8;
    private final static int SHIFT_LEFT_16BITS = 16;
    private final static int GET_BIT24 = 0x00400000;
    private final static int FIRST_BIT_MASK = 0x01;

//    static private Semaphore semaphore;

    public int changeCount;
    public int viewCount;

    public final static UUID HT_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    public final static UUID INTERMEDIATE_TEMP_UUID = UUID.fromString("00002A1E-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

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
            if (status == BluetoothGatt.GATT_FAILURE){
                Log.w(TAG, "GATT Connection Failed");
            }
            else {
                if (newState == STATE_DISCONNECTED) {

                    Message msg = Message.obtain(handler);
                    msg.obj = DEVICE_DISCONNECTED;
                    msg.arg1 = index;
                    msg.what = 1;
                    msg.sendToTarget();

//                    dataArray[index] = "Disconnected";
//                    list.post(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            adapter.notifyDataSetChanged();
//                        }
//                    });
                } else {
                    Message msg = Message.obtain(handler);
                    msg.obj = DEVICE_CONNECTED;
                    msg.arg1 = index;
                    msg.what = 1;
                    msg.sendToTarget();

//                    dataArray[index] = "Connected: Waiting for data";
//                    list.post(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            adapter.notifyDataSetChanged();
//                        }
//                    });

                    gatt.discoverServices();
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
                //turn on notifications for intermediate temperature
                BluetoothGattCharacteristic characteristic = gatt.getService(HT_SERVICE_UUID).
                        getCharacteristic(INTERMEDIATE_TEMP_UUID);

                gatt.setCharacteristicNotification(characteristic, true);

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);

                Log.d(TAG, "notifications enabled for device #" + Integer.toString(index));
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated
         *                       remote device.
         * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         * <p>
         * <p>If this callback is invoked while a reliable write transaction is
         * in progress, the value of the characteristic represents the value
         * reported by the remote device. An application should compare this
         * value to the desired value to be written. If the values don't match,
         * the application must abort the reliable write transaction.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#writeCharacteristic}
         * @param characteristic Characteristic that was written to the associated
         *                       remote device.
         * @param status         The result of the write operation
         *                       {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //String address = gatt.getDevice().getAddress();
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt           GATT client the characteristic is associated with
         * @param characteristic Characteristic that has been updated as a result
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //super.onCharacteristicChanged(gatt, characteristic);
            changeCount++;
            //update UI with newly received data
            byte[] data = characteristic.getValue();
            int index = addresses.indexOf(gatt.getDevice().getAddress());
            if (data != null && data.length > 0) {

                Message msg = Message.obtain(handler);
                msg.obj = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT,1);
                msg.what = 0;
                msg.arg1 = index;
                msg.sendToTarget();
            }
            else{
                Log.w(TAG, "null data received from device #" + Integer.toString(index));
            }

        }

        /**
         * Callback reporting the result of a descriptor read operation.
         *
         * @param gatt       GATT client invoked {@link BluetoothGatt#readDescriptor}
         * @param descriptor Descriptor that was read from the associated
         *                   remote device.
         * @param status     {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        /**
         * Callback indicating the result of a descriptor write operation.
         *
         * @param gatt       GATT client invoked {@link BluetoothGatt#writeDescriptor}
         * @param descriptor Descriptor that was writte to the associated
         *                   remote device.
         * @param status     The result of the write operation
         *                   {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };

    // setup UI handler
    private final static int UPDATE_DATA = 0;
    private final static int SEND_DATA= 1;
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;
            final Float value = (Float) msg.obj;
            switch(what) {
                case UPDATE_DATA: updateData(value, msg.arg1); break;
                case SEND_DATA: updateConnection(value, msg.arg1); break;
            }
        }
    };

    private void updateData(Float data, int index){
        dataArray[index] = data;
        adapter.notifyDataSetChanged();
        // add data to DB
        database.addDataRow(deviceNames[index], data);
    }

    private void updateConnection(Float connectionStatus, int index){
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

        Toolbar mToolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);

        addresses = new ArrayList<>();


        database = new DBHelper(this, null, null, 1);

        df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.UP);

        Bundle bundle = this.getIntent().getExtras();
        addresses = bundle.getStringArrayList(MainActivity.EXTRAS_CHECKED_ADDRESSES);
        deviceNames = bundle.getStringArray(MainActivity.EXTRAS_DEVICE_NAMES);

        int size = addresses.size();
        deviceNames = new String[size];
        //test addresses
//        addresses.add("A0:E6:F8:4C:2B:53");
//        addresses.add("A0:E6:F8:4C:2B:63");
//        addresses.add("A0:E6:F8:53:DD:75"); //best
//
//        deviceNames[0] = "sensor_63";
//        deviceNames[1] = "sensor_75";

        //Register filter for bluetooth state changes
        IntentFilter iFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, iFilter);

        list = (ListView) findViewById(R.id.read_list);
        dataArray = new Float[addresses.size()];
        gattArray = new BluetoothGatt[size];
        deviceArray = new BluetoothDevice[size];
//        semaphores = new Semaphore[addresses.size()];

        viewCount = 0;
        changeCount = 0;


//        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            public void onItemClick(AdapterView<?> parent, View view,
//                                    int position, long id) {
//                if (dataArray[position].equals("Disconnected")) {
//                    String address = addresses.get(position);
//                    Toast.makeText(getApplicationContext(), "item " + Integer.toString(position) +
//                            " clicked", Toast.LENGTH_SHORT).show();
//                    connect(address);
//                }
//            }
//        });

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
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
        this.startConnection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mMenuInflater = getMenuInflater();
        mMenuInflater.inflate(R.menu.data_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

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
        super.onStop();
        unregisterReceiver(mReceiver);
        close();
    }

    private void startConnection() {
        //initialize bluetoothgatts for each address
        //called once, when the activity starts

        for (String address : addresses){
            if (mBluetoothAdapter == null || address == null) {
                Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.");
                return;
            }
            // creates a semaphore for each ble device. decrements upon a data update
            // and increments upon that upon that update being reflected in the UI
//            semaphores[addresses.indexOf(address)] = new Semaphore(1);
            connect(address);
        }


    }

    private void connect(String address){
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
        Log.d(TAG, "Trying to create a new connection to address " + address);
    }

    public void reconnect(View v){

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Toast.makeText(getApplicationContext(), "Reconnecting", Toast.LENGTH_SHORT).show();
        for (int i = 0; i < gattArray.length; i++) {
            if (dataArray[i] == STATE_DISCONNECTED) {
                deviceArray[i].connectGatt(this, true, mGattCallback);
                Log.d(TAG, "Trying to create a new connection to address " +
                        deviceArray[i].getAddress());
            }
        }


    }

    private void beginReading(){

    }

    public void close(){
        for (BluetoothGatt gatt : gattArray){
            if (gatt == null) {
                continue;
            }
            gatt.disconnect();
            gatt.close();
            gatt = null;
            Log.d(TAG, "GATTs closed");
        }
    }


    private class ReadAdapter extends ArrayAdapter<String> {

        Context context;
        int resourceId;
        public ReadAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull ArrayList<String> objects) {
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
                String name = deviceNames[position];
                TextView nameView = (TextView) convertView.findViewById(R.id.read_device_name);
                if (name == null){
                    nameView.setText("No Name");
                }
                else {
                    nameView.setText("Name: " + name);
                }
            }

            String data;
            TextView temperature = (TextView) convertView.findViewById(R.id.temperature);
            final float currentData = dataArray[position];
            if (currentData == 0.0f || currentData == DEVICE_DISCONNECTED){
                temperature.setText("Disconnected");
            }
            else if (currentData == DEVICE_CONNECTED){
                temperature.setText("Connected: Waiting for data");
            }
            else{
                data = Float.toString(dataArray[position]);
                temperature.setText("Temperature: " + data + "\u00B0" + "C");
            }
            return convertView;
        }



        public void addData(String address, Float data){

            int position = addresses.indexOf(address);
            dataArray[position] = data;
            adapter.notifyDataSetChanged();
        }



    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_TURNING_OFF){
                    Toast.makeText(getApplicationContext(), "BT turning off", Toast.LENGTH_SHORT).show();
                }
                if (state == BluetoothAdapter.STATE_OFF){
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    };

    /**
     * This method decode temperature value received from Health Thermometer device First byte {0} of data is flag and first bit of flag shows unit information of temperature. if bit 0 has value 1
     * then unit is Fahrenheit and Celsius otherwise Four bytes {1 to 4} after Flag bytes represent the temperature value in IEEE-11073 32-bit Float format
     */
    private double decodeTemperature(byte[] data) throws Exception {
        double temperatureValue = 0.0;
        byte flag = data[0];
        byte exponential = data[4];
        short firstOctet = convertNegativeByteToPositiveShort(data[1]);
        short secondOctet = convertNegativeByteToPositiveShort(data[2]);
        short thirdOctet = convertNegativeByteToPositiveShort(data[3]);
        int mantissa = ((thirdOctet << SHIFT_LEFT_16BITS) | (secondOctet << SHIFT_LEFT_8BITS) | (firstOctet)) & HIDE_MSB_8BITS_OUT_OF_32BITS;
        mantissa = getTwosComplimentOfNegativeMantissa(mantissa);
        temperatureValue = (mantissa * Math.pow(10, exponential));
		/*
		 * Conversion of temperature unit from Fahrenheit to Celsius if unit is in Fahrenheit
		 * Celsius = (98.6*Fahrenheit -32) 5/9
		 */
        if ((flag & FIRST_BIT_MASK) != 0) {
            temperatureValue = (float) ((98.6 * temperatureValue - 32) * (5 / 9.0));
        }
        return temperatureValue;
    }

    private short convertNegativeByteToPositiveShort(byte octet) {
        if (octet < 0) {
            return (short) (octet & HIDE_MSB_8BITS_OUT_OF_16BITS);
        } else {
            return octet;
        }
    }

    private int getTwosComplimentOfNegativeMantissa(int mantissa) {
        if ((mantissa & GET_BIT24) != 0) {
            return ((((~mantissa) & HIDE_MSB_8BITS_OUT_OF_32BITS) + 1) * (-1));
        } else {
            return mantissa;
        }
    }

}
