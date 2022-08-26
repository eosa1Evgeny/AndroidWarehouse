package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Класс-обёртка для передачи в качестве параметра в метод API "/api/Inventarization"
 */
public class Inventory {
    @SerializedName("lots")
    public List<InventoryLot> lots;

    public List<InventoryLot> getInventoryLots() { return lots;}
    public void setInventoryLots(List<InventoryLot> inventoryLots) { lots = inventoryLots; }
}
