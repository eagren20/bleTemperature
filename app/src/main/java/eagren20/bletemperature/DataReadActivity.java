package eagren20.bletemperature;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;

public class DataReadActivity extends AppCompatActivity {

    private ListView list;
    private ReadAdapter adapter;
    private ArrayList<String> addresses;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read_activity);

        addresses = new ArrayList<>();


        list = (ListView) findViewById(R.id.read_list);
        adapter = new ReadAdapter(this, R.layout.read_row, addresses);
        list.setAdapter(adapter);


        displayData();
    }

    private void displayData() {

        Bundle bundle = this.getIntent().getExtras();
        addresses = bundle.getStringArrayList(MainActivity.EXTRAS_CHECKED_ADDRESSES);
        adapter.notifyDataSetChanged();
    }


}
