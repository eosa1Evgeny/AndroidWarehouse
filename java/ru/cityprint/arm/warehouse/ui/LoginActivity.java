package ru.cityprint.arm.warehouse.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import ru.cityprint.arm.warehouse.R;

public class LoginActivity extends AppCompatActivity {

    private Button mBtnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_login);
        myToolbar.setTitle(R.string.login_toolbar);
        setSupportActionBar(myToolbar);

        mBtnLogin = findViewById(R.id.btn_login);

       Intent i = getIntent();
       boolean wf = i.getBooleanExtra("wiFi", false);

       if (!wf){
         //нет вайфая
           MaterialDialog dialog = new MaterialDialog.Builder(this)
                   .title("Ошибка связи!")
                   .content("Нет соединения с сервером.Проверьте работу WiFi.")
                   .positiveText(R.string.agreeS)
                   .show();
       }

        login();
    }

    private void login(){
       mBtnLogin.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               // активность
               start();

           }
       });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                //TODO Здесь что то надо добавить
                Toast.makeText(this, "О программе", Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);


        }
    }

    protected void start(){
        Intent i = new Intent(LoginActivity.this, ModulesActivity.class );
        startActivity(i);
    }

}
