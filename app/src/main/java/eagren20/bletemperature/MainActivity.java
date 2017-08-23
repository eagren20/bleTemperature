package eagren20.bletemperature;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    //List of scanned devices
    private ArrayList<BLE_Device> device_list;
    //Custom array adapter for the device listview
    private DeviceAdapter adapter;
    //Views in the main activity
    private Button scan_button;
    private Button read_button;
    private ListView list;
    private TextView noneFound;
    private ProgressBar progress;
    //Bluetooth scan parameters
    private List<ScanFilter> filterList;
    private ScanSettings settings;
    //Used for bluetooth related functions
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner btScanner;

    private Handler mHandler;
    private boolean scanning;
    private boolean btEnable;

    //The length of the scanning periods
    private static final long SCAN_PERIOD = 10000;
    private static final long POST_PERIOD = 1100;

    private static final int REQUEST_ENABLE_BT = 1;

    public static final int NUM_DEVICES_UNKNOWN = -1;

    //String constants
    public static final String EXTRAS_CHECKED_ADDRESSES = "CHECKED_ADDRESSES";
    public static final String EXTRAS_DEVICE_NAMES = "DEVICE_NAMES";
    public final static UUID HT_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");

    /**
     * Called upon creating the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize variables
        device_list = new ArrayList<>();
        list = (ListView) findViewById(R.id.device_list);
        adapter = new DeviceAdapter(this, R.layout.scan_row, device_list);
        list.setAdapter(adapter);
        scanning = false;
        btEnable = false;
        mHandler = new Handler();
        scan_button = (Button) findViewById(R.id.scan_button);
        read_button = (Button) findViewById(R.id.read_button);
        noneFound = (TextView) findViewById(R.id.noneFound);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setIndeterminate(true);

        //Initialize scan settings
        filterList = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HT_SERVICE_UUID)).build();
        filterList.add(filter);
        settings = new ScanSettings.Builder().setScanMode(
                ScanSettings.SCAN_MODE_BALANCED).build();

        //If the sdk is past 23 the user needs to grant the app location access
        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                    Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
                }else{
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }else{
                Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
            }
        }

        //Register filter for bluetooth state changes (to detect if bluetooth turns off)
        IntentFilter iFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, iFilter);
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            btAdapter = bluetoothManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        checkBT();
    }
    /**
     * Initialize the contents of the Activity's standard options menu.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.read_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * @param item The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_data) {
            Intent intent = new Intent(this, DatabaseActivity.class);
            Bundle bundle = new Bundle();;
            bundle.putInt(DataReadActivity.EXTRAS_DATABASE_STRING, NUM_DEVICES_UNKNOWN);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Dispatch incoming result to the correct fragment.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Ensures Bluetooth is available on the device and it is enabled. If not,
     * displays a dialog requesting user permission to enable Bluetooth.
     */
    public void checkBT(){
        //
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Called when the "Scan" button is clicked. Checks that bluetooth is enabled and 
     * @param view
     */
//    public void scanClick(View view) {
//
//        if (!scanning) {
//            //start scan
//
//            checkBT();
//
//            device_list.clear();
//            adapter.newScan();
//            adapter.notifyDataSetChanged();
//            read_button.setVisibility(View.GONE);
//            noneFound.setVisibility(View.GONE);
//            progress.setVisibility(View.VISIBLE);
//
//            scan_button.setText("Stop");
//            scanLeDevice(true);
//
//        } else {
//            //stop the scan
//            Toast.makeText(getApplicationContext(), "Ending scan", Toast.LENGTH_SHORT).show();
//            scanLeDevice(false);
//        }
//    }

    public void scanClick(View view){
        Intent intent = new Intent(this, DataReadActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkBT();

        btScanner = btAdapter.getBluetoothLeScanner();
        progress.setVisibility(View.GONE);
        scan_button.setEnabled(true);
        read_button.setEnabled(true);

        scan_button.setText("Scan");
        scanning = false;
        btEnable = false;
        //Register filter for bluetooth state changes
        IntentFilter iFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, iFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    btAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
//                    btAdapter.stopLeScan(mLeScanCallback);
                    stopScan();
                }
            }, SCAN_PERIOD);
            scanning = true;

            btAdapter.getBluetoothLeScanner().startScan(filterList, settings, mLeScanCallback);
