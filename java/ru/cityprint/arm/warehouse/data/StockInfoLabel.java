package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

/**
 * Retrofit, получение меток товара
 */
@SuppressWarnings("serial")
public class StockInfoLabel implements Serializable{
    @Expose
    public String label;
    @Expose
    public double qty;


    //единица измерения, нужна только для отображения в RecycledView, не участвует в обмене
    private String measure;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }
}
