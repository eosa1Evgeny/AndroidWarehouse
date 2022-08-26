package ru.cityprint.arm.warehouse.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.cityprint.arm.warehouse.barcode.CoreApplication;
import ru.cityprint.arm.warehouse.database.BarCodesDbSchema;
import ru.cityprint.arm.warehouse.database.BaseHelper;
import ru.cityprint.arm.warehouse.database.ItemCursorWrapper;
import ru.cityprint.arm.warehouse.network.WifiHelper;

import ru.cityprint.arm.warehouse.database.ItemsDbSchema.*;
import ru.cityprint.arm.warehouse.database.ApiBarCodesDBSchema.*;
import ru.cityprint.arm.warehouse.ui.ModulesActivity;


/**
 * Вспомогательный класс для работы с таблицей Items (ApiBarcodes)
 */
public class ItemHelper {
    private List<Item> mCategories;
    private String TAG = "ItemHelper";
    private static ItemHelper sItemHelper;
    private Context mContext;
    private SQLiteDatabase mDatabase;
    private String mBarcodeID;
    private List<Item> mAllItems;
    private static BarCodeHelper mBarCodeHelper;
    private AcceptanceListener mAcceptanceListener;
    private ManufactureListener mManufactureListener;
    private InventoryListener mInventoryListener;
    private String mBarCodeR;
    private String mParentIdR;
    private NomenclatureListener mNomenclatureListener;
    private String mParentID;
    private String mLabel;
    private String mQty;
    private String mParentItem;
    private OnDataLoadListener mDataLoadListener;

    /**
     * ItemHelper singleton
     *
     * @param context - Context
     * @return - return ItemHelper
     */
    public static ItemHelper get(Context context) {
        sItemHelper = new ItemHelper(context);
        return sItemHelper;
    }

    /**
     * @param context - Context
     */
    private ItemHelper(Context context) {
        mContext = context;
        mDatabase = BaseHelper.get(mContext).getWritableDatabase();

        if (mContext instanceof AcceptanceListener) {
            mAcceptanceListener = (AcceptanceListener) mContext;
        }

        if (mContext instanceof NomenclatureListener) {
            mNomenclatureListener = (NomenclatureListener) mContext;
        }

        if (mContext instanceof ManufactureListener) {
            mManufactureListener = (ManufactureListener) mContext;
        }

        if (mContext instanceof InventoryListener) {
            mInventoryListener = (InventoryListener) mContext;
        }

        if (mContext instanceof ModulesActivity) {
            mDataLoadListener = (OnDataLoadListener) mContext;
        }
    }


    /**
     * Загружает все объекты из Web Service API (JSON) в БД
     */
    public int loadAll() {

        //проверить вай фай соединение
        //если есть, то очистить таблиц
        //если нет то не очищать таблицу
        //TODO закомментить, если нет WiFi
        if (isWifiConnected()) {
            clearTables();
        } else {
            return 0;
        }

        // стартуем загрузку данных
        if (mDataLoadListener != null) {
            mDataLoadListener.onStartingDataLoad();
        }

        // вызываем метод API
        CoreApplication.getApi().getCategories().enqueue(new Callback<NomenclatureResponse>() {
            @Override
            public void onResponse(Call<NomenclatureResponse> call, Response<NomenclatureResponse> response) {
                NomenclatureResponse nomenclatureResponse = response.body();

                // список всех Items (категории и товары) - верхний уровень
                List<Item> itemList = nomenclatureResponse.getData();

                Item removeItem = new Item();
                //ищем корневую
                for (Item item : itemList) {
                    String parentId = item.getParentId();
                    if (parentId == null) {
                        mParentID = item.getId();
                        removeItem = item;
                    }
                }

                //удаляем корневую
                itemList.remove(removeItem);
                //выгружаем категории
                addToDbItem(itemList);
            }

            @Override
            public void onFailure(Call<NomenclatureResponse> call, Throwable t) {
                String error = t.getMessage();
                // завершаем загрузку данных с сервера API
                // message - текст ошибки
                if (mDataLoadListener != null) {
                    mDataLoadListener.onCompleteDataLoad(error);
                }
                Log.d(TAG, error);
            }
        });


        // вызываем метод API
        CoreApplication.getApi().getItems().enqueue(new Callback<NomenclatureResponse>() {
            @Override
            public void onResponse(Call<NomenclatureResponse> call, Response<NomenclatureResponse> response) {
                NomenclatureResponse nomenclatureResponse = response.body();

                // список товаров
                List<Item> itemList = nomenclatureResponse.getData();
                // выгружаем товары
                addToDbItem(itemList);

                //   окончание загрузки
                if (mDataLoadListener != null) {
                    mDataLoadListener.onCompleteDataLoad("Обновлено");
                }

            }

            @Override
            public void onFailure(Call<NomenclatureResponse> call, Throwable t) {
                String error = t.getMessage();
                // завершаем загрузку данных с сервера API
                // message - текст ошибки
                if (mDataLoadListener != null) {
                    mDataLoadListener.onCompleteDataLoad(error);
                }
                Log.d(TAG, error);
            }
        });



        return 1;
    }


