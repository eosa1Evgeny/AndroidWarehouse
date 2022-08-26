package ru.cityprint.arm.warehouse.ui;

import android.app.DialogFragment;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.math.BigDecimal;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.barcode.CoreApplication;
import ru.cityprint.arm.warehouse.data.AcceptanceListener;
import ru.cityprint.arm.warehouse.data.ItemResponse;
import ru.cityprint.arm.warehouse.data.MeasureResponse;
import ru.cityprint.arm.warehouse.data.ApiResponse;
import ru.cityprint.arm.warehouse.data.StockInfoDetails;
import ru.cityprint.arm.warehouse.data.StockInfoLabel;
import ru.cityprint.arm.warehouse.data.StockInfoResponse;
import ru.cityprint.arm.warehouse.network.WifiHelper;


public class StockInfoActivity extends AppCompatActivity implements AcceptanceListener {

    private String mItemId;
    private String TAG="StockInfoActivity";
    private AcceptanceListener mAcceptanceListener;
    private RecyclerView mRecyclerView;
    private InfoAdapter mAdapter;
    private ImageButton mBack;
    private ImageButton mBarCodeButton;
    private static final String MESSAGE_TITLE = "Информация";
    private static final String MESSAGE_WIFI_ERROR_TEXT = "К сожалению, в настоящий момент отсутствует соединение с WiFi. Проверьте, пожалуйста настройки WiFi.";
    private static final int YES_NO_CALL = 100;
    private StockInfoDetails mStockInfoDetails;
    private TextView mStockInfoItemsQuantity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_info);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_inv);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        mBack = findViewById(R.id.image_button_back);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mBarCodeButton = findViewById(R.id.image_button_barcode);
        mBarCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //вызов StockInfoBarcodesActivity
                callStockInfoBarcodeActivity();
            }
        });

        // инициализация RecyclerView
        mRecyclerView = findViewById(R.id.info_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAcceptanceListener = (AcceptanceListener)this;
        //переданный из поиска id
        Intent i = getIntent();
        mItemId = i.getStringExtra("id_item");

        // Итого количество
        mStockInfoItemsQuantity = findViewById(R.id.stock_items_count);

        // проверяем есть ли активное соединение с WiFi
        if (!isWifiConnected()) {
            // если нет соединения с WiFi - отобображаем пользователю инфо
            displayWifiErrorDialog();
        }

        getStockInfoDetails();

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


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void callStockInfoBarcodeActivity(){

        if (mStockInfoDetails!=null) {
            Intent i = new Intent(this, StockInfoBarcodesActivity.class);
            i.putExtra("StockInfoDetails", mStockInfoDetails);
            startActivity(i);
        }
    }

    /**
     * Retrofit получение информации о товаре
     */
    public void getStockInfoDetails(){

        CoreApplication.getApi().getStockInfoDetails(mItemId).enqueue(new Callback<StockInfoResponse>() {
            @Override
            public void onResponse(Call<StockInfoResponse> call, Response<StockInfoResponse> response) {


                if (response.isSuccessful()) {
                    //ок
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceStock(response.body());


                    }

                } else {
                    // ошибка
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceStock(response.body());
                    }
                }

            }

            @Override
            public void onFailure(Call<StockInfoResponse> call, Throwable t) {
                String error = t.getMessage();

                Log.d(TAG, error);

                try {
                    StockInfoResponse mResult = new StockInfoResponse();
                    mResult.code = call.execute().code();
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;
                    if (mAcceptanceListener != null){
                        mAcceptanceListener.onAcceptanceStock(mResult);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });

    }

    @Override
    public void onAcceptanceResponse(ApiResponse response) {

    }

    @Override
    public void onAcceptanceItem(ItemResponse response) {

    }

    @Override
    public void onAcceptanceMeasure(MeasureResponse response) {

    }

    /**
     * Listener получения товара
     * @param response
     */
    @Override
    public void onAcceptanceStock(StockInfoResponse response){

        if (response == null) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.titleS)
                    .content("Нет ответа от сервера. Проверьте наличие сети")
                    .positiveText(R.string.agreeS)
                    .show();
            return;
        }



        if (response.message.equals("")) {//нет ошибок

            //это передаем в активность StockInfoBarcodesActivity
            mStockInfoDetails =response.data;

            String name = response.data.name;
            TextView title = findViewById(R.id.textViewTitle);
            title.setText(name);

            String measure = response.data.measure;
            String lenght = String.valueOf(response.data.length);
            String width = String.valueOf(response.data.width);
            String height = String.valueOf(response.data.height);

            TextView tvLenght = findViewById(R.id.lenght_val);
            tvLenght.setText(lenght);
            TextView tvWidth = findViewById(R.id.width_val);
            tvWidth.setText(width);
            TextView tvHeight = findViewById(R.id.height_val);
            tvHeight.setText(height);
            TextView tvMeasure1 = findViewById(R.id.measure1);
            tvMeasure1.setText(measure);
            TextView tvMeasure2 = findViewById(R.id.measure2);
            tvMeasure2.setText(measure);
            TextView tvMeasure3 = findViewById(R.id.measure3);
            tvMeasure3.setText(measure);

            List<StockInfoLabel> stockInfos = response.data.stockInfo;

            for (StockInfoLabel stockInfo: stockInfos ){

             String label77 = String.valueOf(stockInfo.getLabel());
             // убрать "77-" вначале
             String[] array = label77.split("-");
             String label="";
             if (array.length==2) {
                  label = array[1];
             }else {
                  label = label77;
             }
                stockInfo.setLabel(label);
                //добавим единицу измерения для отображения в RV
                stockInfo.setMeasure(measure);
            }

            Toast.makeText(this, "Передача данных прошла успешно", Toast.LENGTH_SHORT).show();
            updateUI(stockInfos);


        } else {
            //ошибки, алерт
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.titleS)
                    .content("code: "+ String.valueOf(response.code) + " data: " + String.valueOf(response.data) + " message: " + String.valueOf(response.message))
                    .positiveText(R.string.agreeS)
                    .show();
        }
    }

    /**
     * обновление списка
     */
    private void updateUI(List<StockInfoLabel> itemList) {
        // при работе с БД
        if (mAdapter == null) {
            mAdapter = new InfoAdapter(itemList);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setInfoList(itemList);
            mAdapter.notifyDataSetChanged();
        }

        // обновляем количество ("Итого количество")
        mStockInfoItemsQuantity.setText( mStockInfoItemsQuantity.getText() + " " + String.valueOf(getStockInfoItemsQuantity(itemList)));
    }


    /**
     * Возвращает общее количество
     * @param items
     * @return
     */
    private BigDecimal getStockInfoItemsQuantity(List<StockInfoLabel> items) {
        double result = 0;

        for (StockInfoLabel item: items) {
            result += item.getQty();
        }
        BigDecimal qtyRound = new BigDecimal(result);
        return qtyRound.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Holder
     */
    private class InfoHolder extends RecyclerView.ViewHolder implements RecyclerView.OnClickListener {
        public StockInfoLabel mItem;
        private TextView mTitleTextView;
        private TextView mValueTextView;
        private TextView mMeasureTextView;


        public InfoHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.name);
            mValueTextView = (TextView) itemView.findViewById(R.id.value);
            mMeasureTextView = (TextView) itemView.findViewById(R.id.measure);
        }

        public void bindInfo(StockInfoLabel Item) {
            mItem = Item;
            mTitleTextView.setText(mItem.getLabel());
            mValueTextView.setText(String.valueOf(mItem.getQty()));
            mMeasureTextView.setText(mItem.getMeasure());
        }

        @Override
        public void onClick(View v) {

        }
    }

    /**
     * Адаптер
     */
    private class InfoAdapter extends RecyclerView.Adapter<InfoHolder> {
        private List<StockInfoLabel> items;

        public InfoAdapter(List<StockInfoLabel> itemList) {
            this.items = itemList;
        }

        @Override
        public InfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(StockInfoActivity.this);
            View view = layoutInflater.inflate(R.layout.list_item_info_activity, parent, false);
            return new InfoHolder(view);
        }

        @Override
        public void onBindViewHolder(final InfoHolder holder, final int position) {
            final StockInfoLabel item = items.get(position);
            holder.bindInfo(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void setInfoList(List<StockInfoLabel> itemList) {
            items = itemList;
        }

    }


}
