package ru.cityprint.arm.warehouse.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import ru.cityprint.arm.warehouse.R;
import ru.cityprint.arm.warehouse.barcode.CoreApplication;

/**
 * Активити, которая отображает настройки приложения
 */

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getFragmentManager().findFragmentById(android.R.id.content) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    /**
     * Фрагмент, отображающий настройки (Preferences) из файла preferences.xml
     */
    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // нужно проинициализировать Retrofit - изменить BaseUrl (из настроек)
            CoreApplication.setRetrofit();
        }
    }
}