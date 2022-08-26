package ru.cityprint.arm.warehouse.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import ru.cityprint.arm.warehouse.database.BaseHelper;
import ru.cityprint.arm.warehouse.network.WifiHelper;

/**
 * Работа с таблицей БД measure
 */
public class MeasureHelper {

    private String TAG = "MeasureHelper";
    private static MeasureHelper sMeasureHelper;
    private Context mContext;
    private static SQLiteDatabase mDatabase;


    /**
     * get
     *
     * @param context - Context
     * @return - return ItemHelper
     */

    public static MeasureHelper get(Context context) {

            sMeasureHelper = new MeasureHelper(context);
            return sMeasureHelper;
    }

    /**
     *
     * @param context - Context
     */
    private MeasureHelper(Context context) {
        mContext = context;
        mDatabase = BaseHelper.get(mContext).getWritableDatabase();
    }

    /**
     * Добавление
     */
    public void InsertMeasure(String name, String code){

        ContentValues measureValues = new ContentValues();
        measureValues.put("code", String.valueOf(code));
        measureValues.put("name", String.valueOf(name));
        mDatabase.insert("measure", null, measureValues);
    }




    /**
     * Получаем все объекты measure (коллекцию) из БД
     * @return - список(коллекцию) объектов measure
     */

    public List<String> getMeasures() {
        List<String> measure = new ArrayList<>();

        Cursor cursor = mDatabase.query(
                "measure",
                null, // Columns - null selects all columns
                null,
                null,
                null, // groupBy
                null, // having
                null  // orderBy
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Measure m = new Measure();

            String name = cursor.getString(1);
            String code = cursor.getString(2);

            m.setCode(code);
            m.setName(name);
            measure.add(name);

            cursor.moveToNext();
        }
        cursor.close();

        return measure;
    }


    /**
     * Очищает таблицу measure
     */
    public void clearTable(){
        //проверить вай фай соединение
        //если есть, то очистить таблиц
        //если нет то не очищать таблицу

        if (WifiHelper.isConnected(mContext)) {
            mDatabase.execSQL("delete from measure");
        }
        return;
    }

}
