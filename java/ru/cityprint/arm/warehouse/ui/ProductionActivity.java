package ru.cityprint.arm.warehouse.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.hanks.library.AnimateCheckBox;

import java.math.BigDecimal;
import java.util.List;

import rs.core.hw.Barcode;
import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.barcode.BarcodeHelper;
import ru.cityprint.arm.warehouse.barcode.CoreApplication;
import ru.cityprint.arm.warehouse.barcode.LocalCoreDataReceiver;
import ru.cityprint.arm.warehouse.data.BarCode;
import ru.cityprint.arm.warehouse.data.BarCodeHelper;
import ru.cityprint.arm.warehouse.data.Item;
import ru.cityprint.arm.warehouse.data.ItemHelper;
import ru.cityprint.arm.warehouse.data.ManufactureListener;
import ru.cityprint.arm.warehouse.data.ManufactureResponse;
import ru.cityprint.arm.warehouse.utils.SoundHelper;

/**
 * Список отсканированных ШК - передача в производство
 */
public class ProductionActivity extends AppCompatActivity implements LocalCoreDataReceiver, ManufactureListener {
    private final String TAG = getClass().getSimpleName();
    private FloatingActionButton fab;
    private TextView mTitle;                            // заголовок
    private RecyclerView mRecyclerView;                 // список отсканированных ШК
    private BarCodeAdapter mAdapter;                    // адаптер для списка ШК
    private ImageButton mClose;
    private ImageButton mSendData;                      // выделить все (для удаления)
    private int mCheck = 0;                             // количество выделенных элементов (галочка)
    private BarCodeHelper mBarCodeHelper;               // работа с таблицей Barcodes
    private ItemHelper mItemHelper;                     // работа с таблицей Items
    private String mBarcode;                            // отсканированный ШК
    private BarCode mDbBarcode;                         // ШК из базы
    private SoundHelper soundHelper;                    // работа со звуковым оповещением при сканировании
    private ManufactureListener mManufactureListener;
    private Item mSelectedItem;                         // выбранная номенклатура

    // константы
    private final static String ACTION_EXIT = "exit";
    private final static String ACTION_DELETE = "delete";

