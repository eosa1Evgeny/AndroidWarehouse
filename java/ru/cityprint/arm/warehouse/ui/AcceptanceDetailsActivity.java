package ru.cityprint.arm.warehouse.ui;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.hanks.library.AnimateCheckBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rs.core.hw.Barcode;
import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.barcode.CoreApplication;
import ru.cityprint.arm.warehouse.barcode.LocalCoreDataReceiver;
import ru.cityprint.arm.warehouse.data.Acceptance;
import ru.cityprint.arm.warehouse.data.AcceptanceListener;
import ru.cityprint.arm.warehouse.data.AcceptanceLot;
import ru.cityprint.arm.warehouse.data.AcceptanceResponseId;
import ru.cityprint.arm.warehouse.data.BarCode;
import ru.cityprint.arm.warehouse.data.BarCodeHelper;
import ru.cityprint.arm.warehouse.data.Item;
import ru.cityprint.arm.warehouse.data.ItemHelper;
import ru.cityprint.arm.warehouse.data.ItemResponse;
import ru.cityprint.arm.warehouse.data.Measure;
import ru.cityprint.arm.warehouse.data.MeasureHelper;
import ru.cityprint.arm.warehouse.data.MeasureResponse;
import ru.cityprint.arm.warehouse.data.Nomenclature;
import ru.cityprint.arm.warehouse.data.ApiResponse;
import ru.cityprint.arm.warehouse.data.NomenclatureListener;
import ru.cityprint.arm.warehouse.data.StockInfoResponse;
import ru.cityprint.arm.warehouse.utils.SoundHelper;

/**
 * Список отсканированных ШК - прием товара
 */
public class AcceptanceDetailsActivity extends AppCompatActivity implements LocalCoreDataReceiver, AcceptanceListener, NomenclatureListener {
    private final String TAG = getClass().getSimpleName();
    private static final int YES_NO_CALL = 100;
    private FloatingActionButton fab;
    private TextView mTitle;                            // заголовок
    private RecyclerView mRecyclerView;                 // список отсканированных ШК
    private BarCodeAdapter mAdapter;                    // адаптер для списка ШК
    private ImageButton mClose;
    private ImageButton mSendData;                     // выделить все (для удаления)
    private int mCheck = 0;                             // количество выделенных элементов (галочка)
    private BarCodeHelper mBarCodeHelper;               // работа с таблицей Barcodes
    private ItemHelper mItemHelper;                     // работа с таблицей Items
    private String mBarcode;                            // отсканированный ШК
    private BarCode mDbBarcode;                         // ШК из базы
    private SoundHelper soundHelper;                    // работа со звуковым оповещением при сканировании
    private AcceptanceListener mAcceptanceListener;
    private MeasureHelper mMeasureHelper;
    private List<String> mMeasures;
    private String mId;
    private List<AcceptanceLot> mAcceptanceLots;
    private String mAcceptanceName;
    private Item mSelectedItem;
    private boolean isEditMode = false;


    // отправить на сервер

    // константы
    private final static String ACTION_EXIT = "exit";
    private final static String ACTION_DELETE = "delete";

