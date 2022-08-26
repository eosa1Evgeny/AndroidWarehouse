package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Класс-обёртка для передачи в качестве параметра в метод API "/api/Manufacture"
 */
public class Manufacture {
    @SerializedName("lots")
    public List<ManufactureLot> lots;

    public List<ManufactureLot> getManufactureLots() { return lots;}
    public void setManufactureLots(List<ManufactureLot> manufactureLots) { lots = manufactureLots; }
}