    /**
     * перебор и запись в базу данных объектов
     *
     * @param items - коллекиця объектов Item
     */
    private void addToDbItem(List<Item> items) {
        if (items != null && items.size() > 0) {

            ContentValues values;
            //начало транзакции
            mDatabase.beginTransaction();

            try {

                for (Item itemR : items) {

                    //для детей корневого считаем, что они теперь  корневые
                    if (String.valueOf(itemR.getParentId()).equals(mParentID)) {
                        itemR.setParentId("null");
                    }

                    // добавляем Item в БД
                    values = getContentValuesItem(itemR);
                    mDatabase.insert(ItemsTable.TABLE_NAME, null, values);

                    List<Object> tmpBarCodes = itemR.getBarCodes();

                    //связанные штрих коды в отдельную таблицу, так как их может быть несколько
                    if (tmpBarCodes != null && tmpBarCodes.size() > 0) {
                        for (Object barCode : tmpBarCodes) {
                            mBarCodeR = String.valueOf(barCode);
                            mParentIdR = itemR.getId();
                            values = getContentValuesApiBarCode(mBarCodeR, mParentIdR);
                            mDatabase.insert(ApiBarCodesTable.TABLE_NAME, null, values);
                        }
                    }

                    List<StockInfoLabel> stocksInfo = itemR.getStockInfo();
                    // Метки
                    if (stocksInfo != null && stocksInfo.size() > 0) {
                        for (StockInfoLabel fLabel : stocksInfo) {
                            mLabel = String.valueOf(fLabel.getLabel());
                            mQty = String.valueOf(fLabel.getQty());
                            mParentItem = String.valueOf(itemR.getId());
                            values = getContentValuesStock(mLabel, mQty, mParentItem);
                            mDatabase.insert("Stock", null, values);
                        }
                    }

                }//for

                mDatabase.setTransactionSuccessful();

            } catch (Exception e) {
                e.printStackTrace();
            }

            //завершение транзакции
            mDatabase.endTransaction();
        }
    }


    /**
     * Передаем объекты из БД в Web Service API (JSON)
     */
    public void sendQuantity() {

        mBarCodeHelper = BarCodeHelper.get(mContext);

        if (!isWifiConnected()) {
            return;
        }

        //создаем массив Acceptance[]
        List<BarCode> tmpBarcode = mBarCodeHelper.getBarCodes();
        Acceptance acceptance = new Acceptance();
        List<AcceptanceLot> lots = new ArrayList<>();

        for (BarCode bc : tmpBarcode) {
            //создаем класс Acceptance
            AcceptanceLot lot = new AcceptanceLot();
            lot.setId(String.valueOf(bc.getItemId()));
            lot.setQty(bc.getQuantity());
            lots.add(lot);

        }

        acceptance.setAcceptaneeLots(lots);
        sendItemToServer(acceptance);

    }


