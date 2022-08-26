package ru.cityprint.arm.warehouse.data;

/**
 * Ответ, возвращаемый по запросу "/api/Inventarization" от сервиса API
 */
public class InventoryResponse {
    public String code;
    public String message;
    public String data;

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