//            btAdapter.startLeScan(mLeScanCallback);

        } else {
            scanning = false;
            btAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
//            btAdapter.stopLeScan(mLeScanCallback);
            this.stopScan();
        }

    }

//    private void scanLeDevice(final boolean enable) {
//        if (enable) {
//            scanning = true;
//            btScanner.startScan(filterList, settings, mLeScanCallback);
//            scanPost();
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    //Stop scanning after 10 seconds
//                    scanLeDevice(false);
//                }
//            }, SCAN_PERIOD);
//        } else {
//            scanning = false;
//            btScanner.stopScan(mLeScanCallback);
//            mHandler.removeCallbacksAndMessages(null);
////            btAdapter.stopLeScan(mLeScanCallback);
//            this.stopScan();
//        }
//    }

    private void scanPost(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (btAdapter != null && btAdapter.isEnabled()) {
                    btScanner.stopScan(mLeScanCallback);
//                btAdapter.stopLeScan(mLeScanCallback);
                    btScanner.startScan(filterList, settings, mLeScanCallback);
//                btAdapter.startLeScan(mLeScanCallback);
                    scanPost();
                }

            }
        }, POST_PERIOD);

    }

    private void stopScan() {
        if (device_list.isEmpty()) {
            noneFound.setVisibility(View.VISIBLE);
        } else {
            read_button.setVisibility(View.VISIBLE);
        }
        progress.setVisibility(View.GONE);
        scan_button.setText("Scan");

    }

    /**
     * Called when the read_button is clicked
     * @param view the calling view
     */
    public void readClick(View view){

        //get the number of checked sensors
        int numChecked = adapter.getNumberChecked();

        //if no sensors have been checked, inform the user
        if (numChecked == 0){
            //toast
            Toast.makeText(this, "No sensors have been selected", Toast.LENGTH_SHORT).show();
        }
        else{
            Intent intent = new Intent(this, DataReadActivity.class);
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(EXTRAS_CHECKED_ADDRESSES, adapter.getCheckedAddresses());
            bundle.putStringArray(EXTRAS_DEVICE_NAMES, adapter.getCheckedNames());

            intent.putExtras(bundle);
            startActivity(intent);
        }
    }



//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//                @Override
//                public void onLeScan(final BluetoothDevice device, int rssi,
//                                     byte[] scanRecord) {
//                    final int new_rssi = rssi;
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            // add to view
//                            //TODO: add in check in UUIDs to make sure only temperature sensors are added
//
//                            if (new_rssi > -90) {
//                                adapter.addDevice(device, new_rssi);
//                                adapter.notifyDataSetChanged();
//                            }
//                        }
//                    });
//                }
//            };

    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    final int new_rssi = result.getRssi();
                    final BluetoothDevice new_device = result.getDevice();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // add to view
                            //TODO: add in check in UUIDs to make sure only temperature sensors are added

                            if (new_rssi > -90) {
                                adapter.addDevice(new_device, new_rssi);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //TODO: fix this garbo
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_TURNING_OFF){
                    Toast.makeText(getApplicationContext(), "BT turning off", Toast.LENGTH_SHORT).show();
                    mHandler.removeCallbacksAndMessages(null);
                    scan_button.setEnabled(false);
                    read_button.setEnabled(false);
                }
                if (state == BluetoothAdapter.STATE_OFF && !btEnable){
                    btEnable = true;
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                if (state == BluetoothAdapter.STATE_ON){
                    scan_button.setEnabled(true);
                    read_button.setEnabled(true);
                    if (scanning) {
                        scanLeDevice(true);
                    }
                }

            }
        }
    };


}
