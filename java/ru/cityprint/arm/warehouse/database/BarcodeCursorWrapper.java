package ru.cityprint.arm.warehouse.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.UUID;

import ru.cityprint.arm.warehouse.data.BarCode;
import ru.cityprint.arm.warehouse.database.BarCodesDbSchema.BarCodesTable;

/**
 * Курсор, содержащий данные из таблицы Barcodes
 * Created by evgeny on 13.03.18.
 */
public class BarcodeCursorWrapper extends CursorWrapper{
    public BarcodeCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public BarCode getBarCode(){
        String uuidString = getString(getColumnIndex(BarCodesTable.Cols.UUID));
        String name = getString(getColumnIndex(BarCodesTable.Cols.NAME));
        String barcode = getString(getColumnIndex(BarCodesTable.Cols.BARCODE));
        int quantity = getInt(getColumnIndex(BarCodesTable.Cols.QUANTITY));
        String measure = getString(getColumnIndex(BarCodesTable.Cols.MEASURE));
        int checked = getInt(getColumnIndex(BarCodesTable.Cols.CHECKED));
        String itemId = getString(getColumnIndex(BarCodesTable.Cols.ITEM_ID));

        BarCode barCode = new BarCode(UUID.fromString(uuidString));
        barCode.setName(name);
        barCode.setBarcode(barcode);
        barCode.setMeasure(measure);
        barCode.setQuantity(quantity);
        barCode.setChecked(checked!=0);
        barCode.setItemId(itemId);

        return barCode;
    }
}
