package ru.cityprint.arm.warehouse.ui;

import android.content.DialogInterface;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.List;

import rs.core.hw.Barcode;
import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.barcode.BarcodeHelper;
import ru.cityprint.arm.warehouse.barcode.CoreApplication;
import ru.cityprint.arm.warehouse.barcode.LocalCoreDataReceiver;
import ru.cityprint.arm.warehouse.data.BarCode;
import ru.cityprint.arm.warehouse.data.BarCodeHelper;
import ru.cityprint.arm.warehouse.data.InventoryListener;
import ru.cityprint.arm.warehouse.data.InventoryResponse;
import ru.cityprint.arm.warehouse.data.Item;
import ru.cityprint.arm.warehouse.data.ItemHelper;
import ru.cityprint.arm.warehouse.utils.SoundHelper;

/**
 * Инвентаризация
 */

public class InventoryActivity extends AppCompatActivity implements LocalCoreDataReceiver, InventoryListener {
    private ImageButton mImageButtonClose;
    private ImageButton mImageButtonSend;
    private String mBarcode;                            // отсканированный ШК
    private SoundHelper soundHelper;                    // работа со звуковым оповещением при сканировании
    private ItemHelper mItemHelper;                     // работа с таблицей Items
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mAdapter;
    private BarCodeHelper mBarCodeHelper;
    private List<Item> mItems;
    private FloatingActionButton mFloatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_inv);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        // инициализация RecyclerView
        mRecyclerView = findViewById(R.id.inv_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mItems = new ArrayList<>();
        mBarCodeHelper = BarCodeHelper.get(this);

        mImageButtonClose = findViewById(R.id.imageButton_close_inv);
        mImageButtonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //закрываем
                finish();
            }
        });

        // кнопка "Отправить данные"
        mImageButtonSend = findViewById(R.id.imageButton_send_inv);
        mImageButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //отправляем на сервер отмеченные галочками
                sendToService();
            }
        });

        mFloatingActionButton = findViewById(R.id.fab_delete);
        setDeleteVisible();
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItems != null) {
                    List<Item> tmpArray = new ArrayList<>();
                    for (Item rItem : mItems) {
                        if (!rItem.isSetChecked()) {
                            tmpArray.add(rItem);
                        }
                    }
                    mItems = tmpArray;
                    tmpArray = null;
                    updateUI();
                }
            }
        });

        // инициализация объекта SoundHelper - звук при сканировании
        soundHelper = new SoundHelper(this);

        //работа с Item
        mItemHelper = ItemHelper.get(this);

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

    /**
     * проверка и вывод в RV
     *
     * @param barcodeValue - значение ШК
     */
    private void barcodeValidate(String barcodeValue) {

        Item item = mItemHelper.getItemByBarcode(barcodeValue);

        if (item == null) {
            // номенклатура не привязана к ШК
        } else {
            //вывести в RecyclerView Item.getName() + barcodeValue;
            item.setInventoryBarCode(barcodeValue);
            mItems.add(item);
            updateUI();
        }
    }

    /**
     * обновление списка
     */
    private void updateUI() {
        if (mAdapter == null) {
            mAdapter = new RecyclerViewAdapter(mItems);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setInfoList(mItems);
            mAdapter.notifyDataSetChanged();
        }
        // доступность кнопки "Отправка данных"
        setDeleteVisible();
    }

    /**
     * Подтверждение перед отправкой данных на инвентаризацию
     * @param dialogTitle
     * @param dialogMessage
     * @param ok
     * @param cancel
     */
    private void sendToInventoryDialog(String dialogTitle, String dialogMessage, String ok, String cancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(dialogMessage).setTitle(dialogTitle);
        builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                sendToService();
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

    @Override
    public void onInventoryResponse(InventoryResponse response) {

        if (response == null) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.titleS)
                    .content("Список пустой, или нет соединения с сервером. Проверьте наличие сети")
                    .positiveText(R.string.agreeS)
                    .show();
            return;
        }

        if (response.message.equals("")) {//нет ошибок
            if (mAdapter!=null) {
                //очищаем список
                mAdapter.clear();
                mFloatingActionButton.hide();
            }
            Snackbar.make(findViewById(R.id.inventoryCoordinatorLayout), "Передача на инвентаризацию прошла успешно", Snackbar.LENGTH_SHORT).show();
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
     * Адаптер
     */
    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.InfoHolder> {
        private List<Item> RvItems;

        /**
         * Holder
         */
        public class InfoHolder extends RecyclerView.ViewHolder implements RecyclerView.OnClickListener {
            public Item mItem;
            public TextView mTitleTextView;
            public TextView mBarcodeTextView;
            private TextView mMeasure;
            private TextView mQuantity;
            private AnimateCheckBox mCustomCheckbox;                // CustomCheck - круглый check
            private String mBarcodeValue;


            public InfoHolder(View itemView) {
                super(itemView);
                mTitleTextView = itemView.findViewById(R.id.list_item_product_title_text_view);
                mMeasure = (TextView) itemView.findViewById(R.id.list_item_inv_measure);
                mQuantity = (TextView) itemView.findViewById(R.id.list_item_inv_qty);
                mBarcodeTextView = itemView.findViewById(R.id.list_item_inv_label);
                mCustomCheckbox = itemView.findViewById(R.id.list_item_invent_barcode);
                itemView.setOnClickListener(this);
            }

            public void bindBarCode(Item item) {

                // определяем BarCode по item
                BarCode barCode = mBarCodeHelper.getBarCodeByItem(item);

                // отсканированный ШК
                mBarcodeValue = item.getInventoryBarCode();

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

                if (barCode != null) {
                    mTitleTextView.setText(barCode.getName());
                    mMeasure.setText(mItem.getMeasure());
                    mCustomCheckbox.setChecked(barCode.getChecked());
                } else {
                    mItem = item;
                    mTitleTextView.setText(mItem.getName());
                    mMeasure.setText(mItem. getMeasure());
                    mCustomCheckbox.setChecked(mItem.isSetChecked());
                }

                mBarcodeTextView.setText(mBarcodeValue);
            }

            @Override
            public void onClick(View v) {

            }
        }

        /**
         * Конструктор
         *
         * @param itemList
         */

        public RecyclerViewAdapter(List<Item> itemList) {
            this.RvItems = itemList;
        }

            @Override
            public InfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                LayoutInflater layoutInflater = LayoutInflater.from(InventoryActivity.this);
                View view = layoutInflater.inflate(R.layout.list_item_inventory, parent, false);
                return new InfoHolder(view);
            }

            @Override
            public void onBindViewHolder(final InfoHolder holder, final int position) {
                final Item item = RvItems.get(position);

                TextView titleTextView = holder.mTitleTextView;
                titleTextView.setText(item.getName());
                TextView barCodeTextView = holder.mBarcodeTextView;
                barCodeTextView.setText(item.getInventoryBarCode());

                holder.mCustomCheckbox.setOnCheckedChangeListener(new AnimateCheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(View buttonView, boolean isChecked) {
                        if (isChecked) {
                            item.setSetChecked(true);
                        } else {
                            item.setSetChecked(false);
                        }
                    }
                });

                holder.mCustomCheckbox.setChecked(item.isSetChecked());

                holder.bindBarCode(item);
            }

            @Override
            public int getItemCount() {
                return RvItems.size();
            }

            public void setInfoList(List<Item> itemList) {
                RvItems = itemList;
            }

            public void clear(){
                final int size = RvItems.size();
                //очищаем таблицу Barcodes
                mBarCodeHelper.clearTable();
                //очищаем переменную
                RvItems.clear();
                //уведомляем рециклер
                notifyItemRangeRemoved(0, size);
            }
    }

    /**
     * отправка на сервер/сервис товаров
     */
    private void sendToService() {

        if (mItems == null) {
            return;
        }

        //выбираем, отмеченные галочками
        List<BarCode> barcodesList = new ArrayList<>();

        for (Item item : mItems) {
            BarCode barcode = new BarCode();
            barcode.setBarcode(item.getInventoryBarCode());
            barcode.setItemId(item.getId());
            barcode.setName(item.getName());
            barcodesList.add(barcode);

            if (item.isSetChecked()) {
                //отмечен
                Log.d("InventoryActivity", item.getName() + "  отмечено галочкой");
            }
        }

        if (barcodesList.size()==0){

            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.titleS)
                    .content("Список пустой, или нет отмеченного галочкой товара.")
                    .positiveText(R.string.agreeS)
                    .show();
            return;
        }

        mItemHelper.sendBarcodesToInventory(barcodesList);
    }


    /**
     * Данные со сканера
     *
     * @param data
     * @return
     */
    @Override
    public boolean onReceivedData(Object data) {
        if (data != null && data instanceof Barcode) {
            final String barcodeValue = ((Barcode) data).value;
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
     * Доступность кнопки "Удаление"
     * @return
     */
    private boolean isDeleteVisible() {
        return getBarcodesCount() > 0;
    }

    /**
     * Инициализация глобальной переменной mBarcode (ШК)
     */
    private void setBarcode(String barcodeValue) {
        mBarcode = barcodeValue;
    }

    /**
     * Устанавливает видимость кнопки "Удаление"
     */
    private void setDeleteVisible() {
        mFloatingActionButton.setVisibility(isDeleteVisible() ? View.VISIBLE : View.GONE);
    }

    /**
     * Доступность кнопки "Отправка данных"
     * @return
     */
    private boolean isSendDataEnabled() {
        return getBarcodesCount() > 0;
    }

}
