package ru.cityprint.arm.warehouse.data;

/**
 * Created by evgeny on 11.03.18.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.cityprint.arm.warehouse.database.BarCodesDbSchema.BarCodesTable;
import ru.cityprint.arm.warehouse.database.BarcodeCursorWrapper;
import ru.cityprint.arm.warehouse.database.BaseHelper;

/**
 *  Вспомогательный класс для работы с таблицей BarCodes
*/
public class BarCodeHelper {
    private static BarCodeHelper sBarCodeHelper;
    private Context mContext;
    private static SQLiteDatabase mDatabase;
    private static ItemHelper mItemHelper;


    /**
     * Singleton BarCodeHelper
     * @param context - Context
     * @return - return BarCodeHelper
     */
    public static BarCodeHelper get(Context context) {
            sBarCodeHelper = new BarCodeHelper(context);
        return sBarCodeHelper;
    }

    /**
     * Ctor
     * @param context - Context
     */
    private BarCodeHelper(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = BaseHelper.get(mContext).getWritableDatabase();
        mItemHelper = ItemHelper.get(mContext);
    }

    /**
     * Возвращает ContentValues для значений из таблицы BarCodes
     * @param barCode - Объект BarCode
     * @return - return ContentValues
     */
    private static ContentValues getContentValues(BarCode barCode) {
        ContentValues values = new ContentValues();
        values.put(BarCodesTable.Cols.UUID, String.valueOf(barCode.getId()));
        values.put(BarCodesTable.Cols.NAME, String.valueOf(barCode.getName()));
        values.put(BarCodesTable.Cols.BARCODE, String.valueOf(barCode.getBarcode()));
        values.put(BarCodesTable.Cols.QUANTITY, String.valueOf(barCode.getQuantity()));
        values.put(BarCodesTable.Cols.MEASURE, String.valueOf(barCode.getMeasure()));
        values.put(BarCodesTable.Cols.CHECKED, String.valueOf(barCode.getChecked() ? "1" : "0"));
        values.put(BarCodesTable.Cols.ITEM_ID, String.valueOf(barCode.getItemId()));
        return values;
    }

    /**
     * Добавление объекта BarCode в базу
     * @param barCode - объект BarCode
     */
    public void addBarcode(BarCode barCode) {
        ContentValues values = getContentValues(barCode);
        mDatabase.insert(BarCodesTable.TABLE_NAME, null, values);
    }

    public void addBarcodeRetUUID(BarCode barCode) {
        ContentValues values = getContentValues(barCode);
        mDatabase.insert(BarCodesTable.TABLE_NAME, null, values);
    }


    /**
     * Удаление объекта BarCode из базы
     * @param barCode - объект BarCode
     */
    public void deleteBarCode(BarCode barCode) {
        mDatabase.delete(BarCodesTable.TABLE_NAME, BarCodesTable.Cols.UUID + " = ?", new String[]{ String .valueOf(barCode.getId())});
    }

    /**
     * Обновление объекта BarCode
     * @param barCode - объект BarCode
     */
    public void updateBarCode(BarCode barCode) {
        ContentValues values = getContentValues(barCode);
        mDatabase.update(BarCodesTable.TABLE_NAME, values,BarCodesTable.Cols.UUID + " = ?", new String[] { String.valueOf(barCode.getId())});
    }

    /**
     * Запрос на выборку
     * @param whereClause - условный оператор WHERE
     * @param whereArgs - значения для условного оператора WHERE
     * @return - объект  BarcodeCursorWrapper
     */
    private BarcodeCursorWrapper queryBarCode(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                BarCodesTable.TABLE_NAME,
                null, // Columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                null  // orderBy
        );

