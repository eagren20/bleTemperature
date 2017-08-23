package eagren20.bletemperature;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Author: Erik Agren
 * 7/10/2017
 * Extension of SQLiteOpenHelper to implement a custom database for temperature readings
 * One row for each individual reading
 */

class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "TemperatureData.db";
    private static final int DATABASE_VERSION = 2;
    //labels for table/column names
    static final String TEMPERATURE_TABLE= "temperature";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_DEVICE = "device";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_TEMPERATURE = "temperature";

    /**
     * Create a helper object to create, open, and/or manage a database.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context to use to open or create the database
     */

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TEMPERATURE_TABLE +
                        "(" + COLUMN_ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_TIME + " TEXT, " +
                        COLUMN_DEVICE + " TEXT, " +
                        COLUMN_TEMPERATURE + " DECIMAL(3, 1))"
        );
    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     * <p>
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TEMPERATURE_TABLE);
        onCreate(db);
    }

    /**
     * Adds a row of data to the DB table
     * @param device the device name
     * @param data the actual temperature data
     * @param time the timestamp
     */
    void addDataRow(String device, float data, String time){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COLUMN_TIME, time);
        contentValues.put(COLUMN_DEVICE, device);
        contentValues.put(COLUMN_TEMPERATURE, data);

        db.insert(TEMPERATURE_TABLE, null, contentValues);
    }


    /**
     * Remove all users and groups from database.
     */
    void removeAll()
    {
        // db.delete(String tableName, String whereClause, String[] whereArgs);
        // If whereClause is null, it will delete all rows.
        SQLiteDatabase db = this.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db.delete(TEMPERATURE_TABLE, null, null);
    }

    /**
     * @param numDevices The number of devices
     * @return A string representing the contents of the database
     */
    String databaseToString(int numDevices){
        String dbString = "";
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TEMPERATURE_TABLE+ " WHERE 1";// why not leave out the WHERE  clause?

        //Cursor points to a location in your results
        Cursor c = db.rawQuery(query, null);
        //Move to the first row in your results
        c.moveToFirst();

        int i = 0;
        //Position after the last row means the end of the results
        while (!c.isAfterLast()) {
            // null could happen if we used our empty constructor
            if (c.getString(c.getColumnIndex(COLUMN_DEVICE)) != null &&
                    c.getString(c.getColumnIndex(COLUMN_TEMPERATURE)) != null) {
                dbString += DatabaseActivity.removeUID(c.getString(c.getColumnIndex(COLUMN_DEVICE)));
                dbString += ": ";
                dbString += c.getString(c.getColumnIndex(COLUMN_TEMPERATURE));
                dbString += " - ";
                dbString += c.getString(c.getColumnIndex(COLUMN_TIME));
                dbString += "\n";
                i++;
                if (i == numDevices){
                    dbString += "-------------------------\n";
                    i = 0;
                }
            }
            c.moveToNext();
        }
        c.close();
        db.close();
        return dbString;
    }

    /**
     * Checks if the database is empty
     * @return True if empty, false if not empty
     */
    boolean isEmpty() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor mCursor = db.rawQuery("SELECT * FROM " + TEMPERATURE_TABLE, null);
        Boolean empty = (mCursor.getCount() == 0);
        mCursor.close();
        return empty;
    }

// Unused utilty methods

//    public int numberOfRows() {
//        SQLiteDatabase db = this.getReadableDatabase();
//        return (int) DatabaseUtils.queryNumEntries(db, TEMPERATURE_TABLE);
//    }
//
//    public Cursor getRow(int id) {
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor res =  db.rawQuery("SELECT * FROM " + TEMPERATURE_TABLE + " WHERE " +
//                COLUMN_ID + "=?", new String[]{Integer.toString(id)});
//        return res;
//    }

}

