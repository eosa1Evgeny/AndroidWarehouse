package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;

/**
 * Ответ, возвращаемый по запросу "/api/Acceptance" от сервиса API
 */
public class ApiResponse {
    @Expose
    public int code;
    @Expose
    public String message;
    @Expose
    public String data;

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