    /**
     * Отправка списка отсканированных товаров (количество) на сервер
     *
     * @param acceptance - объект параметр метода API ("/api/Acceptance")
     */
    public void sendItemToServer(Acceptance acceptance) {

        CoreApplication.getApi().sendQuantity(acceptance).enqueue(new Callback<ApiResponse>() {

            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {

                if (response.isSuccessful()) {
                    //ок
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceResponse(response.body());
                    }

                } else {
                    // ошибка
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceResponse(response.body());
                    }
                }

            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                String error = t.getMessage();
                Log.e(TAG, error);

                try {
                    ApiResponse mResult = new ApiResponse();
                    mResult.code = Integer.valueOf(call.execute().code());
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceResponse(mResult);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

            }
        });
    }

    /**
     * Передаем объекты из БД в Web Service API (JSON) - Передача в производство
     */
    public void sendQuantityAndBarcodesToProduction() {
        mBarCodeHelper = BarCodeHelper.get(mContext);

        if (!isWifiConnected()) {
            return;
        }

        //создаем массив Manufacture[]
        List<BarCode> tmpBarcode = mBarCodeHelper.getBarCodes();
        Manufacture manufacture = new Manufacture();
        List<ManufactureLot> lots = new ArrayList<>();

        for (BarCode bc : tmpBarcode) {
            //создаем класс Manufacture
            ManufactureLot lot = new ManufactureLot();
            lot.setId(String.valueOf(bc.getItemId()));
            lot.setQty((int) bc.getQuantity());
            lot.setCode(bc.getBarcode());
            lots.add(lot);
        }

        manufacture.setManufactureLots(lots);
        sendItemToProduction(manufacture);

    }

    /**
     * Отправка списка отсканированных товаров (количество и шК) в производство
     *
     * @param manufacture - объект параметр метода API ("/api/Manufacture")
     */
    public void sendItemToProduction(Manufacture manufacture) {
        CoreApplication.getApi().sendQuantityToProduction(manufacture).enqueue(new Callback<ManufactureResponse>() {
            @Override
            public void onResponse(Call<ManufactureResponse> call, Response<ManufactureResponse> response) {
                if (mManufactureListener != null) {
                    mManufactureListener.onManufactureResponse(response.body());
                }
            }

            @Override
            public void onFailure(Call<ManufactureResponse> call, Throwable t) {
                String error = t.getMessage();
                Log.e(TAG, error);

                try {
                    ManufactureResponse mResult = new ManufactureResponse();
                    mResult.code = call.execute().code();
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;
                    if (mManufactureListener != null) {
                        mManufactureListener.onManufactureResponse(mResult);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }

    /**
     * Передаем объекты из БД в Web Service API (JSON) - Инвентаризация
     */
    public void sendBarcodesToInventory(List<BarCode> barcodes) {
        mBarCodeHelper = BarCodeHelper.get(mContext);

        if (!isWifiConnected()) {
            return;
        }

        //создаем массив InventoryLot[]
        Inventory inventory = new Inventory();
        List<InventoryLot> lots = new ArrayList<>();

        for (BarCode bc : barcodes) {
            //создаем класс Manufacture
            InventoryLot lot = new InventoryLot();

            lot.setId(String.valueOf(bc.getItemId()));
            lot.setCode(bc.getBarcode());
            lots.add(lot);
        }

        inventory.setInventoryLots(lots);
        sendItemToInventory(inventory);

    }

    /**
     * Отправка списка отсканированных товаров (количество и ШК) на инвентаризацию
     *
     * @param inventory - объект параметр метода API ("/api/Inventarization")
     */
    public void sendItemToInventory(Inventory inventory) {
        CoreApplication.getApi().sendToInventory(inventory).enqueue(new Callback<InventoryResponse>() {
            @Override
            public void onResponse(Call<InventoryResponse> call, Response<InventoryResponse> response) {
                if (mInventoryListener != null) {
                    mInventoryListener.onInventoryResponse(response.body());
                }
            }

            @Override
            public void onFailure(Call<InventoryResponse> call, Throwable t) {
                String error = t.getMessage();
                Log.e(TAG, error);

                try {
                    InventoryResponse mResult = new InventoryResponse();
                    mResult.code = String.valueOf(call.execute().code());
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;
                    if (mInventoryListener != null) {
                        mInventoryListener.onInventoryResponse(mResult);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }

    /**
     * Отправка ШК для выбранного товара
     *
     * @param itemId       - id товара, параметр метода API ("/api/Nomenclature/{id}")
     * @param barcodeValue - значение ШК, тело (Body) метода API ("/api/Nomenclature/{id}")
     */
    public void sendBarcodeToServer(String itemId, String barcodeValue) {

        CoreApplication.getApi().addNewBarcode(itemId, barcodeValue).enqueue(new Callback<ItemResponse>() {

            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {
                clearTables();
                loadAll();

                if (mNomenclatureListener != null) {
                    mNomenclatureListener.onNomenclatureResponse(response.body());
                }

            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {
                String error = t.getMessage();
                Log.e(TAG, error);

                try {
                    ItemResponse mResult = new ItemResponse();
                    mResult.code = String.valueOf(call.execute().code());
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;
                    if (mNomenclatureListener != null) {
                        mNomenclatureListener.onNomenclatureResponse(mResult);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }


    /**
     * Возвращает объект Item по Id
     *
     * @param itemId - идентификатор Id в таблице Items
     * @return - String
     */
    public Item getItemById(String itemId) {
        ItemCursorWrapper cursor = queryItems(ItemsTable.Cols.ID + " = ?", new String[]{String.valueOf(itemId)});

        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            Item item = cursor.getItem();
            return item;

        } finally {
            cursor.close();
        }
    }

    /**
     * Возвращает объект Item по ШК (barcode)
     *
     * @param barcode - ШК в виде String
     * @return - Item
     */
    public Item getItemByBarcode(String barcode) {
        // получаем ItemId по связанному с ним barcode
        String itemId = getApiBarcodesItemId(barcode);

        if (itemId != null && !itemId.equals("")) {
            // получаем номенклатуру по ItemId
            Item item = getItemById(itemId);
            return item;
        }
        return null;
    }

    /**
     * Возвращает Id объекта Item по ШК
     *
     * @param barCode - значение ШК
     * @return - ItemId
     */
    public String getApiBarcodesItemId(String barCode) {
        Cursor cursor = queryApiBarCode(ApiBarCodesTable.Cols.BARCODE + " = ?", new String[]{String.valueOf(barCode)});
        try {
            if (cursor.getCount() == 0) {
                return "";
            }

            cursor.moveToFirst();
            String itemId = cursor.getString(2);
            return itemId;

        } finally {
            cursor.close();
        }
    }

    /**
     * Проверяем, есть ли такой ШК в таблице
     */
    public boolean isApiBarcodes(String barCode) {
        Cursor cursor = queryApiBarCode(ApiBarCodesTable.Cols.BARCODE + " = ?", new String[]{String.valueOf(barCode)});
        try {
            if (cursor.getCount() == 0) {
                return false;
            }

            if (cursor.moveToFirst()) {
                return true;
            }

            return false;

        } finally {
            cursor.close();
        }
    }


    /**
     * Возвращает ContentValues из таблицы Items для объекта Item
     *
     * @param item - объект Item
     * @return - ContentValues
     */
    private static ContentValues getContentValuesItem(Item item) {
        ContentValues values = new ContentValues();
        values.put(ItemsTable.Cols.ID, String.valueOf(item.getId()));
        values.put(ItemsTable.Cols.NAME, String.valueOf(item.getName()));
        values.put(ItemsTable.Cols.PARENT_ID, String.valueOf(item.getParentId()));
        values.put(ItemsTable.Cols.WIDTH, String.valueOf(item.getWidth()));
        values.put(ItemsTable.Cols.HEIGHT, String.valueOf(item.getHeight()));
        values.put(ItemsTable.Cols.LENGTH, String.valueOf(item.getLength()));
        values.put(ItemsTable.Cols.MEASURE, String.valueOf(item.getMeasure()));
        values.put(ItemsTable.Cols.IS_CATEGORY, item.isCategory() ? "1" : "0");
        return values;
    }

    /**
     * Возвращает ContentValues из таблицы ApiBarcodes для ШК по определенному ParentId
     *
     * @param barcode  - значение ШК
     * @param parentid - ParentId
     * @return - ContentValues
     */
    private static ContentValues getContentValuesApiBarCode(String barcode, String parentid) {
        ContentValues values = new ContentValues();
        values.put(ApiBarCodesTable.Cols.BARCODE, String.valueOf(barcode));
        values.put(ApiBarCodesTable.Cols.ITEM_ID, String.valueOf(parentid));
        return values;
    }

    /**
     * Возвращает ContentValues из таблицы label для меток
     *
     * @return - ContentValues
     */
    private static ContentValues getContentValuesStock(String label, String qty, String parent_item) {
        ContentValues values = new ContentValues();
        values.put("label", String.valueOf(label));
        values.put("qty", String.valueOf(qty));
        values.put("parent_item", String.valueOf(parent_item));
        return values;
    }


    private Boolean isWifiConnected() {
        return WifiHelper.isConnected(mContext);
    }


    /**
     * Очищает таблицы Items и ApiBarcodes
     */
    public void clearTables() {
        mDatabase.execSQL("delete from " + ItemsTable.TABLE_NAME);
        mDatabase.execSQL("delete from " + ApiBarCodesTable.TABLE_NAME);
        mDatabase.execSQL("delete from " + "Stock");
        mDatabase.execSQL("delete from " + "measure");
    }

    /**
     * Создание объектов Item из базы данных
     */
    private ItemCursorWrapper queryItems(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                ItemsTable.TABLE_NAME,
                null, // Columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                null  // orderBy
        );

        return new ItemCursorWrapper(cursor);
    }

    /**
     * Создание объектов ApiBarCode из базы данных
     */
    private Cursor queryApiBarCode(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                ApiBarCodesTable.TABLE_NAME,
                null, // Columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                null  // orderBy
        );

        return cursor;
    }

    /**
     * Получаем количество по метке
     */

    public double getQtyFromLabel(String label) {

        Cursor cursor = mDatabase.query(
                "Stock",
                null, // Columns - null selects all columns
                "label" + " = ?",
                new String[]{label},
                null, // groupBy
                null, // having
                null  // orderBy
        );

        try {
            if (cursor.getCount() == 0) {
                return 0;
            }

            cursor.moveToFirst();
            String qty = cursor.getString(2);
            return Double.valueOf(qty);

        } finally {
            cursor.close();
        }

    }


    /**
     * Возвращает всю коллекицю(список) объектов Item из БД
     *
     * @return - List<Item>
     */
    public List<Item> getAllItemFromDB() {
        mAllItems = new ArrayList<>();
        mCategories = new ArrayList<>();

        //выбираем все записи из БД
        ItemCursorWrapper cursor = queryItems("", null);
        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {

                mAllItems.add(cursor.getItem());
                cursor.moveToNext();
            }

        } finally {
            cursor.close();
        }
        return mAllItems;
    }

    /**
     * выбираем всю номенклатуру, без категорий, для поиска
     */
    public List<Item> getAllItemFromDBnotCategory() {

        mAllItems = new ArrayList<>();


        //выбираем все записи из БД
        ItemCursorWrapper cursor = queryItems(ItemsTable.Cols.IS_CATEGORY + " = ?", new String[]{"0"});
        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {

                mAllItems.add(cursor.getItem());
                cursor.moveToNext();
            }

        } finally {
            cursor.close();
        }
        return mAllItems;

    }

    /**
     * Полный поиск либо по ШК, либо наименованию
     */
    public List<Item> findFullWord(String wordSearch) {

        mAllItems = new ArrayList<>();

        // поиск может осуществляться как по наименованию номенклатуры,
        // так и по штрих-коду
        // 1. определяем товар (Item) по ШК
        Item barcodeItem = getItemByBarcode(wordSearch);

        // 2. если по ШК найден товар - добавляем в коллекцию и возвращаем
        if (barcodeItem != null) {
            mAllItems.add(barcodeItem);
        } else {
            // если по ШК товар не найден, ищем по наименованию
            //выбираем все записи из БД
            ItemCursorWrapper cursor = queryItems(ItemsTable.Cols.NAME + " like ?  and  " + ItemsTable.Cols.IS_CATEGORY + " = ?", new String[]{"%" + wordSearch + "%", "0"});
            try {
                if (cursor.getCount() == 0) {
                    return null;
                }

                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    mAllItems.add(cursor.getItem());
                    cursor.moveToNext();
                }

            } finally {
                cursor.close();
            }
        }

        return mAllItems;
    }


    /**
     * Создаем "полный" Item с его объектами Barcodes
     *
     * @param itemId строковой UUID
     * @return
     */

    public Item createItemAndBarcodes(String itemId) {

        Item item = getItemById(itemId);
        item.setBarCodes(getListBarcodes(item));
        return item;
    }

    /**
     * Получаем список строковых баркодов этого Item
     *
     * @param item
     * @return
     */
    List<Object> getListBarcodes(Item item) {

        ArrayList<Object> barCodes = new ArrayList<>();

        String barcodeTmp;
        BarCode barcode;

        Cursor cursor = queryApiBarCode(ApiBarCodesTable.Cols.ITEM_ID + " = ?", new String[]{String.valueOf(item.getId())});
        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                barcodeTmp = cursor.getString(1);
                //создать объект barcodeTmp
                barcode = createBarcode(item, barcodeTmp);
                barCodes.add((barcode));
                cursor.moveToNext();
            }

            return barCodes;

        } finally {
            cursor.close();
        }

    }


    /**
     * Создаем объект BarCode
     *
     * @param item    - объект Item
     * @param barcode - ШК
     * @return - объект BarCode
     */
    public BarCode createBarcode(Item item, String barcode) {
        BarCode barCode = new BarCode();
        //конструктор перегружен, новые с новым Id, существующие с родным Id
        barCode.setId(UUID.randomUUID()); //fromString(item.getId()));
        barCode.setName(item.getName());
        barCode.setBarcode(barcode);
        barCode.setMeasure(item.getMeasure());
        barCode.setQuantity(0);
        barCode.setChecked(false);
        barCode.setItemId(item.getId());
        return barCode;
    }


    /**
     * Возвращает коллекцию(список) объектов Item по ParentId
     *
     * @param items    - коллекция всех объектов Item
     * @param parentId - ParentId
     * @return - коллекция List<Item>
     */
    public List<Item> getItemsByParentId(List<Item> items, String parentId) {
        return recursiveAddToList(items, parentId);
    }

    /**
     * Рекурсивное добавление в коллекцию объектов Item, отфильтрованных по ParentId
     *
     * @param items    - коллекция всех объектов Item
     * @param parentId - ParentId
     * @return - коллекция List<Item>
     */
    private List<Item> recursiveAddToList(List<Item> items, String parentId) {
        List<Item> result = new ArrayList<>();

        try {
            List<Item> filteredItems = new ArrayList<>();
            for (Item filteredItem : items) {
                if (filteredItem.getParentId().equals(parentId)) {
                    filteredItems.add(filteredItem);
                }
            }

            for (Item item : filteredItems) {
                Item newItem = new Item();
                newItem.setId(item.getId());
                newItem.setName(item.getName());
                newItem.setParentId(item.getParentId());
                newItem.setLength(item.getLength());
                newItem.setWidth(item.getWidth());
                newItem.setHeight(item.getHeight());
                newItem.setIsCategory(item.isCategory());
                newItem.setMeasure(item.getMeasure());
                newItem.setItems(recursiveAddToList(items, item.getId()));
                result.add(newItem);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return result;
    }

}