package ru.cityprint.arm.warehouse.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import ru.cityprint.arm.warehouse.data.Acceptance;
import ru.cityprint.arm.warehouse.data.AcceptanceResponseId;
import ru.cityprint.arm.warehouse.data.Inventory;
import ru.cityprint.arm.warehouse.data.InventoryResponse;
import ru.cityprint.arm.warehouse.data.ItemResponse;
import ru.cityprint.arm.warehouse.data.Manufacture;
import ru.cityprint.arm.warehouse.data.ManufactureResponse;
import ru.cityprint.arm.warehouse.data.MeasureResponse;
import ru.cityprint.arm.warehouse.data.Nomenclature;
import ru.cityprint.arm.warehouse.data.NomenclatureResponse;
import ru.cityprint.arm.warehouse.data.AcceptanceResponse;
import ru.cityprint.arm.warehouse.data.ApiResponse;
import ru.cityprint.arm.warehouse.data.StockInfoResponse;

/**
 * Интерфейс для работы с Web Service API с помощью Retrofit
 * Created by Seregin Alexey on 24.03.2018.
 */

public interface ServiceApi {
    // возвращает ответ от Web Service API - вся номенклатура
    @GET("/api/nomenclature/")
    Call<NomenclatureResponse> getAll();

    // отправляет список отсканированных товаров с указанным количеством
    @POST("/api/Acceptance")
    Call<ApiResponse> sendQuantity(@Body Acceptance data);

    // добавляет новую номенклатуру (на сервере) и возвращает id нового товара
    @POST("/api/Nomenclature/")
    Call<ItemResponse> addNewNomenclature(@Body Nomenclature item);

    // возвращает список единиц измерения
    @GET("/api/Measure/")
    Call<MeasureResponse> getMeasures();

    // детализация товара
    @GET("/api/Nomenclature/{id}")
    Call<StockInfoResponse> getStockInfoDetails(@Path("id") String id);

    // добавляет ШК к выбранному товару
    @POST("/api/Nomenclature/{id}")
    Call<ItemResponse> addNewBarcode(@Path("id") String id, @Body String barcodeValue);

    // отправляет список отсканированных товаров с указанным количеством в производство
    @POST("/api/Manufact")
    Call<ManufactureResponse> sendQuantityToProduction(@Body Manufacture data);

    // отправляет список отсканированных товаров в инвентаризацию
    @POST("/api/Inventarization")
    Call<InventoryResponse> sendToInventory(@Body Inventory data);

    // список отсканированных ранее и отправленных ШК
    @GET("/api/Acceptance")
    Call<AcceptanceResponse> getAcceptances();

    // получить ШК по id
    @GET("/api/Acceptance/{id}")
    Call<AcceptanceResponseId> getAcceptancesId(@Path("id") String id);

    // редактирование накладной
    @POST("/api/Acceptance/{id}")
    Call<ApiResponse> sendInvoiceById(@Path("id") String id, @Body Acceptance data);

    // "плоский" запрос номенклатуры
    @GET("/api/Nomenclature/items")
    Call<NomenclatureResponse> getItems();

    // "плоский" запрос категорий
    @GET("/api/Nomenclature/categories")
    Call<NomenclatureResponse> getCategories();
}

