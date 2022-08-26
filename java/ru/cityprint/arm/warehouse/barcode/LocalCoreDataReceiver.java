package ru.cityprint.arm.warehouse.barcode;

/**
 * Created by seregin.aleksey on 21/03/2018.
 * Интерфейс для работы с данными, получаемыми от сканера ШК
 */

public interface LocalCoreDataReceiver {
    boolean onReceivedData(Object data);
}
