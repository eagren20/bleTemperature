package eagren20.bletemperature;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by eagre on 7/10/2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "TemperatureData.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TEMPERATURE_TABLE= "temperature";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ONE = "sensor_one";
    public static final String COLUMN_TWO = "sensor_two";
    public static final String COLUMN_THREE = "sensor_three";

    /**
     * Create a helper object to create, open, and/or manage a database.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context to use to open or create the database
     * @param name    of the database file, or null for an in-memory database
     * @param factory to use for creating cursor objects, or null for the default
     * @param version number of the database (starting at 1); if the database is older,
     *                {@link #onUpgrade} will be used to upgrade the database; if the database is
     *                newer, {@link #onDowngrade} will be used to downgrade the database
     */
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
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
                        COLUMN_ONE + " DECIMAL(3, 1), " +
                        COLUMN_TWO + " DECIMAL(3, 1), " +
                        COLUMN_THREE + " DECIMAL(3, 1))"
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

    public void addDataRow(float one, float two, float three){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COLUMN_ONE, one);
        contentValues.put(COLUMN_TWO, two);
        contentValues.put(COLUMN_THREE, three);

        db.insert(TEMPERATURE_TABLE, null, contentValues);
    }

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, TEMPERATURE_TABLE);
        return numRows;
    }

    public Cursor getPerson(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("SELECT * FROM " + TEMPERATURE_TABLE + " WHERE " +
                COLUMN_ID + "=?", new String[]{Integer.toString(id)});
        return res;
    }

    /**
     * Remove all users and groups from database.
     */
    public void removeAll()
    {
        // db.delete(String tableName, String whereClause, String[] whereArgs);
        // If whereClause is null, it will delete all rows.
        SQLiteDatabase db = this.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
        db.delete(TEMPERATURE_TABLE, null, null);
    }
}