    /**
     * Обработка нажатия кнопки "Назад"
     */
    @Override
    public void onBackPressed() {
        if (getBarcodesCount() > 0) {
            alertDialog("Уверены, что хотите выйти?", "Содержимое будет очищено!", "Да", "Нет", ACTION_EXIT);
        } else {
            startModulesActivity();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_production);

        // заголовок
        mTitle = findViewById(R.id.productionTitle);
        mTitle.setText(R.string.production);

        mManufactureListener = (ManufactureListener) this;

        // Singleton BarCodeHelper
        mBarCodeHelper = BarCodeHelper.get(this);

        // Singleton ItemHelper
        mItemHelper = ItemHelper.get(this);

        // инициализация RecyclerView
        mRecyclerView = findViewById(R.id.production_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // кнопка "Выйти"
        mClose = findViewById(R.id.productionButtonClose);
        mClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBarcodesCount() > 0) {
                    alertDialog("Уверены, что хотите выйти?", "Содержимое будет очищено!", "Да", "Нет", ACTION_EXIT);
                } else {
                    startModulesActivity();
                }
            }
        });

        // кнопка "Отправить данные"
        mSendData = findViewById(R.id.productionButtonSendData);
        mSendData.setEnabled(isSendDataEnabled());
        mSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // подтверждение отправки данных
                sendToProductionDialog("Передача в производство", "Передать в производство?", "передать", "отмена");
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
                    singleChoiseListDialog();
                }
            }
        });

        // инициализация объекта SoundHelper - звук при сканировании
        soundHelper = new SoundHelper(this);

        // обновление UI
        updateUI();
    }//onCreate


    // отправка данных в производство
    private void sendToProduction() {
        Snackbar.make(findViewById(R.id.productionCoordinatorLayout), "Подождите, идет обмен данными...", Snackbar.LENGTH_SHORT).show();
        mItemHelper.sendQuantityAndBarcodesToProduction();
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
     * @param data - отсканированное значение
     * @return - Возвращает boolean
     */
    @Override
    public boolean onReceivedData(Object data) {
        if (data != null && data instanceof Barcode) {
            String barcodeValue = ((Barcode) data).value;
            if (!barcodeValue.isEmpty()) {
                // инициализируем переменную mBarcode
                setBarcode(barcodeValue);

                if (!mBarcode.isEmpty()) {
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

        // если ШК не найден - выбор из списка
        if (item == null) {
            // номенклатура не привязана к ШК
            // предлагаем выбрать из CategoryActivity
            addOrCancelDialog(true);
        } else {
            // проверяем является ли ШК - меткой
            if (BarcodeHelper.isBarcodeLabel(barcodeValue)) {
               // добавляенм ШК (метку) в таблицу Barcodes
                mDbBarcode = mBarCodeHelper.createBarcodeFromLabel(item, barcodeValue);
            } else {
                //получим BarCode из базы, если он там есть
                BarCode barCodeEdit = mBarCodeHelper.getBarCodeByItem(item);
                if (barCodeEdit != null) {
                    //cуществует
                    //получим количество
                    //обновить количество
                    mDbBarcode = mBarCodeHelper.createBarcodeFromItem(item, barcodeValue, "", 1.0f);
                    mBarCodeHelper.updateBarCode(barCodeEdit);

                } else {
                    //нет в таблице
                    //найден по ШК
                    mDbBarcode = mBarCodeHelper.createBarcodeFromItem(item, barcodeValue, "", 1.0f);
                    mBarCodeHelper.updateBarCode(mDbBarcode);
                }
            }
            // обнволяем UI
            updateUI();
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
     * обновление списка
     */
    private void updateUI() {

        BarCodeHelper barCodeHelper = BarCodeHelper.get(ProductionActivity.this);
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
     * Обработка ответов от сервера (Передача в производство)
     * @param response - ManufactureResponse
     */
    @Override
    public void onManufactureResponse(ManufactureResponse response) {
        if (response == null) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.titleS)
                    .content("Нет ответа от сервера. Проверьте наличие сети")
                    .positiveText(R.string.agreeS)
                    .show();
            return;
        }

        if (response.message.equals("")) {//нет ошибок
            //Очищаем список после отправки
            mAdapter.clear();
            Snackbar.make(findViewById(R.id.productionCoordinatorLayout), "Передача данных прошла успешно", Snackbar.LENGTH_SHORT).show();
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
     * Holder
     */
    private class BarCodeHolder extends RecyclerView.ViewHolder implements RecyclerView.OnClickListener {
        public BarCode mBarCode;
        private TextView mTitleTextView;
        private TextView mMeasure;
        private TextView mQuantity;
        private TextView mTvBarCode;
        private AnimateCheckBox mCustomCheckbox;                // CustomCheck - круглый check
        private String mBarcodeValue;

        public BarCodeHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            mTitleTextView = (TextView) itemView.findViewById(R.id.list_item_product_title_text_view);
            mMeasure = (TextView) itemView.findViewById(R.id.list_item_edizm_data_text_view);
            mQuantity = (TextView) itemView.findViewById(R.id.list_item_textViewQuantity);
            mCustomCheckbox = (AnimateCheckBox) itemView.findViewById(R.id.list_item_product_custom_checkbox);
            mTvBarCode = (TextView)itemView.findViewById(R.id.list_item_tv_barcode);

        }

        public void bindBarCode(BarCode barCode) {

            // отсканированный ШК
            mBarcodeValue = barCode.getBarcode();

            // если ШК не пустой и является меткой - подгружаем количество (из остатков)
            if (mBarcodeValue != null && !mBarcodeValue.equals("") && BarcodeHelper.isBarcodeLabel(mBarcodeValue)) {
                //метка, ищем по ней в StockInfo qty - его и отображаешь;
                double qty =  mItemHelper.getQtyFromLabel(mBarcodeValue);

                BigDecimal qtyRound = new BigDecimal(qty);
                qtyRound = qtyRound.setScale(2, BigDecimal.ROUND_HALF_UP);

                mQuantity.setText(String.valueOf(qtyRound));
                // для отображения в списке метки - отбрасываем префикс (77-)
                mBarcodeValue = BarcodeHelper.getBarcodeLabel(mBarcodeValue);
            }else{
                //не метка, отображаешь ВСЕГДА 1 шт
                mQuantity.setText(String.valueOf(1d));
            }

            mBarCode = barCode;
            mTitleTextView.setText(mBarCode.getName());
            mMeasure.setText(mBarCode.getMeasure());
            mCustomCheckbox.setChecked(mBarCode.getChecked());
            mTvBarCode.setText(mBarcodeValue);
        }

        @Override
        public void onClick(View v) {
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
            LayoutInflater layoutInflater = LayoutInflater.from(ProductionActivity.this);
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
                        mBarCodeHelper.updateBarCode(BarCode);
                    } else {
                        mCheck--;
                        if (mCheck<0) mCheck=0;
                        BarCode.setChecked(false);
                        mBarCodeHelper.updateBarCode(BarCode);

                        if (mCheck == 0) {
                            setFabInsert();
                        }
                    }
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
                    Intent i = new Intent(ProductionActivity.this, ModulesActivity.class);
                    startActivity(i);
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
                Snackbar.make(findViewById(R.id.productionCoordinatorLayout), "Отмена удаления", Snackbar.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Подтверждение перед отправкой данных в производство
     * @param dialogTitle
     * @param dialogMessage
     * @param ok
     * @param cancel
     */
    private void sendToProductionDialog(String dialogTitle, String dialogMessage, String ok, String cancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(dialogMessage).setTitle(dialogTitle);
        builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                sendToProduction();
            }
        });
        builder.setNegativeButton(cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * radioButton диалог
     */
    private void singleChoiseListDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.title)
                .items(R.array.production_select_names)
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (which == 0) {
                            Intent intent = new Intent(ProductionActivity.this, CategoryActivity.class);
                            intent.putExtra("activity", "ProductionActivity");
                            startActivityForResult(intent, 1);
                        }
/*                        if (which == 1) {
                            inputTextDialog();
                        }*/
                        return true;
                    }
                })
                .positiveText(R.string.choose)
                .negativeText(R.string.negative)
                .show();
    }


    /**
     * Диалог - если ШК отсутствует - выбор из списка
     */
    private void addOrCancelDialog(final boolean isScan) {
        //скроллинг в диалоге
        boolean wrapInScrollView = false;

        final MaterialDialog  dialog = new MaterialDialog.Builder(this)
                .title("Номенклатура не найдена")
                .customView(R.layout.dialog_button_not_in_list_production, wrapInScrollView)
                .show();

        //onClick
        Button dSelect = (Button) dialog.findViewById(R.id.btn_select_production);
        dSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Выбрать из списка
                Intent i = new Intent(ProductionActivity.this, CategoryActivity.class);
                i.putExtra("activity", "ProductionActivity");
                i.putExtra("isScan", isScan);
                startActivityForResult(i, 1);
                dialog.cancel();
            }
        });

        Button dCancel = (Button) dialog.findViewById(R.id.btn_cancel_production);
        dCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //отмена
                dialog.cancel();
            }
        });
    }

    /**
     * Результат выбора CategoryActivity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //Id выбранного товара
            String ItemId = data.getStringExtra("id_items");
            String barcodeValue = "";
            boolean isScan;
            isScan = data.getBooleanExtra("isScan", false);

            //создаем Item с его подчиненными BarCodes
            Item item = mItemHelper.createItemAndBarcodes(ItemId);

            // выбранный товар
            mSelectedItem = item;

            if (isScan) {
                //проверим, есть ли такой код
                if (!mItemHelper.isApiBarcodes(getBarcode())) {
                    barcodeValue = getBarcode();
                }
            }

            barcodeValidateManual(barcodeValue, item);
            updateUI();
        }
    }

    /**
     * Контроль ввода из списка товаров, выбираемых вручную
     *
     * @param barcodeValue
     * @param item
     */

    private void barcodeValidateManual(String barcodeValue, Item item) {

        if (item == null) {
            // номенклатура не привязана к ШК
            // предлагаем выбрать из CategoryActivity
            addOrCancelDialog(false);
        } else {
            //получим BarCode из базы, если он там есть
            BarCode barCodeEdit = mBarCodeHelper.getBarCodeByItem(item);
            if (barCodeEdit != null) {
                //есть в таблице
                //получим количество
                mDbBarcode = mBarCodeHelper.createBarcodeFromItem(item, barcodeValue, "", 1.0f);
                mBarCodeHelper.updateBarCode(barCodeEdit);

            } else {
                //нет в таблице
                mDbBarcode = mBarCodeHelper.createBarcodeFromItem(item, barcodeValue, "", 1.0f);
                mBarCodeHelper.updateBarCode(mDbBarcode);
            }
            updateUI();
        }
    }


    /**
     * Открывает активность ModulesActivity
     */
    private void startModulesActivity() {
        // переходим на активность "Модули"
        Intent i = new Intent(ProductionActivity.this, ModulesActivity.class);
        startActivity(i);
    }



}