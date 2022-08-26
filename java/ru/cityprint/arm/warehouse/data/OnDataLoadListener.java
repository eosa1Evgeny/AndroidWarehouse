package ru.cityprint.arm.warehouse.data;

/**
 * Слушатель загрузки данных
 * Created by Alex on 26.05.2018.
 */

public interface OnDataLoadListener {
    void onStartingDataLoad();
    void onCompleteDataLoad(String message);
}
