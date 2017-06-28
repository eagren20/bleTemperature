package eagren20.bletemperature;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
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
    private Button scan_button;
    private Button read_button;
    private ListView list;
    private TextView noneFound;
    private ProgressBar progress;
    // Stops scanning after 10 seconds.
//    private static final long SCAN_PERIOD = 10000;
    private static final long SCAN_PERIOD = 10000000;

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
        mHandler = new Handler();
        scan_button = (Button) findViewById(R.id.scan_button);
        read_button = (Button) findViewById(R.id.read_button);
        noneFound = (TextView) findViewById(R.id.noneFound);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setIndeterminate(true);

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

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
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

    public void scanClick(View view) {

        if (!scanning) {
            //start scan

            // Ensures Bluetooth is available on the device and it is enabled. If not,
            // displays a dialog requesting user permission to enable Bluetooth.
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }


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
            scanLeDevice(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        device_list.clear();
        adapter.notifyDataSetChanged();
        progress.setVisibility(View.GONE);
        read_button.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
//                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    stopScan();
                }
            }, SCAN_PERIOD);
            scanning = true;
            //TODO: probably use startScan(UUID[], ...) instead

//            List<ScanFilter> filterList;
//            filterList = new ArrayList<ScanFilter>();
//            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HT_SERVICE_UUID)).build();
//            filterList.add(filter);
//
//            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();


//            mBluetoothAdapter.getBluetoothLeScanner().startScan(filterList, settings, mLeScanCallback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);

        } else {
            scanning = false;
//            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            this.stopScan();
        }

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



    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    final int new_rssi = rssi;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // add to view
                            //TODO: add in check in UUIDs to make sure only temperature sensors are added

                            if (new_rssi > -90) {
                                adapter.addDevice(device, new_rssi);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };

//    private ScanCallback mLeScanCallback =
//            new ScanCallback() {
//                @Override
//                public void onScanResult(int callbackType, ScanResult result) {
//                    final int new_rssi = result.getRssi();
//                    final BluetoothDevice new_device = result.getDevice();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            // add to view
//                            //TODO: add in check in UUIDs to make sure only temperature sensors are added
//
//                            if (new_rssi > -90) {
//                                adapter.addDevice(new_device, new_rssi);
//                                adapter.notifyDataSetChanged();
//                            }
//                        }
//                    });
//                }
//            };
//

}
