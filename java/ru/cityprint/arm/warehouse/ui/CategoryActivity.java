package ru.cityprint.arm.warehouse.ui;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.data.Item;
import ru.cityprint.arm.warehouse.data.ItemHelper;
import ru.cityprint.arm.warehouse.network.WifiHelper;

/**
 * Список категорий, товаров
 */
public class CategoryActivity extends AppCompatActivity {
    private static final String MESSAGE_TITLE = "Информация";
    private static final String MESSAGE_WIFI_ERROR_TEXT = "К сожалению, в настоящий момент отсутствует соединение с WiFi. Проверьте, пожалуйста настройки WiFi.";
    private static final int YES_NO_CALL = 100;

    private List<Item> mCategories;
    private RecyclerView rvCategories;
    private TextView mTitle;
    private ImageButton mButtonBack;
    private String mParentId;
    private ItemHelper mItemHelper = ItemHelper.get(this);
    private Stack<Item> mStack = new Stack<>();
    private Intent mCallingIntent; // активность из которой вызывана CategoryActivity
    private boolean isScan;  //вызвано при сканировании

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent();
        setResult(RESULT_CANCELED, i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        // определяем активность, из которой произошел вызов
        Intent intent = getIntent();
        String activity = intent.getStringExtra("activity");
        switch (activity) {
            case "AcceptanceDetailsActivity":
                mCallingIntent = new Intent(this, AcceptanceDetailsActivity.class);
                isScan = intent.getBooleanExtra("isScan", false);
                break;
            case "ProductionActivity":
                mCallingIntent = new Intent(this, ProductionActivity.class);
                break;
        }

        mTitle = findViewById(R.id.textViewTitle);
        mButtonBack = findViewById(R.id.imageButtonBack);
        mButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //нажатие стрелки назад в Toolbar
               setBackItems();
            }
        });

        //Получаем товар из базы
        getAll();

        // проверяем есть ли активное соединение с WiFi
        if (!isWifiConnected()) {
            // если нет соединения с WiFi - отобображаем пользователю инфо
            displayWifiErrorDialog();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Класс, представляющий адаптер для списка объектов Item (Категория, Товар)
     * Created by Alex on 24.03.2018.
     */

    public class ListItemAdapter extends RecyclerView.Adapter<ListItemAdapter.ViewHolder> {
        private List<Item> mItemList;
        private Item mItem;


        /**
         * Класс, содержащий компоненты для отображения объекта Item в списке
         */
        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            // Holder должен содержать переменные для любой View
            public TextView mCategoryName;
            public ImageView mImageView;


            public ViewHolder(View itemView) {
                super(itemView);

                mCategoryName = (TextView) itemView.findViewById(R.id.category_name);
                mImageView = (ImageView) itemView.findViewById(R.id.category_arrow);

                itemView.setOnClickListener(this);

            }

            @Override
            public void onClick(View v) {

                //листаем вперед
                int position = getAdapterPosition();
                Item item = mItemList.get(position);
                //положить этот item в стек
                mStack.push(item);

                if (item.isCategory()) {
                    //заголовок в ToolBar
                    mTitle.setText(item.getName());
                    mParentId = item.getId();
                }else{
                    //выбрана номенклатура, возврат
                    returnItem(item);
                }
                //переход в нажатую категорию
                setForwardItems(item);
            }
        }

        /**
         * Ctor
         */
        public ListItemAdapter(Context context, List<Item> itemList) {
            this.mItemList = itemList;

        }


        /**
         * Заполняет layout из xml и возвращает ViewHolder
         */
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(CategoryActivity.this);
            // layout из файла list_item.xml
            View categoryView = inflater.inflate(R.layout.list_item, parent, false);

            ViewHolder viewHolder = new ViewHolder(categoryView);

            return viewHolder;
        }

        /**
         * Заполняем объект Item данными с помощью ViewHolder
         *
         * @param viewHolder
         * @param position
         */
        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, int position) {
            // модель (объект Item) данных по позиции в списке
            mItem = mItemList.get(position);

            // устанавливаем значения TextView - название объекта (Категория, Товар)
            TextView textView = viewHolder.mCategoryName;
            textView.setText(mItem.getName());

            // Отображаем ImageView (стрелка вправо) только для категорий (isCategory=true)
            ImageView imageView = viewHolder.mImageView;
            imageView.setVisibility(mItem.isCategory() ? View.VISIBLE : View.GONE);
        }

        /**
         * Возвращает количество объектов Item в спсике
         *
         * @return - List<Item>
         */
        @Override
        public int getItemCount() {
            return mItemList == null ? 0 : mItemList.size();
        }
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
     * Возврат выбранного объекта в родительскую активность
     */
    void returnItem(Item item){
        Intent i = new Intent();
        i.putExtra("id_items", item.getId());
        i.putExtra("name_items", item.getName());
        i.putExtra("isScan", isScan);
        setResult(RESULT_OK, i);
        finish();
    }

    /**
     * Возвращает все объекты
     */
    private void getAll() {
        List<Item> mAllItems = mItemHelper.getAllItemFromDB();
        mCategories = mItemHelper.getItemsByParentId(mAllItems,"null");
        setAdapter(mCategories);
    }

    /**
     * Навигация вперед
     *
     * @param item
     */
    private void setForwardItems(Item item){
              String id=item.getId();
                List<Item> backItems = new ArrayList<>();
                List<Item> allItems = mItemHelper.getAllItemFromDB();
                for (Item itemR : allItems) {
                    if (id.equals(itemR.getParentId())) {
                        backItems.add(itemR);
                    }
                }

                setAdapter(backItems);
    }

    /**
     * Навигация назад
     *
     */
    private void setBackItems() {
        try{

            if (mStack.size() > 0) {
                Item backItem = mStack.pop();
                if (mStack.size()>0){
                  String parentNameCategory=mStack.peek().getName();
                    //заголовок
                    mTitle.setText(parentNameCategory);
                }

                String parentId = backItem.getParentId();
                List<Item> backItems = new ArrayList<>();
                List<Item> allItems = mItemHelper.getAllItemFromDB();
                for (Item item : allItems) {
                    if (item.getParentId().equals(parentId)) {
                        backItems.add(item);
                    }
                }

                setAdapter(backItems);
            } else {
                //Intent intent = new Intent(this, AcceptanceDetailsActivity.class);
                startActivity(mCallingIntent);
            }

        }catch (EmptyStackException e){
            Log.e("TAG",  e.getMessage());
        }
    }

    /**
     * Устанавливает адаптер (источник данных) - список объектов Item
     * @param categories - коллекция (список) объектов Item
     */
    private void setAdapter(List<Item> categories) {
        if (rvCategories == null) {

            rvCategories = (RecyclerView) findViewById(R.id.category_recycler_view);
            ListItemAdapter adapter = new ListItemAdapter(CategoryActivity.this, categories);
            rvCategories.setLayoutManager(new LinearLayoutManager(CategoryActivity.this));
            rvCategories.setAdapter(adapter);

        } else {

            ListItemAdapter adapter = new ListItemAdapter(CategoryActivity.this, categories);
            rvCategories.setLayoutManager(new LinearLayoutManager(CategoryActivity.this));
            rvCategories.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }

    }

}
