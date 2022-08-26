package ru.cityprint.arm.warehouse.ui;

import android.app.DialogFragment;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import rs.core.hw.Barcode;
import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.barcode.LocalCoreDataReceiver;
import ru.cityprint.arm.warehouse.data.ItemHelper;
import ru.cityprint.arm.warehouse.data.ItemResponse;
import ru.cityprint.arm.warehouse.data.NomenclatureListener;
import ru.cityprint.arm.warehouse.data.StockInfoDetails;
import ru.cityprint.arm.warehouse.network.WifiHelper;
import ru.cityprint.arm.warehouse.utils.SoundHelper;

/**
 * Создание ШК для данного товара
 */
public class StockInfoBarcodesActivity extends AppCompatActivity implements LocalCoreDataReceiver, NomenclatureListener{
    private String TAG = "StockInfoBarcodesActivity";
    private RecyclerView mRecyclerView;
    private BarCodesAdapter mAdapter;
    private List<String> mItemList;
    private String mItemId;                                     // id товара
    private ImageButton mBack;
    private FloatingActionButton mAddBarcode;                   // добавление ШК
    private boolean mAllowBarcode = false;                      //разрешение на создание штрих-кода
    private String mBarCodeValue;                               //штрих-код со сканера
    private ItemHelper mItemHelper;                             // работа с таблицей Items
    private SoundHelper soundHelper;                            // работа со звуковым оповещением при сканировании
    private static final String MESSAGE_TITLE = "Информация";
    private static final String MESSAGE_WIFI_ERROR_TEXT = "К сожалению, в настоящий момент отсутствует соединение с WiFi. Проверьте, пожалуйста настройки WiFi.";
    private static final int YES_NO_CALL = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_info_barcodes);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_inv);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        mAddBarcode = findViewById(R.id.floatingActionButton_stock_info);
        mAddBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBarcode();
            }
        });

        mBack = findViewById(R.id.image_button_back);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // инициализация RecyclerView
        mRecyclerView = findViewById(R.id.barcodes_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Singleton ItemHelper
        mItemHelper = ItemHelper.get(this);

        Intent i = getIntent();
        StockInfoDetails details =  (StockInfoDetails)i.getSerializableExtra("StockInfoDetails");

        TextView title = findViewById(R.id.textViewTitle);
        title.setText(details.getName());

        mItemId = details.getId();

        // проверяем есть ли активное соединение с WiFi
        if (!isWifiConnected()) {
            // если нет соединения с WiFi - отобображаем пользователю инфо
            displayWifiErrorDialog();
        }

        //подготавливаем список
        mItemList = prepareBarCodes(details);
        updateUI(mItemList);

        // инициализация объекта SoundHelper - звук при сканировании
        soundHelper = new SoundHelper(this);

    }

    /**
     * действия по нажатию кнопки FAB
     */
    private void addBarcode(){
        //вывести диалог
        alertDialog();
        //в диалоге установить mAllowBarcode = true
        //потом вернуть ее в false
    }

    /**
     * Диалог создания штрих-кода
     */

    private void alertDialog() {

        //TODO  сканер заполняет баркод вместо R.string.qinput_prefill

        new MaterialDialog.Builder(this)
                .title("Введите или отсканируйте штрих-код")
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input(R.string.qinput_hint, R.string.qinput_prefill, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                input = String.valueOf(input);
                                if (input==null || input.equals("")){
                                    dialog.cancel();
                                }else {
                                    mItemList.add(input.toString());
                                    updateUI(mItemList);
                                }
                            }
                        }
                )
                .positiveText(R.string.qchoose)
                .negativeText(R.string.qnegative)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mAllowBarcode = true;
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mAllowBarcode = false;
                        dialog.cancel();
                    }
                })
                .show();
    }

    /**
     * Проверка - существует ли активное (подключенное) WiFi-соединение
     */
    private Boolean isWifiConnected() {
        return WifiHelper.isConnected(this);
    }

    /**
     * Отображение диалогового окна при возникновении ошибки соединения с WiFi
     */
    private void displayWifiErrorDialog() {
        DialogFragment dialog = new MessageDialog();
        Bundle args = new Bundle();
        args.putString(MessageDialog.ARG_TITLE, MESSAGE_TITLE);
        args.putString(MessageDialog.ARG_MESSAGE, MESSAGE_WIFI_ERROR_TEXT);
        dialog.setArguments(args);
        dialog.setTargetFragment(dialog, YES_NO_CALL);
        dialog.show(getFragmentManager(), "tag");
    }

    /**
     * Подготавливаем список к передаче в RecyclerView
     * @param details
     * @return
     */
    private List<String> prepareBarCodes(StockInfoDetails details){

        List<String> barCodes = details.getBarCodes();
        List<String> rvBarCodes = new ArrayList<>();

        for (String barCode: barCodes ){

            String barcode77 = barCode;
            // убрать "77-" вначале
            String[] array = barcode77.split("-");
            String label="";
            if (array.length==2) {
                label = array[1];
            }else {
                label = barcode77;
            }
            rvBarCodes.add(label);
        }

        return rvBarCodes;
    }

    /**
     * обновление списка
     */
    private void updateUI(List<String> itemList) {

        List<String> tmpList = new ArrayList<>();
        tmpList.addAll(itemList);

        // при работе с БД
        if (mAdapter == null) {
            mAdapter = new BarCodesAdapter(tmpList);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter = new BarCodesAdapter(tmpList);
            mRecyclerView.setAdapter(mAdapter);

        }

    }

    /**
     * метод интерфейса LocalCoreDataReceiver
     * @param data - данные, которые предоставляет служба RSCore от сканера
     * @return - Boolean
     */
    @Override
    public boolean onReceivedData(Object data) {
        if (data != null && data instanceof Barcode) {
            final String barcodeValue = ((Barcode) data).value;
            if (!barcodeValue.isEmpty()) {
                // проверяем - разрешено ли сканировать
                if (mAllowBarcode) {

                    // если ШК нет в списке
                    if (notInList(barcodeValue)) {
                        mBarCodeValue = barcodeValue;

                        // отправляем данные в сервис (API)
                        sendBarcodeToServer();
                    }else{
                        // если ШК есть в списке, выводим сообщение
                        Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Такой баркод уже в списке", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Проверка, есть ли уже в списке вносимый баркод
     */
    private boolean notInList(String barCodeValue){

        for (String item: mItemList){
            if (item.equals(barCodeValue)){
                return false;
            }
        }
        //нет в списке
        return true;
    }

    /**
     *
     * @param response
     */
    @Override
    public void onNomenclatureResponse(ItemResponse response) {
        if (response.getCode() == "200") {
            // добавляем в список
            mItemList.add(mBarCodeValue);
            // обновляем UI
            updateUI(mItemList);

            // проигрываем звуковое оповещение (успешное)
            soundHelper.playSuccessSound();
        } else {
            soundHelper.playErrorSound();
        }
    }


    /**
     * Адаптер
     */
    private class BarCodesAdapter extends RecyclerView.Adapter<BarCodesAdapter.InfoHolder> {
        private List<String> items;
        private String item;

        /**
         * Holder
         */
        public class InfoHolder extends RecyclerView.ViewHolder implements RecyclerView.OnClickListener {
            public TextView mTitleTextView;

            public InfoHolder(View itemView) {
                super(itemView);
                mTitleTextView = itemView.findViewById(R.id.barcode_name);
                itemView.setOnClickListener(this);

            }

            @Override
            public void onClick(View v) {

            }
        }

        /**
         * Конструктор
         * @param itemList
         */

        public BarCodesAdapter(List<String> itemList) {
            this.items = itemList;
        }

        @Override
        public InfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(StockInfoBarcodesActivity.this);
            View view = layoutInflater.inflate(R.layout.list_item_stock_info_barcodes, parent, false);
            return new InfoHolder(view);
        }

        @Override
        public void onBindViewHolder(final InfoHolder holder, final int position) {
            item = items.get(position);
            TextView titleTextView = holder.mTitleTextView;
            titleTextView.setText(item);

        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void setInfoList(List<String> itemList) {
            this.items.clear();
            this.items.addAll(itemList);
            notifyDataSetChanged();
        }

    }

    /**
     * Отправка ШК на сервер
     */
    private void sendBarcodeToServer() {
        if (!mItemId.isEmpty()) {
            Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Подождите, идет обмен данными...", Snackbar.LENGTH_SHORT).show();
            mItemHelper.sendBarcodeToServer(mItemId, mBarCodeValue);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}


