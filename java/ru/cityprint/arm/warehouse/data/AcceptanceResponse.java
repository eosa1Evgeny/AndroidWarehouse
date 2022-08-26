package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Ответ, возвращаемый по запросу "/api/Acceptance" от сервиса API
 */
public class AcceptanceResponse {
    @SerializedName("code")
    @Expose
    public int code;
    @SerializedName("message")
    @Expose
    public String message;
    @SerializedName("data")
    @Expose
    public List<Acceptance> data;

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

    public List<Acceptance> getData() {
        return data;
    }

    public void setData(List<Acceptance> data) {
        this.data = data;
    }
}
