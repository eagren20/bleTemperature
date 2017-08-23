package eagren20.bletemperature;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author: Erik Agren
 * 7/25/2017
 * The activity that shows the database contents, as well as allows to delete or export the data
 */

public class DatabaseActivity extends AppCompatActivity {

    private static final String NUM_DEVICES_NOT_FOUND_MSG = "Number of devices not found: Please " +
            "read again. Possible causes include clearing the cache";
    private static final String DB_EMPTY_MSG = "Database is empty";
    private DBHelper database;
    private int numDevices;
    //Main body textview. Where the database contents is shown
    private TextView contents;
    //The header/title of the activity
    private TextView header;
    private String[] deviceNames;

    //The row in the database that is currently being read from
    private int currRow = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database_activity);

        contents = (TextView) findViewById(R.id.db_contents);
        header = (TextView) findViewById(R.id.db_header);

        database = new DBHelper(this);
        //get the number of devices and device names
        SharedPreferences sharedpreferences = getSharedPreferences(DataReadActivity.PREFERNCES,
                Context.MODE_PRIVATE);
        numDevices = sharedpreferences.getInt(DataReadActivity.SP_numDevices, -1);
        Bundle bundle = this.getIntent().getExtras();
        deviceNames = bundle.getStringArray(MainActivity.EXTRAS_DEVICE_NAMES);
        if (numDevices != -1) {
            printDB();
        }
        else{
            contents.setVisibility(View.GONE);
            header.setText(NUM_DEVICES_NOT_FOUND_MSG);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.data_export_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            //delete all database entries
            database.removeAll();
            printDB();
        } else if (id == R.id.action_export) {
            //export the data to a .csv file
            if (contents.getText().equals("")) {
                //the DB is empty, so let the user know and do nothing
                Toast.makeText(getApplicationContext(), "The database is empty", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            if (numDevices == -1) {
                Toast.makeText(getApplicationContext(), "Number of devices not found", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            SQLiteDatabase sqldb = database.getReadableDatabase();
            //Used to read from the database
            Cursor c;
            try {

                //SQL query that gets the entirety of the database
                c = sqldb.rawQuery("select * from " + DBHelper.TEMPERATURE_TABLE, null);

                int rowcount = 0;

                //Generate the file and its directory on the device
                String mDir = Environment.DIRECTORY_DOCUMENTS;
                File sdCardDir = Environment.getExternalStoragePublicDirectory(mDir);
                String filename = generateFilename();

                // the name of the file to export with
                File saveFile = new File(sdCardDir, filename);
                FileWriter fw = new FileWriter(saveFile);
                // Used to write Strings to the .csv file
                BufferedWriter bw = new BufferedWriter(fw);
                rowcount = c.getCount();



                if (rowcount > 0) {
                    c.moveToFirst();

                    /*
                     * .csv files have their columns seperated by commas and their rows seperated
                     * by newline characters
                     */

                    //Write column headers of the .csv file
                    bw.write("Time,");
                    writeColumnHeaders(c, bw);
                    bw.newLine();

                    /*write data to .csv file*/
                    c.moveToPosition(0);

                    //execute until all rows have been added to .csv file
                    while (currRow < rowcount) {

                        //write the time for the current row
                        bw.write(c.getString(1) + ",");

                        /*
                         * Used to order the data in the .csv file. Due to delays or other issues
                         * in data reading, the order that devices are read from in any given set
                         * of readings can be inconsistent. Therefore, it is necessary to order the
                         * data in each set of readings before adding it to the .csv file.
                         *
                         * deviceNames contains the names in the order they appear in the .csv file,
                         * while nameArray contains the names of the devices in the order of a
                         * particular set of readings. For each index of nameArray, the correct
                         * index is determined using deviceNames, and the corresponding data is
                         * inserted to dataArray at that index. Once dataArray has finished being
                         * generated (ordering is now correct), the row is added to the .csv file.
                         */
                        String[] nameArray = new String[numDevices];

                        int temp = currRow;
                        //Read through the set of readings and store the deviceNames in nameArray
                        for (int j = 0; j < numDevices; j++) {
                            String name = c.getString(2);
                            nameArray[j] = name;

                            if (temp < rowcount-1) {
                                c.moveToNext();
                                temp++;
                            }
                            else{
                                break;
                            }
                        }
                        c.moveToPosition(currRow);

                        //For each reading, match the name of the device with the correct name in
                        //nameArray to get the correct index of dataArray the data should be added to
                        String[] dataArray = generateDataArray(nameArray, c, rowcount);

                        //write ordered data to file
                        writeDataRow(dataArray, bw);

                        bw.newLine();
                    }
                }
                bw.flush();
                //Scan for the newly created file so it will show up in file explorer
                scanFile(this, saveFile, null);
                Toast.makeText(getApplicationContext(), "Exported Successfully", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "File Location: Documents folder",
                        Toast.LENGTH_SHORT).show();
                c.close();

            } catch (Exception ex) {
                ex.printStackTrace();
                if (sqldb.isOpen()) {
                    sqldb.close();
                    Toast.makeText(getApplicationContext(), ex.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Prints the contents of the database to the "contents" view
     */
    private void printDB(){
        String dbString = database.databaseToString(numDevices);
        if (dbString.equals("")){
            header.setText(DB_EMPTY_MSG);
            contents.setText("");
        }
        else{
            contents.setText(dbString);
        }
    }

    /**
     * This function causes the device to scan for a certain file/show that file in the directory
     */
    private void scanFile(Context ctxt, File f, String mimeType) {
        MediaScannerConnection
                .scanFile(ctxt, new String[] {f.getAbsolutePath()},
                        new String[] {mimeType}, null);
    }

    /**
     * Generates a unique filename based on the current DateTime
     */
    private String generateFilename() {
        String filename = "TemperatureData_";
        Date curDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yy_HH꞉mm꞉ss");
        String date = format.format(curDate);
        filename+=date;
        filename+=".csv";
        return filename;
    }

    /**
     * Writes the column headers of the .csv file, i.e. "Time" and the the names of the devices
     */
    private void writeColumnHeaders(Cursor c, BufferedWriter bw){
        try{
            /*If devicenames is null the activity was spawned from mainactivity
             and the array needs to be generated. A better way to day this may be
             to use SharedPreferences to store the device names
             */
            if (deviceNames == null) {
                deviceNames = new String[numDevices];
                for (int i = 0; i < numDevices; i++) {
                    String curr = c.getString(2);
                    deviceNames[i] = curr;
                    if (i != numDevices - 1) {
                        bw.write(removeUID(curr) + ",");
                    } else {
                        bw.write(removeUID(curr));
                    }
                    c.moveToNext();
                }
            }
            else {
                for (int i = 0; i < numDevices; i++) {

                    if (i != numDevices - 1) {
                        bw.write(removeUID(deviceNames[i]) + ",");
                    } else {
                        bw.write(removeUID(deviceNames[i]));
                    }
                }

            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Create dataArray, the array that for every set of readings contains the data of those
     * readings in the correct order
     * @param nameArray The array of names in the order of the current set of readings
     * @param rowcount The total number of rows in the database
     * @return The completed array
     */
    private String[] generateDataArray(String[] nameArray, Cursor c, int rowcount){
        String[] dataArray = new String[nameArray.length];
        for (int j = 0; j < numDevices; j++) {
            //check for duplicates. If a certain name already appeared in a set of
            //readings, the current reading should actually be the beginning of the
            //next row.
            boolean duplicate = false;
            for (int x = 0; x < j; x++) {
                if (nameArray[x] != null && nameArray[j] != null) {
                    if (nameArray[x].equals(nameArray[j])) {
                        duplicate = true;
                    }
                }
            }
            if (duplicate) {break;}
            String curr = c.getString(2);
            //store current cursor value in correct index of dataArray
            for (int x = 0; x < numDevices; x++) {
                if (deviceNames[x] != null) {
                    if (curr.equals(deviceNames[x])){
                        dataArray[x] = c.getString(3);
                        break;
                    }
                }
                else{
                    dataArray[x] = "";
                }
            }

            if(currRow < rowcount-1) {
                c.moveToNext();
                currRow++;
            }
            else{
                currRow++;
            }
        }
        return dataArray;
    }

    /**
     * Writes the completed dataArray to the .csv file
     */
    private void writeDataRow(String[] dataArray, BufferedWriter bw){
        try {
            for (int j = 0; j < numDevices; j++) {
                String curr;
                if (dataArray[j] != null) {
                    curr = dataArray[j];
                } else {
                    curr = "";
                }
                if (j != numDevices - 1) {
                    bw.write(curr + ",");
                } else {
                    bw.write(curr);
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     *
     * Removes the unique identifier from a device name, returning it to how it originally was
     */
    public static String removeUID(String str) {
        return str.substring(0, str.length() - 1);
    }
}
