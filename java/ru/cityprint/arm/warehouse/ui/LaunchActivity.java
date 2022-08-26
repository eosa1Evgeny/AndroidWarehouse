package ru.cityprint.arm.warehouse.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.data.BarCodeHelper;
import ru.cityprint.arm.warehouse.data.ItemHelper;
import ru.cityprint.arm.warehouse.data.MeasureHelper;

/**
 * Экран загрузки данных и отображения логотипа
 */
public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        start();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        finish();
    }

    /**
     * загружаем объекты из Json в БД
     */
    protected void start() {
       /* //загружаем таблицу БД из Json
        //перед этим очищаем таблицы
        ItemHelper itemHelper = ItemHelper.get(this);
        BarCodeHelper barCodeHelper = BarCodeHelper.get(this);
        barCodeHelper.clearTables();
        MeasureHelper measureHelper = MeasureHelper.get(this);
        measureHelper.clearTables();*/

        //int result = itemHelper.loadAll();

        //логин
        //TODO временно обходим логин
        //Intent i = new Intent(LaunchActivity.this, LoginActivity.class);
        Intent i = new Intent(LaunchActivity.this, ModulesActivity.class );
        //1-есть wifi, 0-нет
        /*if (result == 0) {
            i.putExtra("wiFi", false);
        } else {
            i.putExtra("wiFi", true);
        }*/
        startActivity(i);
    }

}
