package ru.cityprint.arm.warehouse.data;

/**
 * Слушаем возврат от сервера (/api/Manufacture) и передаем в соответствующую функцию
 */
public interface ManufactureListener {
    void onManufactureResponse(ManufactureResponse response);
}