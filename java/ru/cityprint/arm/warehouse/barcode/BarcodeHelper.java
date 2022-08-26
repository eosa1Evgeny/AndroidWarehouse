package ru.cityprint.arm.warehouse.barcode;

/**
 * Функции для работы со ШК
 * Created by Alex on 06.05.2018.
 */

public class BarcodeHelper {

    private final static String BARCODE_LABEL_PREFIX = "77-";
    private final static String BARCODE_LABEL_SEPARATOR = "-";

    /**
     * Возвращает признак является ли значение ШК меткой
     * @param barcodeValue
     * @return
     */
    public static Boolean isBarcodeLabel(String barcodeValue) {
        return barcodeValue.contains(BARCODE_LABEL_PREFIX);
    }

    /**
     * Возвращает значение ШК для метки
     * @param barcodeValue - String
     * @return
     */
    public static String getBarcodeLabel(String barcodeValue) {
        return barcodeValue.substring(barcodeValue.lastIndexOf(BARCODE_LABEL_SEPARATOR) + 1);
    }
}
