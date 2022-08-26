package ru.cityprint.arm.warehouse.data;

/**
 * Слушаем возврат от сервера (/api/Inventarization) и передаем в соответствующую функцию
 */
public interface InventoryListener {
    void onInventoryResponse(InventoryResponse response);
}