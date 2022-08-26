package ru.cityprint.arm.warehouse.database;

import android.database.Cursor;
import android.database.CursorWrapper;
import ru.cityprint.arm.warehouse.data.Item;
import ru.cityprint.arm.warehouse.database.ItemsDbSchema.*;

/**
 * Курсор, содержащий данные из таблицы Items
 */
public class ItemCursorWrapper extends CursorWrapper{
    public ItemCursorWrapper(Cursor cursor){
        super(cursor);
    }

    public Item getItem(){

        String id = getString(getColumnIndex(ItemsTable.Cols.ID));
        String name = getString(getColumnIndex(ItemsTable.Cols.NAME));
        String measure = getString(getColumnIndex(ItemsTable.Cols.MEASURE));
        String parentId = getString(getColumnIndex(ItemsTable.Cols.PARENT_ID));
        int width = getInt(getColumnIndex(ItemsTable.Cols.WIDTH));
        double height = getDouble(getColumnIndex(ItemsTable.Cols.HEIGHT));
        int isCategory = getInt(getColumnIndex(ItemsTable.Cols.IS_CATEGORY));
        double length = getDouble(getColumnIndex(ItemsTable.Cols.LENGTH));


        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setParentId(parentId);
        item.setWidth(width);
        item.setHeight(height);
        item.setIsCategory(isCategory!=0);
        item.setLength(length);
        item.setMeasure(measure);

        return item;

    }
}