        return new BarcodeCursorWrapper(cursor);
    }

    /**
     * Получаем все объекты BarCode (коллекцию) из БД
     * @return - список(коллекцию) объектов BarCode
     */
    public List<BarCode> getBarCodes() {
        List<BarCode> barcodes = new ArrayList<>();

        BarcodeCursorWrapper cursor = queryBarCode(null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            barcodes.add(cursor.getBarCode());
            cursor.moveToNext();
        }
        cursor.close();

        return barcodes;
    }

    /**
     * Удаляем выделенные объекты
     */
    public void deleteSelectedBarCodes() {
        //выбираем выделенные объекты
        BarcodeCursorWrapper cursor = queryBarCode(BarCodesTable.Cols.CHECKED + " = ?",  new String[] {"1"});
        cursor.moveToFirst();
        String id="";

        while (!cursor.isAfterLast()) {

            id=cursor.getBarCode().getId().toString();
            mDatabase.delete(BarCodesTable.TABLE_NAME, BarCodesTable.Cols.UUID + " = ?",  new String[] {id});
            cursor.moveToNext();
        }
        cursor.close();
    }


    /**
     * Достать объект по UUID
     * @param id - UUID - идентификатор объекта в SQLite
     * @return - объект BarCode
     */
    public BarCode getBarCode(UUID id) {
        BarcodeCursorWrapper cursor = queryBarCode(
                BarCodesTable.Cols.UUID + " = ?",
                new String[] { id.toString() }
        );

        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            return cursor.getBarCode();
        } finally {
            cursor.close();
        }
    }

    /**
     * Очищает таблицу BarCodes
     */
    public void clearTable(){
        mDatabase.execSQL("delete from "+ BarCodesTable.TABLE_NAME);
    }

    /**
     * Возвращает ItemId, связанный со ШК (barcode)
     * @param barcode - ШК
     * @return - String
     */
    public String getApiBarcodeItemId(String barcode) {
        // получаем ItemId, связанный с barcode
        return mItemHelper.getApiBarcodesItemId(barcode);
    }

    /**
     * Создаем из Item объект BarCode
     * @param item - объект Item
     * @param barcode - ШК
     * @return - объект BarCode
     */
    public BarCode createBarcodeFromItem(Item item, String barcode, String barcodeId, float Qty){
            BarCode barCode = new BarCode();
            //конструктор перегружен, новые с новым Id, существующие с родным Id
        if (barcodeId==null || barcodeId.equals("")) {
            //новый баркод
            barCode.setId(UUID.randomUUID()); //fromString(item.getId()));
        }else {
            // у баркода есть ID
            barCode.setId(UUID.fromString(barcodeId));
        }
            barCode.setName(item.getName());
            barCode.setBarcode(barcode);
            barCode.setMeasure(item.getMeasure());
            barCode.setQuantity(Qty);
            barCode.setChecked(false);
            barCode.setItemId(item.getId());
            //добавляем в базу
            addBarcode(barCode);

            return barCode;
    }

    /**
     * Создаем из метки объект BarCode для добавления метки в таблицу Barcodes
     * @param item - объект Item
     * @param barcode - ШК
     * @return - объект BarCode
     */
    public BarCode createBarcodeFromLabel(Item item, String barcode){
        BarCode barCode = new BarCode();
        //конструктор перегружен, новые с новым Id, существующие с родным Id
        barCode.setId(UUID.randomUUID()); //fromString(item.getId()));
        barCode.setName(item.getName());
        barCode.setBarcode(barcode);
        barCode.setMeasure(item.getMeasure());
        barCode.setQuantity(1);
        barCode.setChecked(false);
        barCode.setItemId(item.getId());
        //добавляем в базу
        addBarcode(barCode);

        return barCode;
    }

    /**
     * проверить, существует ли в таблице BarCode с itemId=itemId (добавляемый)
     */
    public BarCode getBarCodeByItem(Item item){
        BarcodeCursorWrapper cursor = queryBarCode(BarCodesTable.Cols.ITEM_ID + "=?", new String[]{item.getId()});

        if (cursor.moveToFirst()){
            //запись с таким Id есть в таблице barcodes
            BarCode barCode = cursor.getBarCode();
            return barCode;
        }
        cursor.close();
        return null;
    }

    /**
     * Получить количество barcodes quantity по itemId
     */
    public float getBarCodesQuantity(Item item){
        BarcodeCursorWrapper cursor = queryBarCode(BarCodesTable.Cols.ITEM_ID + "=?", new String[]{item.getId()});

        if (cursor.moveToFirst()){
            //запись с таким Id есть в таблице barcodes
            BarCode barCode = cursor.getBarCode();
            return barCode.getQuantity();
        }
        cursor.close();
        return 0;
    }

}
