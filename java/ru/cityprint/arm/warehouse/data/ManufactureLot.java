package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.SerializedName;

/**
 * Модель, представляющая количество и идентификатор товара для передачи
 * товаров в производство - в метод API
 */
public class ManufactureLot {
    //Item id
    @SerializedName("id")
    public String id;

    //quantity
    @SerializedName("qty")
    public int qty;

    // ШК (barcode)
    @SerializedName("code")
    public String code;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
