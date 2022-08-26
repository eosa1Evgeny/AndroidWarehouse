package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Класс-обёртка для передачи в качестве параметра в метод API "/api/Acceptance"
 */
public class Acceptance {
    @SerializedName("lots")
    @Expose
    public List<AcceptanceLot> lots;

    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("name")
    @Expose
    public String name;

    public List<AcceptanceLot> getAcceptanceLots() { return lots;}
    public void setAcceptaneeLots(List<AcceptanceLot> acceptanceLots) { lots = acceptanceLots; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
