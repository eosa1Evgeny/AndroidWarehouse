package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;

/**
 * Добавление номенклатуры на сервер
 *
 */
public class Nomenclature {
    @Expose
    public String name;
    @Expose
    public float width;
    @Expose
    public float height;
    @Expose
    public float length;
    @Expose
    public String barCode;
    @Expose
    public String measure;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getLength() {
        return length;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }
}
