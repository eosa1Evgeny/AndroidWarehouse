package ru.cityprint.arm.warehouse.database;

/**
 * Схема таблицы ApiBarCodes в БД
 */
public class ApiBarCodesDBSchema {

    public static final class ApiBarCodesTable{
        public static final String TABLE_NAME = "ApiBarCodes";

        public static final class Cols{
            public static final String ID = "id";
            public static final String BARCODE = "barcode";
            public static final String ITEM_ID = "itemId";
        }
    }
}
