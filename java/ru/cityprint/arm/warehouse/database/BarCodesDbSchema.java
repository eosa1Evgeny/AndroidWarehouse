package ru.cityprint.arm.warehouse.database;

/**
 * Схема таблицы Barcodes в БД
 * Created by evgeny on 13.03.18.
 */
public class BarCodesDbSchema {

    public static final class BarCodesTable{
        public static final String TABLE_NAME = "barcodes";

        public static final class Cols{
            public static final String UUID = "uuid";
            public static final String ITEM_ID = "itemId";
            public static final String NAME = "name";
            public static final String BARCODE = "barcode";
            public static final String QUANTITY = "quantity";
            public static final String MEASURE = "measure";
            public static final String CHECKED = "checked";
        }
    }

}
