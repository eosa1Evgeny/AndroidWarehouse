package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Ответ от сервера с единицами измерения.
 */
public class MeasureResponse {
    @Expose
    public String code;
    @Expose
    public String message;
    @Expose
    public List<Measure> data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Measure> getData() {
        return data;
    }

    public void setData(List<Measure> data) {
        this.data = data;
    }
}
