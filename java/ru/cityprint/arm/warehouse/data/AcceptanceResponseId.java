package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AcceptanceResponseId {
    @SerializedName("code")
    @Expose
    public int code;
    @SerializedName("message")
    @Expose
    public String message;
    @SerializedName("data")
    @Expose
    public Acceptance data;

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

    public Acceptance getData() {
        return data;
    }

    public void setData(Acceptance data) {
        this.data = data;
    }
}
