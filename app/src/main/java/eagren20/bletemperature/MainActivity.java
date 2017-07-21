package eagren20.bletemperature;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
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
import android.support.v7.widget.ListViewCompat;
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

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BLE_Device> device_list;
    private DeviceAdapter adapter;
    private Handler mHandler;
    private boolean scanning;
    private boolean btEnable;
    private Button scan_button;
    private Button read_button;
    private ListView list;
    private TextView noneFound;
    private ProgressBar progress;
    private List<ScanFilter> filterList;
    private ScanSettings settings;
    private BluetoothLeScanner scanner;

    // Stops scanning after 10 seconds.
//    private static final long SCAN_PERIOD = 10000;
    private static final long SCAN_PERIOD = 10000;
    private static final long POST_PERIOD = 1100;

    public static final String EXTRAS_CHECKED_ADDRESSES = "CHECKED_ADDRESSES";
    public final static UUID HT_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        filterList = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HT_SERVICE_UUID)).build();
        filterList.add(filter);
        settings = new ScanSettings.Builder().setScanMode(
                ScanSettings.SCAN_MODE_BALANCED).build();

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

        //Register filter for bluetooth state changes
        IntentFilter iFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, iFilter);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        scanner = mBluetoothAdapter.getBluetoothLeScanner();

        checkBT();
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

    public void checkBT(){
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void scanClick(View view) {

        if (!scanning) {
            //start scan

            checkBT();

            device_list.clear();
            adapter.newScan();
            adapter.notifyDataSetChanged();
            read_button.setVisibility(View.GONE);
            noneFound.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);

            scan_button.setText("Stop");
            scanLeDevice(true);

        } else {
            //stop the scan
            Toast.makeText(getApplicationContext(), "Ending scan", Toast.LENGTH_SHORT).show();
            scanLeDevice(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkBT();

        scanner = mBluetoothAdapter.getBluetoothLeScanner();
        device_list.clear();
        adapter.notifyDataSetChanged();
        progress.setVisibility(View.GONE);
        read_button.setVisibility(View.GONE);

        scan_button.setText("Scan");
        scanning = false;
        btEnable = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

//    private void scanLeDevice(final boolean enable) {
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    scanning = false;
//                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
////                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    stopScan();
//                }
//            }, SCAN_PERIOD);
//            scanning = true;
//            //TODO: probably use startScan(UUID[], ...) instead
//
//            List<ScanFilter> filterList = new ArrayList<ScanFilter>();
////            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HT_SERVICE_UUID)).build();
////            filterList.add(filter);
//
//            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
//
//
//            mBluetoothAdapter.getBluetoothLeScanner().startScan(filterList, settings, mLeScanCallback);
////            mBluetoothAdapter.startLeScan(mLeScanCallback);
//
//        } else {
//            scanning = false;
//            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
////            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            this.stopScan();
//        }
//
//    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            scanning = true;
            scanner.startScan(filterList, settings, mLeScanCallback);
            scanPost();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Stop scanning after 10 seconds
                    scanLeDevice(false);
                }
            }, SCAN_PERIOD);
        } else {
            scanning = false;
            scanner.stopScan(mLeScanCallback);
            mHandler.removeCallbacksAndMessages(null);
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            this.stopScan();
        }
    }

    private void scanPost(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                    scanner.stopScan(mLeScanCallback);
//                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    scanner.startScan(filterList, settings, mLeScanCallback);
//                mBluetoothAdapter.startLeScan(mLeScanCallback);
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
