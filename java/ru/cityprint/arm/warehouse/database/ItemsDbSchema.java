package ru.cityprint.arm.warehouse.database;

/**
 * Схема таблицы Items в БД
 */
public class ItemsDbSchema {

    public static final class ItemsTable{
        public static final String TABLE_NAME = "Items";

        public static final class Cols{
            public static final String ID = "id";
            public static final String NAME = "name";
            public static final String PARENT_ID = "parent_id";
            public static final String WIDTH = "width";
            public static final String HEIGHT = "height";
            public static final String LENGTH = "length";
            public static final String MEASURE = "measure";
            public static final String IS_CATEGORY = "is_Category";
        }
    }
}


