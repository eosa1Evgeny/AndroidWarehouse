package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Модель, представляющая количество и идентификатор товара для передачи в метод API
 */
public class AcceptanceLot {
    //Item id
    @SerializedName("id")
    @Expose
    public String id;

    //quantity
    @SerializedName("qty")
    @Expose
    public float qty;

    @SerializedName("name")
    @Expose
    public String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getQty() {
        return qty;
    }

    public void setQty(float qty) {
        this.qty = qty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
