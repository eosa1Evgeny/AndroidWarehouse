package ru.cityprint.arm.warehouse.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import rs.core.hw.Barcode;
import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.barcode.CoreApplication;
import ru.cityprint.arm.warehouse.barcode.LocalCoreDataReceiver;
import ru.cityprint.arm.warehouse.data.Item;
import ru.cityprint.arm.warehouse.data.ItemHelper;
import ru.cityprint.arm.warehouse.utils.SoundHelper;

public class StockActivity extends AppCompatActivity implements LocalCoreDataReceiver, SearchView.OnQueryTextListener{

    private SearchAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ItemHelper mItemHelper;
    private ImageButton mImageButtonClose;
    private TextView mTextViewTitle;
    private String mBarcode;                    // отсканированный ШК
    private SoundHelper mSoundHelper;           // работа со звуковым оповещением при сканировании
    private SearchView mSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_inv);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        // инициализация RecyclerView
        mRecyclerView = findViewById(R.id.search_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mTextViewTitle = findViewById(R.id.textViewTitle);
        mTextViewTitle.setText("Остатки");

        mImageButtonClose = findViewById(R.id.image_button_close);
        mImageButtonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // инициализация объекта SoundHelper - звук при сканировании
        mSoundHelper = new SoundHelper(this);

        start();

        //получение и настройка компонента поиска
        mSearchView = findViewById(R.id.search);
        mSearchView.setActivated(true);
        mSearchView.setQueryHint("Введите строку поиска");
        mSearchView.onActionViewExpanded();
        mSearchView.setIconified(false);
        mSearchView.clearFocus();
        //слушатель
        mSearchView.setOnQueryTextListener(this);
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
     * Обработчики SearchView
     * @param query
     * @return
     */

    @Override
    public boolean onQueryTextSubmit(String query) {
        //нажатие кнопки поиска

        List<Item> itemList = mItemHelper.findFullWord(query);
        if (itemList!=null) {
            updateUI(itemList);
        }else {
            start();
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //изменение текста
        List<Item> itemList = mItemHelper.findFullWord(newText);
        if (itemList!=null) {
            updateUI(itemList);
        }

        return false;
    }

    /**
     *
     * Первоначальное заполнение RecyclerView номенклатурой
     **/

    private void start(){
        mItemHelper = ItemHelper.get(this);
        List<Item> itemList = mItemHelper.getAllItemFromDBnotCategory();

        if (itemList!=null) {
            updateUI(itemList);
        }

    }

    /**
     * обновление списка
     */
    private void updateUI(List<Item> itemList) {

        // при работе с БД
        if (mAdapter == null) {
            mAdapter = new SearchAdapter(itemList);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setSearcList(itemList);
            mAdapter.notifyDataSetChanged();
        }

    }

    public void startStockInfo(Item item){
        Intent i = new Intent(this, StockInfoActivity.class);
        i.putExtra("id_item", item.getId());
        startActivity(i);
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

                mSearchView.setQuery(barcodeValue, false);
                mSearchView.clearFocus();

                // проигрываем звуковое оповещение (успешное)
                mSoundHelper.playSuccessSound();
            } else {
                mSoundHelper.playErrorSound();
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
     * Holder
     */
    private class SearchHolder extends RecyclerView.ViewHolder implements RecyclerView.OnClickListener {
        public Item mItem;
        private TextView mTitleTextView;


        public SearchHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.item_name);
        }

        public void bindSearch(Item Item) {
            mItem = Item;
            mTitleTextView.setText(mItem.getName());
        }

        @Override
        public void onClick(View v) {
            startStockInfo(mItem);
        }
    }



    /**
     * Адаптер
     */
    private class SearchAdapter extends RecyclerView.Adapter<SearchHolder> {
        private List<Item> items;

        public SearchAdapter(List<Item> itemList) {
            this.items = itemList;

        }

        @Override
        public SearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(StockActivity.this);
            View view = layoutInflater.inflate(R.layout.list_item_product_from_api, parent, false);
            return new SearchHolder(view);
        }

        @Override
        public void onBindViewHolder(final SearchHolder holder, final int position) {
            final Item item = items.get(position);
            holder.bindSearch(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void setSearcList(List<Item> itemList) {
            items = itemList;
        }

    }




}

