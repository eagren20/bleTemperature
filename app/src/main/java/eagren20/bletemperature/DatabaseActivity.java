package eagren20.bletemperature;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Created by eagre on 7/25/2017.
 */

public class DatabaseActivity extends AppCompatActivity {

    private DBHelper database;
    private int numDevices;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database_activity);

        database = new DBHelper(this);
        //print db contents
        Bundle bundle = this.getIntent().getExtras();
        numDevices = bundle.getInt(DataReadActivity.EXTRAS_DATABASE_STRING);
        printDB();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.data_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            //delete all database entries

            database.removeAll();
            printDB();
        }
        return super.onOptionsItemSelected(item);
    }

    private void printDB(){
        TextView contents = (TextView) findViewById(R.id.db_contents);
        TextView header = (TextView) findViewById(R.id.db_header);
        String dbString = database.databaseToString(numDevices);
        if (dbString.equals("")){
            header.setText("Database is empty");
            contents.setText("");
        }
        else{
            contents.setText(dbString);
        }
    }
}