    /**
     * Обработка нажатия кнопки "Назад"
     */
    @Override
    public void onBackPressed() {
        if (getBarcodesCount() > 0) {
            if (mId!=null){
                //режим редактирования накладной
                super.onBackPressed();
            }else {
                //режим сканирования
                alertDialog("Уверены, что хотите выйти?", "Содержимое будет очищено!", "Да", "Нет", ACTION_EXIT);
            }
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        // заголовок
        mTitle = findViewById(R.id.productsTitle);
        mTitle.setText(R.string.products);

        //получаем единицы измерения
        mMeasureHelper = MeasureHelper.get(this);
        //проверить перед этим таблицы на пустоту
        if(mMeasureHelper.getMeasures()==null || mMeasureHelper.getMeasures().size()==0) {
            getMeasures();
        }

        mAcceptanceListener = (AcceptanceListener) this;

        // Singleton BarCodeHelper
        mBarCodeHelper = BarCodeHelper.get(this);

        // Singleton ItemHelper
        mItemHelper = ItemHelper.get(this);

        // инициализация RecyclerView
        mRecyclerView = findViewById(R.id.product_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // кнопка "Выйти"
        mClose = findViewById(R.id.imageButton_close);
        mClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBarcodesCount() > 0) {
                    if (mId==null) {
                        alertDialog("Уверены, что хотите выйти?", "Содержимое будет очищено!", "Да", "Нет", ACTION_EXIT);
                    }else {
                        mBarCodeHelper.clearTable();
                        startAcceptanceActivity();
                    }
                } else {
                    mBarCodeHelper.clearTable();
                    startAcceptanceActivity();
                }
            }
        });

        // кнопка "Отправить данные"
        mSendData = findViewById(R.id.imageButton_sendData);
        mSendData.setEnabled(isSendDataEnabled());
        mSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Главное действие, прием товара (отправка на сервер)
                sendToServer();
            }
        });

        // кнопка "Добавить"
        fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCheck > 0) {
                    //удаление выделенных
                    alertDialog("Выделенные позиции будут удалены", "Вы уверены?", "Да", "Нет", ACTION_DELETE);
                } else {
                    // добавление новых
                    //singleChoiseListDialog();
                    addOrCancelDialog("Номенклатура", false);
                }
            }
        });

        // инициализация объекта SoundHelper - звук при сканировании
        soundHelper = new SoundHelper(this);

        //если передана накладная
        Intent i = getIntent();
        if (i.getStringExtra("Activity")!=null && i.getStringExtra("Activity").equals(AcceptanceActivity.TAG)){
            mId = i.getStringExtra("id");
        }
        // Получаем с сервера данные накладной
        if (mId!=null && !mId.equals("")){
            getAcceptancesId(mId);
        }

        // обновление UI
        updateUI();
    }//onCreate

    /**
     * Ретрофит, получение накладной
     * @param id
     */

    public void getAcceptancesId(String id){

        CoreApplication.getApi().getAcceptancesId(id).enqueue(new Callback<AcceptanceResponseId>() {
            @Override
            public void onResponse(Call<AcceptanceResponseId> call, Response<AcceptanceResponseId> response) {

               mAcceptanceLots = response.body().getData().getAcceptanceLots();
               mAcceptanceName = response.body().getData().getName();
               mTitle.setText(mAcceptanceName);

               for (AcceptanceLot lot : mAcceptanceLots){

                   BarCode barCode = new BarCode();
                   //конструктор перегружен, новые с новым Id, существующие с родным Id
                   barCode.setId(UUID.fromString(lot.getId()));
                   barCode.setName(lot.getName());
                   barCode.setBarcode("");
                   barCode.setMeasure("");
                   barCode.setQuantity(lot.getQty());
                   barCode.setChecked(false);
                   barCode.setItemId(lot.getId());
                    //добавляем в базу
                   mBarCodeHelper.addBarcode(barCode);

               }
                Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Передача данных прошла успешно", Snackbar.LENGTH_SHORT).show();
                updateUI();

            }

            @Override
            public void onFailure(Call<AcceptanceResponseId> call, Throwable t) {
                Log.e(TAG, t.getMessage());

                try {
                    AcceptanceResponseId mResult = new AcceptanceResponseId();
                    mResult.code = call.execute().code();
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;
                    showErrorGet(mResult);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });

    }

    /**
     * Показываем ошибку
     * @param result
     */
    private void  showErrorGet(AcceptanceResponseId result){
        new MaterialDialog.Builder(this)
                .title(R.string.titleS)
                .content("code: "+ String.valueOf(result.code) + " data: " + String.valueOf(result.data) + " message: " + String.valueOf(result.message))
                .positiveText(R.string.agreeS)
                .show();
    }

    /**
     * retrofit Передача накладной
     */

    public void sendInvoiceById(String id,  Acceptance data){

        CoreApplication.getApi().sendInvoiceById(id, data).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Отправлена накладная", Snackbar.LENGTH_SHORT).show();
                returnToAcceptanceActivity();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, t.getMessage());
                try {
                    ApiResponse mResult = new ApiResponse();
                    mResult.code = call.execute().code();
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;
                  showErrorPost(mResult);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }
    /**
     * Возврат к списку накладных
     */
    private void returnToAcceptanceActivity(){
       Intent i = new Intent(this, AcceptanceActivity.class);
       i.putExtra("TAG", TAG);
       startActivity(i);
       finish();
    }

    /**
     * Показываем ошибку
     * @param result
     */
    private void  showErrorPost(ApiResponse result){
        new MaterialDialog.Builder(this)
                .title(R.string.titleS)
                .content("code: "+ String.valueOf(result.code) + " data: " + String.valueOf(result.data) + " message: " + String.valueOf(result.message))
                .positiveText(R.string.agreeS)
                .show();
    }


    /**
     * Передача JSON (подготовка)
     */

    private void sendToServer() {

        Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Подождите, идет обмен данными...", Snackbar.LENGTH_SHORT).show();

        if (mId!=null && !mId.equals("")) {

            //передаем накладную

            Acceptance acceptance = new Acceptance();


            List<AcceptanceLot> lots = new ArrayList<>();


            BarCodeHelper barCodeHelper = BarCodeHelper.get(AcceptanceDetailsActivity.this);
            List<BarCode> barCodes = barCodeHelper.getBarCodes();

            for (BarCode barCode: barCodes){
                AcceptanceLot lot = new AcceptanceLot();
               lot.setId(barCode.getItemId());
               lot.setQty(barCode.getQuantity());
               lot.setName(barCode.getName());
               lots.add(lot);
            }

            acceptance.setAcceptaneeLots(lots);

            acceptance.setId(mId);
            acceptance.setName(mAcceptanceName);

            //json
            sendInvoiceById(mId, acceptance );

        }else {
            //иначе передаем количество
            mItemHelper.sendQuantity();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        // регистрируем слушатель события сканера (событие приходит на уровне Application)
        CoreApplication.getInstance().registerLocalListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // снимаем регистрацию слушателя события сканера
        CoreApplication.getInstance().unregisterLocalListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //очищаем таблицу Barcodes
        mBarCodeHelper.clearTable();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Получение от сканера ШК
     *
     * @param data
     * @return - Возвращает boolean
     */
    @Override
    public boolean onReceivedData(Object data) {
        if (data != null && data instanceof Barcode) {
            final String barcodeValue = ((Barcode) data).value;
            if (!barcodeValue.isEmpty()) {

                // инициализируем переменную mBarcode
                setBarcode(barcodeValue);

                if (!mBarcode.isEmpty()) {
                    if (!isEditMode)
                        barcodeValidate(barcodeValue);

                    // проигрываем звуковое оповещение (успешное)
                    soundHelper.playSuccessSound();

                    // для целей тестирования сканирования ШК
                    Toast.makeText(this, mBarcode, Toast.LENGTH_LONG).show();
                } else {
                    soundHelper.playErrorSound();
                }
            } else {
                soundHelper.playErrorSound();
            }
        }
        return false;
    }

    /**
     * Инициализация глобальной переменной mBarcode (ШК)
     */
    private void setBarcode(String barcodeValue) {
        mBarcode = barcodeValue;
    }

    /**
     * Возвращает значение ШК
     *
     * @return - значение ШК
     */
    private String getBarcode() {
        return mBarcode;
    }

    /**
     * @param barcodeValue - значение ШК
     */
    private void barcodeValidate(String barcodeValue) {
        Item item = mItemHelper.getItemByBarcode(barcodeValue);
        String barcodeId;


        if (item == null) {
            // номенклатура не привязана к ШК
            // предлагаем выбрать из CategoryActivity
            addOrCancelDialog("Номенклатура не найдена", true);
        } else {
            isEditMode = true;
            //получим BarCode из базы, если он там есть
            BarCode barCodeEdit = mBarCodeHelper.getBarCodeByItem(item);
            if (barCodeEdit != null) {
                //cуществует
                //получим количество
                float val = mBarCodeHelper.getBarCodesQuantity(item);
                //выводим диалоговое окно редактирования, с тем количеством, которое есть в списке
                quantityChoiseListDialogValue(barCodeEdit, val);
                //обновить количество - см. в диалоге
                //получим ID BarCode
                barcodeId = barCodeEdit.getId().toString();
            } else {
                //нет в таблице
                //найден по ШК
                mDbBarcode = mBarCodeHelper.createBarcodeFromItem(item, barcodeValue, "", 0.0f);
                quantityChoiseListDialog(mDbBarcode);
            }
            updateUI();
        }
    }

    /**
     * Проверяет существует ли в БД отсканированный ШК
     *
     * @param barcode - ШК
     * @return - String - пустая строка - не существует, не пустая - значение ItemId
     */
    private boolean isBarcodeExist(String barcode) {
        if (mBarCodeHelper.getApiBarcodeItemId(barcode).equals("")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Возвращает количество ШК в списке
     */
    private int getBarcodesCount() {
        if (mAdapter != null) {
            return mAdapter.getItemCount();
        } else {
            return 0;
        }
    }

    /**
     * Доступность кнопки "Отправка данных"
     * @return
     */
    private boolean isSendDataEnabled() {
        return getBarcodesCount() > 0;
    }

    /**
     * обновление списка
     */
    private void updateUI() {
        BarCodeHelper barCodeHelper = BarCodeHelper.get(AcceptanceDetailsActivity.this);
        List<BarCode> barCodes = barCodeHelper.getBarCodes();

        // при работе с БД
        if (mAdapter == null) {
            mAdapter = new BarCodeAdapter(barCodes);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setBarCodes(barCodes);
            mAdapter.notifyDataSetChanged();
        }

        //есть нажатые галочки
        if (mCheck > 0) {
            setFabDelete();
        } else {
            setFabInsert();
        }

        // доступность кнопки "Отправка данных"
        mSendData.setEnabled(isSendDataEnabled());
    }

    /**
     * Обработка ответов от сервера (Передача ШК)
     *
     * @param response
     */
    @Override
    public void onAcceptanceResponse(ApiResponse response) {

        if (response == null) {
            new MaterialDialog.Builder(this)
                    .title(R.string.titleS)
                    .content("Нет ответа от сервера. Проверьте наличие сети")
                    .positiveText(R.string.agreeS)
                    .show();
            return;
        }

        if (response.message.equals("")) {//нет ошибок
            Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Выгрузка ШК прошла успешно", Snackbar.LENGTH_SHORT).show();
            returnToAcceptanceActivity();
        } else {
            //ошибки, алерт
            new MaterialDialog.Builder(this)
                    .title(R.string.titleS)
                    .content("code: "+ String.valueOf(response.code) + " data: " + String.valueOf(response.data) + " message: " + String.valueOf(response.message))
                    .positiveText(R.string.agreeS)
                    .show();
        }
    }




    /**
     * Holder
     */
    private class BarCodeHolder extends RecyclerView.ViewHolder implements RecyclerView.OnClickListener {
        public BarCode mBarCode;
        private TextView mTitleTextView;
        private TextView mMeasure;
        private TextView mQuantity;
        private TextView mTvBarCode;
        private AnimateCheckBox mCustomCheckbox;                // CustomCheck - круглый check


        public BarCodeHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            mTitleTextView = (TextView) itemView.findViewById(R.id.list_item_product_title_text_view);
            mMeasure = (TextView) itemView.findViewById(R.id.list_item_edizm_data_text_view);
            mQuantity = (TextView) itemView.findViewById(R.id.list_item_textViewQuantity);
            mCustomCheckbox = (AnimateCheckBox) itemView.findViewById(R.id.list_item_product_custom_checkbox);
            mTvBarCode = (TextView)itemView.findViewById(R.id.list_item_tv_barcode);
        }

        public void bindBarCode(BarCode BarCode) {
            mBarCode = BarCode;
            mTitleTextView.setText(mBarCode.getName());
            mQuantity.setText(String.valueOf(mBarCode.getQuantity()));
            mMeasure.setText(mBarCode.getMeasure());
            mTvBarCode.setText(mBarCode.getBarcode());
            mCustomCheckbox.setChecked(mBarCode.getChecked());
        }

        @Override
        public void onClick(View v) {
            quantityChoiseListDialog(mBarCode);
        }
    }

    /**
     * Адаптер
     */
    private class BarCodeAdapter extends RecyclerView.Adapter<BarCodeHolder> {
        private List<BarCode> mBarCodes;

        public BarCodeAdapter(List<BarCode> BarCodes) {
            mBarCodes = BarCodes;

        }

        @Override
        public BarCodeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(AcceptanceDetailsActivity.this);
            View view = layoutInflater.inflate(R.layout.list_item_product, parent, false);
            return new BarCodeHolder(view);
        }

        @Override
        public void onBindViewHolder(final BarCodeHolder holder, final int position) {
            final BarCode BarCode = mBarCodes.get(position);

            holder.mCustomCheckbox.setOnCheckedChangeListener(new AnimateCheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(View buttonView, boolean isChecked) {
                    if (isChecked) {
                        BarCode.setChecked(true);
                        mCheck++;
                        setFabDelete();

                    } else {
                        mCheck--;
                        if (mCheck<0) mCheck=0;
                        BarCode.setChecked(false);

                        if (mCheck == 0) {
                            setFabInsert();
                        }
                    }

                    mBarCodeHelper.updateBarCode(BarCode);
                }
            });

            holder.bindBarCode(BarCode);
        }

        @Override
        public int getItemCount() {
            return mBarCodes.size();
        }

        public void setBarCodes(List<BarCode> barCodes) {
            mBarCodes = barCodes;
        }

        public void clear(){
            final int size = mBarCodes.size();
            //очищаем таблицу Barcodes
            mBarCodeHelper.clearTable();
            //очищаем переменную
            mBarCodes.clear();
            //уведомляем рециклер
            notifyItemRangeRemoved(0, size);
        }
    }

    /**
     * настройка ФАБ, удалять или добавлять товар
     */
    private void setFabDelete() {
        fab.setImageDrawable(getDrawable(R.drawable.ic_action_fab_delete));
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4081")));
    }

    private void setFabInsert() {
        fab.setImageDrawable(getDrawable(R.drawable.ic_action_fab_plus));
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FC9A5F")));
    }

    /**
     * Подтверждение отправки данных
     */
    private void sendToServerDialog(String dialogTitle, String dialogMessage, String ok, String cancel, final String action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(dialogMessage).setTitle(dialogTitle);

        builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {

            }
        });
    }


    /**
     * различные Предупреждения
     *
     * @param dialog_title
     * @param dialog_message
     * @param ok
     * @param cancel
     * @param action
     */
    private void alertDialog(String dialog_title, String dialog_message, String ok, String cancel, final String action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(dialog_message)
                .setTitle(dialog_title);

        builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button

                if (action.equals(ACTION_EXIT)) {
                    mBarCodeHelper.clearTable();
                    finish();
                }

                if (action.equals(ACTION_DELETE)) {
                    mBarCodeHelper.deleteSelectedBarCodes();
                    mCheck = 0;
                    updateUI();
                }
            }
        });
        builder.setNegativeButton(cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Отмена удаления", Snackbar.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }



    /**
     * Диалог редактирования количества
     *
     * @param barCodeValue
     */
    private void quantityChoiseListDialog(BarCode barCodeValue) {
        final BarCode barCode = barCodeValue;
        new MaterialDialog.Builder(this)
                .title(R.string.qtitle)
                .content(R.string.qinput_content)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input(R.string.qinput_hint, R.string.qinput_prefill, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                // Do something
                                if (input.toString().equals("")){
                                    input="0";
                                }
                                barCode.setQuantity((Integer.parseInt(input.toString())));
                                mBarCodeHelper.updateBarCode(barCode);
                                updateUI();
                                isEditMode = false;
                            }
                        }

                )
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        isEditMode = false;
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        isEditMode = false;
                    }
                })
                .positiveText(R.string.qchoose)
                .negativeText(R.string.qnegative)
                .show();
    }

    /**
     * Диалог редактирования количества c указанием начального значения
     *
     * @param barCodeValue начальное значение
     */
    private void quantityChoiseListDialogValue(BarCode barCodeValue, float val) {
        final BarCode barCode = barCodeValue;
        new MaterialDialog.Builder(this)
                .title(R.string.qtitle)
                .content(R.string.qinput_content)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input("", String.valueOf(val), new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                try {
                                    barCode.setQuantity((Float.parseFloat(input.toString())));
                                    mBarCodeHelper.updateBarCode(barCode);
                                    updateUI();
                                    isEditMode = false;
                                }catch (Exception e){
                                    Log.e(TAG, e.getMessage());
                                }
                            }
                        }

                )
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        isEditMode = false;
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        isEditMode = false;
                    }
                })
                .positiveText(R.string.qchoose)
                .negativeText(R.string.qnegative)
                .show();
    }

    /**
     * диалог добавления нового товара
     */
    private void inputTextDialog() {

        //скроллинг в диалоге
     boolean wrapInScrollView = true;

     MaterialDialog dialog = new MaterialDialog.Builder(this)
         .title(R.string.title)
         .customView(R.layout.dialog_input_item, wrapInScrollView)
         .negativeText(R.string.disagree)
         .positiveText(R.string.agreeS)
         .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                        SendFromIputItemManual(dialog);
                    }
                })

          .show();


        // заполняем спинер
        View view =  dialog.getCustomView();
        Spinner spinner = (Spinner)view.findViewById(R.id.d_spinner);
        EditText editBarCode = view.findViewById(R.id.d_barcode);
        editBarCode.setText(getBarcode());

        // из базы данных
        mMeasures = mMeasureHelper.getMeasures();

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mMeasures);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

    }

    /**
     * Продолжение диалога ручного ввода товара, формируем классы и отправляем на сервер
     */
    private void SendFromIputItemManual(MaterialDialog dialog){

        //onClick
        EditText eName =  (EditText)dialog.findViewById(R.id.d_name);
        EditText eWidth =  (EditText)dialog.findViewById(R.id.d_width);
        EditText eHeight =  (EditText)dialog.findViewById(R.id.d_height);
        EditText eLength =  (EditText)dialog.findViewById(R.id.d_lenght);
        EditText eBarCode =  (EditText)dialog.findViewById(R.id.d_barcode);
        Spinner eMeasure =  (Spinner)dialog.findViewById(R.id.d_spinner);

        String name;
        float width=0;
        float height=0;
        float lenght=0;
        String barcode;
        String measure;

        try {
            name = eName.getText().toString();
            width = Float.parseFloat(eWidth.getText().toString());
            height = Float.parseFloat(eHeight.getText().toString());
            lenght = Float.parseFloat(eLength.getText().toString());
            measure = eMeasure.getSelectedItem().toString();
            barcode = eBarCode.getText().toString();

        }catch (Exception e) {
            Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Ошибка! В полях ширина, длина, высота - указывается число!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Подождите, идет обмен данными...", Snackbar.LENGTH_SHORT).show();

        //формируем классы
        //запрос на сервер, добавление номенклатуры
        Nomenclature nomenclature = new Nomenclature();
        nomenclature.setName(name);
        nomenclature.setBarCode(barcode);
        nomenclature.setHeight(height);
        nomenclature.setLength(lenght);
        nomenclature.setWidth(width);
        nomenclature.setMeasure(measure);

        //добавление в локальную временную таблицу
        BarCode barCode = new BarCode();
        barCode.setName(name);
        barCode.setQuantity(0);
        barCode.setMeasure(measure);
        barCode.setChecked(false);
        barCode.setBarcode(barcode);

        addNewNomenclature(nomenclature, barCode);

        updateUI();

    }



    /**
     * Диалог добавить товар или отмена добавления
     * Три кнопки
     * Выбрать из списка
     * Создать новую
     * Отмена
     */
    private void addOrCancelDialog(String sTitle, final boolean isScan) {
        //скроллинг в диалоге
        boolean wrapInScrollView = false;

       final MaterialDialog  dialog = new MaterialDialog.Builder(this)
                .title(sTitle)
                .customView(R.layout.dialog_button_not_in_list, wrapInScrollView)
                .show();

        //onClick
        Button dSelect = (Button) dialog.findViewById(R.id.btn_select);
        dSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Выбрать из списка
                Intent i = new Intent(AcceptanceDetailsActivity.this, CategoryActivity.class);
                i.putExtra("activity", "AcceptanceDetailsActivity");
                i.putExtra("isScan", isScan);
                startActivityForResult(i, 1);
                dialog.cancel();
            }
        });

        Button dNew = (Button) dialog.findViewById(R.id.btn_new);
        dNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //создать
                inputTextDialog();
                dialog.cancel();

            }
        });
        Button dCancel = (Button) dialog.findViewById(R.id.btn_cancel);
        dCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //отмена
                dialog.cancel();
            }
        });


    }

    /**
     * Retrofit добавление товара
     * @param nomenclature
     * @param barCode
     */
    private void addNewNomenclature(Nomenclature nomenclature, BarCode barCode) {
        addNewItem(nomenclature, barCode);
    }

    /**
     * Retrofit  добавление товара
     * @param nomenclature
     * @param barCode
     */
    private void addNewItem(Nomenclature nomenclature, final BarCode barCode) {

        CoreApplication.getApi().addNewNomenclature(nomenclature).enqueue(new Callback<ItemResponse>() {
            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {

                if (response.isSuccessful()) {
                    //ок
                    if (mAcceptanceListener != null) {

                        String uUid = response.body().data;

                        try {
                            //Устанавливаем UUID в наш баркод
                            barCode.setItemId(uUid);
                            //записываем его в базу
                            mBarCodeHelper.addBarcode(barCode);
                            updateUI();
                        }catch (Exception e){
                            Log.e(TAG, e.getMessage());
                        }

                        mAcceptanceListener.onAcceptanceItem(response.body());
                    }

                } else {
                    // ошибка
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceItem(response.body());
                    }
                }

            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {

                String error = t.getMessage();
                Log.e(TAG, error);

                try {
                    ItemResponse mResult = new ItemResponse();
                    mResult.code = String.valueOf(call.execute().code());
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceItem(mResult);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

            }
        });
    }


    /**
     * Listener ответа добавления товара
     */
    @Override
    public void onAcceptanceItem(ItemResponse response) {

        if (response == null) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.titleS)
                    .content("Нет ответа от сервера. Проверьте наличие сети")
                    .positiveText(R.string.agreeS)
                    .show();
            return;
        }

        if (response.message.equals("")) {//нет ошибок
            Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Выгрузка товара прошла успешно", Snackbar.LENGTH_SHORT).show();
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
     * Retrofit получение единиц измерения
     */
    public void getMeasures(){

        CoreApplication.getApi().getMeasures().enqueue(new Callback<MeasureResponse>() {
            @Override
            public void onResponse(Call<MeasureResponse> call, Response<MeasureResponse> response) {


                if (response.isSuccessful()) {
                    //ок
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceMeasure(response.body());
                        mMeasures = mMeasureHelper.getMeasures();
                    }

                } else {
                    // ошибка
                    if (mAcceptanceListener != null) {
                        mAcceptanceListener.onAcceptanceMeasure(response.body());
                    }
                }

            }

            @Override
            public void onFailure(Call<MeasureResponse> call, Throwable t) {
                String error = t.getMessage();
                Log.d(TAG, error);
            }
        });

    }




    /**
     * Listener единиц измерения
     * @param response
     */
   public void onAcceptanceMeasure(MeasureResponse response){

       if (response == null) {
           MaterialDialog dialog = new MaterialDialog.Builder(this)
                   .title(R.string.titleS)
                   .content("Нет ответа от сервера. Проверьте наличие сети")
                   .positiveText(R.string.agreeS)
                   .show();
           return;
       }

       if (response.message.equals("")) {//нет ошибок
           Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Загрузка единиц измерения", Snackbar.LENGTH_SHORT).show();
           List<Measure> measures;
           measures = response.data;

           for (Measure measure: measures){
               // добавить в базу
               mMeasureHelper.InsertMeasure(measure.getName(), measure.getCode());
           }

       } else {
           //ошибки, алерт
           new MaterialDialog.Builder(this)
                   .title(R.string.titleS)
                   .content("code: "+ String.valueOf(response.code) + " data: " + String.valueOf(response.data) + " message: " + String.valueOf(response.message))
                   .positiveText(R.string.agreeS)
                   .show();
       }
   }

    @Override
    public void onAcceptanceStock(StockInfoResponse response) {

    }


    /**
     * Результат выбора CategoryActivity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //Id выбранного товара
            String ItemId = data.getStringExtra("id_items");
            String barcodeValue;
            String barCodeId;
            boolean isScan;
            isScan = data.getBooleanExtra("isScan", false);
            //создаем Item с его подчиненными BarCodes
            Item item = mItemHelper.createItemAndBarcodes(ItemId);

            // выбранный товар
            mSelectedItem = item;

            BarCode tmpBarcode = new BarCode();
                barcodeValue="";
                barCodeId = tmpBarcode.getId().toString();

            //сканер
            if (isScan) {
                //проверим, есть ли такой код
                if (!mItemHelper.isApiBarcodes(getBarcode())) {
                    // если ШК не привязан к товару - необходимо привязать его
                    barcodeValue=getBarcode();
                    linkBarcodeDialog(getString(R.string.dialog_link_barcode_text), getBarcode(), item.getName(), item.getId());
                }
            }

            barcodeValidateManual(barCodeId, barcodeValue, item);
            updateUI();

        }
    }

    /**
     * Контроль ввода из списка товаров, выбираемых вручную
     *
     * @param barcodeValue
     * @param item
     */

    private void barcodeValidateManual(String barCodeId, String barcodeValue, Item item) {

        if (item == null) {
            // номенклатура не привязана к ШК
            // предлагаем выбрать из CategoryActivity
            addOrCancelDialog("Номенклатура не найдена", false);
        } else {
            //получим BarCode из базы, если он там есть
            BarCode barCodeEdit = mBarCodeHelper.getBarCodeByItem(item);
            if (barCodeEdit != null) {
                //есть в таблице
                //получим количество
                float val = mBarCodeHelper.getBarCodesQuantity(item);
                //выводим диалоговое окно редактирования, с тем количеством, которое есть в списке
                quantityChoiseListDialogValue(barCodeEdit, val);
                //обновить количество - см. в диалоге
            } else {
                //нет в таблице
                mDbBarcode = mBarCodeHelper.createBarcodeFromItem(item, barcodeValue, barCodeId, 0.0f);
                quantityChoiseListDialog(mDbBarcode);
            }
            updateUI();
        }
    }

    /**
     * Открывает активность ModulesActivity
     */
    private void startAcceptanceActivity() {
        Intent intent = new Intent(this, AcceptanceActivity.class);
        startActivity(intent);
    }

    /**
     * Диалог - привязать штрих-код к товару
     */
    private void linkBarcodeDialog(String sTitle, final String barcodeValue, String itemName, final String itemId) {
        //скроллинг в диалоге
        boolean wrapInScrollView = false;

        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(sTitle)
                .customView(R.layout.dialog_link_barcode, wrapInScrollView)
                .positiveText("Привязать")
                .negativeText("Отмена")
                .onPositive(new MaterialDialog.SingleButtonCallback(){
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        // вызываем метод API POST /api/Nomenclature/{id} - привязка ШК к товару
                        bindBarcodeToItem(itemId, barcodeValue);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback(){
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        // скрываем модальное окно
                        dialog.dismiss();
                    }
                })
                .show();

        // ШК
        TextView barcodeText = (TextView)dialog.findViewById(R.id.dialog_link_barcode_text);
        barcodeText.setText(barcodeText.getText() + " " + barcodeValue);

        // название товара
        TextView itemText = (TextView)dialog.findViewById(R.id.dialog_link_nomenclature_text);
        itemText.setText(itemText.getText() + " " + itemName);
    }

    /**
     * Привязка ШК к товрау на сервере
     */
    private void bindBarcodeToItem(String itemId, String barcodeValue) {
        if (!itemId.isEmpty()) {
            mItemHelper.sendBarcodeToServer(itemId, barcodeValue);
        }
    }

    /**
     * Метод вызывается при выполнении привязки ШК к товару на сервере
     * @param response
     */
    @Override
    public void onNomenclatureResponse(ItemResponse response) {
        if (response != null) {
            if (response.getCode().equals("200") && response.getMessage().isEmpty()) {
                updateUI();
            } else {
                displayErrorDialog("Ошибка привязки ШК", response.getMessage());
            }
        }
    }

    /**
     * Отображение диалогового окна при возникновении ошибки соединения с WiFi
     */
    private void displayErrorDialog(String title, String message) {
        DialogFragment dialog = new MessageDialog();
        Bundle args = new Bundle();
        args.putString(MessageDialog.ARG_TITLE, title);
        args.putString(MessageDialog.ARG_MESSAGE, message);
        dialog.setArguments(args);
        dialog.setTargetFragment(dialog, YES_NO_CALL);
        dialog.show(getFragmentManager(), "tag");
    }
}