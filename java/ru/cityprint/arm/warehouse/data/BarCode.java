package ru.cityprint.arm.warehouse.data;

import java.util.UUID;

/**
 * Класс Barcode - представляет объект для работы со ШК
 * Created by evgeny on 11.03.18.
 */
public class BarCode {
    private UUID mId;
    private String mName;
    private String mBarcode;
    private String mMeasure;
    private float mQuantity;
    private boolean mChecked;
    private String mItemId;

    public String getItemId() {
        return mItemId;
    }

    public void setItemId(String itemId) {
        mItemId = itemId;
    }

    public BarCode() {
        mId = UUID.randomUUID();
    }

    public BarCode(UUID id) {
        mId = id;
    }

    public boolean getChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(UUID id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getBarcode() {
        return mBarcode;
    }

    public void setBarcode(String barcode) {
        mBarcode = barcode;
    }

    public String getMeasure() {
        return mMeasure;
    }

    public void setMeasure(String measure) {
        mMeasure = measure;
    }

    public float getQuantity() {
        return mQuantity;
    }

    public void setQuantity(float quantity) {
        mQuantity = quantity;
    }

}
