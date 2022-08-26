package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;

/**
 * Единицы измерения
 */
public class Measure {
    @Expose
    public String code;
    @Expose
    public String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
