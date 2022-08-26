package ru.cityprint.arm.warehouse.data;

/**
 * Слушаем возврат от сервера и передаем в соответствующую функцию
 */
public interface AcceptanceListener {
    void onAcceptanceResponse(ApiResponse response);
    void onAcceptanceItem(ItemResponse response);
    void onAcceptanceMeasure(MeasureResponse response);
    void onAcceptanceStock(StockInfoResponse response);
}