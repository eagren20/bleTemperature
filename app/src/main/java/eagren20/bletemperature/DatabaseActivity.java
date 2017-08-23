package eagren20.bletemperature;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by eagre on 7/25/2017.
 */

public class DatabaseActivity extends AppCompatActivity {

    private DBHelper database;
    private int numDevices;
    private TextView contents;
    private TextView header;
    private String[] deviceNames;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database_activity);

        contents = (TextView) findViewById(R.id.db_contents);
        header = (TextView) findViewById(R.id.db_header);

        database = new DBHelper(this);
        //print db contents
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
            header.setText("Number of devices not found: Please read again. " +
                    "Possible causes include clearing the cache");
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
            if (contents.getText().equals("")) {
                Toast.makeText(getApplicationContext(), "The database is empty", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            if (numDevices == -1) {
                Toast.makeText(getApplicationContext(), "Number of devices not found", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }
            SQLiteDatabase sqldb = database.getReadableDatabase();
            Cursor c;
            try {

                c = sqldb.rawQuery("select * from " + DBHelper.TEMPERATURE_TABLE, null);

                int rowcount = 0;
                int colcount = 0;

                String mDir = Environment.DIRECTORY_DOCUMENTS;
                File sdCardDir = Environment.getExternalStoragePublicDirectory(mDir);
                String filename = generateFilename();

                // the name of the file to export with
                File saveFile = new File(sdCardDir, filename);
                FileWriter fw = new FileWriter(saveFile);


                BufferedWriter bw = new BufferedWriter(fw);
                rowcount = c.getCount();
                colcount = c.getColumnCount();

                if (rowcount > 0) {

                    c.moveToFirst();

                    bw.write("Time,");
                    /**If devicenames is null the activity was spawned from mainactivity
                    *and the array needs to be generated. A better way to day this may be
                    *to use SharedPreferences to store the device names
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
                    //TODO: for each set of readings put readings in array, then add
                    else {
                        for (int i = 0; i < numDevices; i++) {

                            if (i != numDevices - 1) {
                                bw.write(removeUID(deviceNames[i]) + ",");
                            } else {
                                bw.write(removeUID(deviceNames[i]));
                            }
                        }

                    }
                    bw.newLine();
                    c.moveToPosition(0);
                    int i = 0;
                    while (i < rowcount) {

                        bw.write(c.getString(1) + ",");
                        String[] dataArray = new String[numDevices];

                        String[] nameArray = new String[numDevices];
                        int temp = i;
                        for (int j = 0; j < numDevices; j++) {
                            //get current set of readings and names
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
                        c.moveToPosition(i);
                        for (int j = 0; j < numDevices; j++) {
                            //check for duplicates
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

                            if(i < rowcount-1) {
                                c.moveToNext();
                                i++;
                            }
                            else{
                                i++;
                            }
                        }
                        //write data to file
                        for (int j = 0; j < numDevices; j++){
                            String curr;
                            if (dataArray[j] != null){
                                curr = dataArray[j];
                            }
                            else{
                                curr = "";
                            }
                            if (j != numDevices-1) {
                                bw.write(curr +",");
                            }
                            else{
                                bw.write(curr);
                            }
                        }
                        bw.newLine();

                    }

                    //**************************

//                            for (int j = 0; j < numDevices; j++){
//                                String string = c.getString(3);
//                                if (j != numDevices-1) {
//                                    bw.write(c.getString(3) + ",");
//
//                                } else {
//                                    bw.write(c.getString(3));
//
//                                }
//                                i++;
//                                c.moveToNext();
//                                if (c.isAfterLast()){
//                                    break;
//                                }
//                            }
//                            bw.newLine();
                }
                bw.flush();
                scanFile(this, saveFile, null);
                Toast.makeText(getApplicationContext(), "Exported Successfully", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "File Location: Documents folder",
                        Toast.LENGTH_SHORT).show();


            } catch (Exception ex) {
                ex.printStackTrace();
                if (sqldb.isOpen()) {
                    sqldb.close();
                    Toast.makeText(getApplicationContext(), ex.getMessage().toString(),
                            Toast.LENGTH_SHORT).show();
                }
            } finally {

            }
        }
//        else if ()
        return super.onOptionsItemSelected(item);
    }

    private void printDB(){
        String dbString = database.databaseToString(numDevices);
        if (dbString.equals("")){
            header.setText("Database is empty");
            contents.setText("");
        }
        else{
            contents.setText(dbString);
        }
    }

    private void scanFile(Context ctxt, File f, String mimeType) {
        MediaScannerConnection
                .scanFile(ctxt, new String[] {f.getAbsolutePath()},
                        new String[] {mimeType}, null);
    }

    private String generateFilename() {
        String filename = "TemperatureData_";
        Date curDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yy_HH꞉mm꞉ss");
        String date = format.format(curDate);
        filename+=date;
        filename+=".csv";
        return filename;
    }

    public static String removeUID(String str) {
        return str.substring(0, str.length() - 1);
    }
}
