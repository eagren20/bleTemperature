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

    DBHelper database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database_activity);

        //print db contents
        database = new DBHelper(this, null, null, 1);
        TextView contents = (TextView) findViewById(R.id.db_contents);
        TextView header = (TextView) findViewById(R.id.db_header);
        String dbString = database.databaseToString();
        if (dbString.equals("")){
            header.setText("Database is empty");
        }
        else{
            contents.setText(dbString);
        }
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
        }
        return super.onOptionsItemSelected(item);
    }
}
