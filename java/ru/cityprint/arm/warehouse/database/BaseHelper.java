package ru.cityprint.arm.warehouse.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.cityprint.arm.warehouse.database.BarCodesDbSchema.BarCodesTable;
import ru.cityprint.arm.warehouse.database.ItemsDbSchema.*;
import ru.cityprint.arm.warehouse.database.ApiBarCodesDBSchema.*;


/**
 * Created by evgeny on 11.03.18.
 * Класс-помощник для работы с объектами БД (создание таблиц)
 */
public class BaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "warehouseBaseHelper";
    private static final int VERSION = 2;
    private static final String DATABASE_NAME = "warehouseBase.db";
    private static BaseHelper sBaseHelper;

    public static BaseHelper get(Context context) {
        if (sBaseHelper == null) {
            sBaseHelper = new BaseHelper(context);
        }
        return sBaseHelper;
    }

    public BaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // создается таблица Barcodes
        db.execSQL("create table " + BarCodesTable.TABLE_NAME + "(" +
                " _id integer primary key autoincrement, " +
                BarCodesTable.Cols.UUID + ", " +
                BarCodesTable.Cols.NAME + ", " +
                BarCodesTable.Cols.BARCODE + ", " +
                BarCodesTable.Cols.QUANTITY + ", " +
                BarCodesTable.Cols.CHECKED + ", " +
                BarCodesTable.Cols.MEASURE + "," +
                BarCodesTable.Cols.ITEM_ID +
                ")"
        );

        // создается таблица Items
        db.execSQL("create table " + ItemsTable.TABLE_NAME + "(" +
                " _id integer primary key autoincrement, " +
                ItemsTable.Cols.ID + ", " +
                ItemsTable.Cols.NAME + ", " +
                ItemsTable.Cols.PARENT_ID + ", " +
                ItemsTable.Cols.WIDTH + ", " +
                ItemsTable.Cols.HEIGHT + ", " +
                ItemsTable.Cols.LENGTH + ", " +
                ItemsTable.Cols.MEASURE + ", "+
                ItemsTable.Cols.IS_CATEGORY +
                ")"
        );

        // создается таблица ApiBarcodes
        db.execSQL("create table " + ApiBarCodesTable.TABLE_NAME + "(" +
                " _id integer primary key autoincrement, " +
                ApiBarCodesTable.Cols.BARCODE + ", " +
                ApiBarCodesTable.Cols.ITEM_ID +
                ")"
        );

        // создается таблица единиц измерения
        db.execSQL("create table measure ( _id integer primary key autoincrement, name TEXT, code TEXT)");

        // создается таблица меток
        db.execSQL("create table Stock ( _id integer primary key autoincrement, label TEXT, qty TEXT, parent_item TEXT)");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
