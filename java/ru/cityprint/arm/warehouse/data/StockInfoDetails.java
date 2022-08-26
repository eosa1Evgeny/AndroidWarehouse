package ru.cityprint.arm.warehouse.data;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.List;

/**
 * Retrofit, детализация информации о товаре
 */
@SuppressWarnings("serial")
public class StockInfoDetails implements Serializable{
    @Expose
    public List<StockInfoLabel> stockInfo;
    @Expose
    public double width;
    @Expose
    public double height;
    @Expose
    public double length;
    @Expose
    public List<String> barCodes;
    @Expose
    public String measure;
    @Expose
    public String id;
    @Expose
    public String parentID;
    @Expose
    public boolean isCategory;
    @Expose
    public String name;

    public List<StockInfoLabel> getStockInfo() {
        return stockInfo;
    }

    public void setStockInfo(List<StockInfoLabel> stockInfo) {
        this.stockInfo = stockInfo;
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

    public List<String> getBarCodes() {
        return barCodes;
    }

    public void setBarCodes(List<String> barCodes) {
        this.barCodes = barCodes;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentID() {
        return parentID;
    }

    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    public boolean isCategory() {
        return isCategory;
    }

    public void setCategory(boolean category) {
        isCategory = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
