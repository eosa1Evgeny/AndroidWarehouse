package ru.cityprint.arm.warehouse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;

import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.data.AcceptanceResponse;
import ru.cityprint.arm.warehouse.data.BarCodeHelper;
import ru.cityprint.arm.warehouse.data.ItemHelper;
import ru.cityprint.arm.warehouse.data.MeasureHelper;
import ru.cityprint.arm.warehouse.data.OnDataLoadListener;

/**
 * Список модулей
 */

public class ModulesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnDataLoadListener {
    private Button mProducts;
    private Button mProduction;
    private Button mStock;
    private Button mInventory;
    private MaterialDialog mMaterialDialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modules);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.login_toolbar_modules);
        setSupportActionBar(toolbar);

        mProducts = findViewById(R.id.button1);
        mProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProductsActivity();
            }
        });

        // кнопка "Передача в производство"
        mProduction = findViewById(R.id.button2);
        mProduction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProductionActivity();
            }
        });

        // кнопка "Остатки на складе"
        mStock = findViewById(R.id.button3);
        mStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStockActivity();
            }
        });

        //Инвентаризация
        mInventory = findViewById(R.id.button4);
        mInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startInventoryActivity();
            }
        });

        // инициализация MaterialDialog - для отображения ProgressDialog
        // при загрузке данных
        MaterialDialog.Builder mMaterialDialogBuilder = new MaterialDialog.Builder(this)
                .title(R.string.data_load_title)
                .content(R.string.data_load_content)
                .cancelable(false)
                .progress(true, 0);

        mMaterialDialog = mMaterialDialogBuilder.build();

        // загрузка данных
        loadData();

        /*DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);  todo*/
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /** Формирование меню - ToolBar
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_settings, menu);
        return true;
    }

    /**
     * Обрабатывает выбор пункта меню - ToolBar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                // настройки приложения
                showMenuSettings();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Открывает активити с настройками
     */
    private void showMenuSettings() {
        // отображаем Activity с настройками приложения
        startActivity(new Intent(this, SettingsActivity.class));
    }


    /**
     * Открывает экран для сканирования ШК - прием товара(ProductsActivity)
     */
    private void startProductsActivity(){
        Intent i = new Intent(this, AcceptanceActivity.class);
        startActivity(i);
    }

    /**
     * Открывает экран для сканирования ШК - передача в производство(ProduceActivity)
     */
    private void startProductionActivity(){
        Intent i = new Intent(this, ProductionActivity.class);
        startActivity(i);
    }

    /**
     * Открывает экран со списком товаров (для получения информации об остатках) StockActivity
     */
    private void startStockActivity() {
        Intent i = new Intent(this, StockActivity.class);
        startActivity(i);
    }

    /**
     * Открывает экран для сканирования ШК - передача в производство(ProduceActivity)
     */
    private void startInventoryActivity(){
        Intent i = new Intent(this, InventoryActivity.class);
        startActivity(i);
    }

    /**
     * Обработка нажатия кнопки "Назад"
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        /*DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        } todo*/
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        /*DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);  todo*/
        return true;
    }

    /**
     * Загруза данных с сервера API
     */
    private void loadData() {
        // очищаем таблицы перед загрузкой
        ItemHelper itemHelper = ItemHelper.get(this);
        itemHelper.clearTables();
        BarCodeHelper barCodeHelper = BarCodeHelper.get(this);
        barCodeHelper.clearTable();

        // загружаем данные с сервера API
        itemHelper.loadAll();
    }

    /**
     * Возникает перед загрузкой данных с сервера API
     */
    @Override
    public void onStartingDataLoad() {
        // показываем модальное окно с Progress Dialog
        if (mMaterialDialog != null)
            mMaterialDialog.show();
    }

    /**
     * Возникает после загрузки данных с сервера API
     */
    @Override
    public void onCompleteDataLoad(String message) {
        // скрываем модальное окно с Progress Dialog
        if (mMaterialDialog != null)
            mMaterialDialog.dismiss();

        // если message содержит ошибку, отображаем текст
        if (!message.isEmpty()) {
            Snackbar.make(findViewById(R.id.modulesCoordinatorLayout), message, Snackbar.LENGTH_SHORT).show();
        }
    }
}