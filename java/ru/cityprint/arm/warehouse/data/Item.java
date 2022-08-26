package ru.cityprint.arm.warehouse.data;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Класс, представляющий объект Item(Категория, Товар)
 * Created by Alex on 24.03.2018.
 */

public class Item {

    @SerializedName("stockInfo")
    @Expose
    private List<StockInfoLabel> stockInfo = null;

    @SerializedName("width")
    @Expose
    private double width;

    @SerializedName("height")
    @Expose
    private double height;

    @SerializedName("length")
    @Expose
    private double length;

    @SerializedName("barCodes")
    @Expose
    private List<Object> barCodes = null;

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("parentID")
    @Expose
    private String parentId;

    @SerializedName("isCategory")
    @Expose
    private boolean isCategory;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("measure")
    @Expose
    private String measure;

    @SerializedName("items")
    @Expose
    private List<Item> items = null;
    //инвентаризационный баркод
    private String mInventoryBarCode;
    //галочки
    private boolean mSetChecked;


    public boolean isSetChecked() {
        return mSetChecked;
    }

    public void setSetChecked(boolean setChecked) {
        mSetChecked = setChecked;
    }

    public String getInventoryBarCode() {
        return mInventoryBarCode;
    }

    public void setInventoryBarCode(String inventoryBarCode) {
        mInventoryBarCode = inventoryBarCode;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public List<Object> getBarCodes() {
        return barCodes;
    }

    public void setBarCodes(List<Object> barCodes) {
        this.barCodes = barCodes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isCategory() {
        return isCategory;
    }

    public void setIsCategory(boolean isCategory) {
        this.isCategory = isCategory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<StockInfoLabel> getStockInfo() {
        return stockInfo;
    }

    public void setStockInfo(List<StockInfoLabel> stockInfo) {
        this.stockInfo = stockInfo;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

}
