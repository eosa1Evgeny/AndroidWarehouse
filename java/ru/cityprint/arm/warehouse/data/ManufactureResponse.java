package ru.cityprint.arm.warehouse.data;

/**
 * Ответ, возвращаемый по запросу "/api/Manufacture" от сервиса API
 */
public class ManufactureResponse {
    public int code;
    public String message;
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
