package ru.cityprint.arm.warehouse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.barcode.CoreApplication;
import ru.cityprint.arm.warehouse.data.Acceptance;
import ru.cityprint.arm.warehouse.data.AcceptanceResponse;

/**
 * список ранее отсканированных и отправленных ШК
 */

public class AcceptanceActivity extends AppCompatActivity {
    public final static String TAG = "AcceptanceActivity";
    private List<Acceptance> mAcceptances;              // принимаем с сервера
    private RecyclerViewAdapter mAdapter;
    private RecyclerView mRecyclerView;                 // список отсканированных ШК

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acceptance);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_acceptance);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_close);

        // инициализация RecyclerView
        mRecyclerView = findViewById(R.id.rv_acceptance);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               startAcceptanceDetailsActivity();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getAcceptance();

    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent i = new Intent(this , ModulesActivity.class);
        startActivity(i);
        finish();
        return true;
    }

    /**
     * старт активности сканирования
     */
    private void startAcceptanceDetailsActivity(){
        Intent i = new Intent(this , AcceptanceDetailsActivity.class);
        startActivity(i);
    }

    /**
     * старт активности редактирования накладной (id)
     */

    private void startAcceptanceEditActivity(String id){

        Intent i = new Intent(this , AcceptanceDetailsActivity.class);
        i.putExtra("Activity", TAG);
        i.putExtra("id", id);
        startActivity(i);
    }

    /**
     * Ретрофит
     *
     */

    public void getAcceptance(){

        CoreApplication.getApi().getAcceptances().enqueue(new Callback<AcceptanceResponse>() {
            @Override
            public void onResponse(Call<AcceptanceResponse> call, Response<AcceptanceResponse> response) {
                try {
                    mAcceptances = response.body().getData();
                    updateUI();
                }catch (Exception e){
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<AcceptanceResponse> call, Throwable t) {
                Log.e(TAG, t.getMessage());

                try {
                    AcceptanceResponse mResult = new AcceptanceResponse();
                    mResult.code = call.execute().code();
                    mResult.message = t.getMessage();
                    mResult.data = call.execute().body().data;

                    showError(mResult);

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
    private void  showError(AcceptanceResponse result){
        new MaterialDialog.Builder(this)
                .title(R.string.titleS)
                .content("code: "+ String.valueOf(result.code) + " data: " + String.valueOf(result.data) + " message: " + String.valueOf(result.message))
                .positiveText(R.string.agreeS)
                .show();
    }

    /**
     * обновление адаптера
     */

    private void updateUI(){

    if (mAdapter == null) {
        mAdapter = new RecyclerViewAdapter(mAcceptances);
        mRecyclerView.setAdapter(mAdapter);
    } else {
        mAdapter.setAcceptancesRecyclerView(mAcceptances);
        mAdapter.notifyDataSetChanged();
    }
}




    /**
     * Holder
     */
    private class RecyclerViewHolder extends RecyclerView.ViewHolder implements RecyclerView.OnClickListener {

        private TextView mTitleTextView;
        private String mId;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = itemView.findViewById(R.id.acceptance_name);
        }

        public void bindAcceptance(Acceptance acceptance) {
            mTitleTextView.setText(acceptance.getName());
            mId = acceptance.getId();
        }

        @Override
        public void onClick(View v) {
            startAcceptanceEditActivity(mId);
        }
    }

    /**
     * Адаптер
     */
    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
        private List<Acceptance> mAcceptancesRecyclerView;

        public RecyclerViewAdapter(List<Acceptance> Acceptances) {
            mAcceptancesRecyclerView = Acceptances;

        }

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(AcceptanceActivity.this);
            View view = layoutInflater.inflate(R.layout.list_item_acceptance_activity, parent, false);
            return new RecyclerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewHolder holder, final int position) {
            final Acceptance acceptance = mAcceptancesRecyclerView.get(position);

            holder.bindAcceptance(acceptance);
        }

        @Override
        public int getItemCount() {
            return mAcceptancesRecyclerView.size();
        }

        public void setAcceptancesRecyclerView(List<Acceptance> acceptances) {
            mAcceptancesRecyclerView = acceptances;
        }

    }



}

