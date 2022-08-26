package ru.cityprint.arm.warehouse.data;

/**
 * Слушатель ответа от сервиса API (/api/Nomenclature/{id})
 * Created by seregin.aleksey on 28/04/2018.
 */

public interface NomenclatureListener {
    void onNomenclatureResponse(ItemResponse response);
}
