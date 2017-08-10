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
            if (numDevices == -1){
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

//                if (rowcount > 0) {
//
//                    c.moveToFirst();
//                    for (int i = 0; i < colcount; i++) {
//
//                        if (i != colcount - 1) {
//                            bw.write(c.getColumnName(i) + ",");
//                        } else {
//                            bw.write(c.getColumnName(i));
//                        }
//                    }
//                    bw.newLine();
//                    for (int i = 0; i < rowcount; i++) {
//
//                        c.moveToPosition(i);
//
//                        for (int j = 0; j < colcount; j++) {
//
//                            if (j != colcount - 1) {
//                                bw.write(c.getString(j) + ",");
//                            } else {
//                                bw.write(c.getString(j));
//                            }
//                        }
//                        bw.newLine();
//                    }

                    if (rowcount > 0) {

                        c.moveToFirst();;
                        bw.write("Time,");
                        if (deviceNames == null){
                            for (int i = 0; i <numDevices; i++){
                                if (i != numDevices - 1) {
                                    bw.write(c.getString(2)+ ",");
                                } else {
                                    bw.write(c.getString(2));
                                }
                                c.moveToNext();
                            }
                        }
                        //TODO: for each set of readings put readings in array, then add
                        else {
                            for (int i = 0; i < numDevices; i++) {

                                if (i != numDevices - 1) {
                                    bw.write(deviceNames[i] + ",");
                                } else {
                                    bw.write(deviceNames[i]);
                                }
                            }

                        }
                        bw.newLine();
                        c.moveToPosition(0);
                        for (int i = 0; i < rowcount; i++) {

                            bw.write(c.getString(1)+",");

                            for (int j = 0; j < numDevices; j++){
                                String string = c.getString(3);
                                if (j != numDevices-1) {
                                    bw.write(c.getString(3) + ",");

                                } else {
                                    bw.write(c.getString(3));

                                }
                                i++;
                                c.moveToNext();
                                if (c.isAfterLast()){
                                    break;
                                }
                            }
                            bw.newLine();
                        }
                    bw.flush();
                    scanFile(this, saveFile, null);
                    Toast.makeText(getApplicationContext(), "Exported Successfully", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "File Location: Documents folder",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {

                if (sqldb.isOpen()) {
                    sqldb.close();
                    Toast.makeText(getApplicationContext(), ex.getMessage().toString(),
                            Toast.LENGTH_SHORT).show();
                }
            } finally {

            }
        }
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
}
