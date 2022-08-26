package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;

/**
 * Retrofit, ответ на запрос о детализации по товару
 */

public class StockInfoResponse {
    @Expose
    public int code;
    @Expose
    public String message;
    @Expose
    public StockInfoDetails data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public StockInfoDetails getData() {
        return data;
    }

    public void setData(StockInfoDetails data) {
        this.data = data;
    }
}
