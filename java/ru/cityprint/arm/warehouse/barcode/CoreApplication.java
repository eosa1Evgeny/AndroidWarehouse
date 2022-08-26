package ru.cityprint.arm.warehouse.barcode;

import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rs.core.CoreInterface;
import rs.core.Utils;
import rs.core.hw.Barcode;
import rs.core.hw.NfcTag;
import ru.cityprint.arm.warehouse.api.ServiceApi;

import static ru.cityprint.arm.warehouse.utils.Constants.SERVICE_API_URL;

/**
 * Класс-диспетчер для обработки событий от сервиса RSCore(сканер штрих-кодов)
 */

public class CoreApplication extends Application {
    private final String MESSAGE_TAG = getClass().getPackage().getName() + "." + hashCode();
    private static CoreApplication instance = null;
    protected CoreInterface mRSCore;
    protected String DEVICE = "barcodeReader";
    private static ServiceApi mServiceApi;
    private static Retrofit mRetrofit;
    private static SharedPreferences mSharedPreferences;
    private static String mBaseUrl;                        // адрес (хост) сервиса API

    /**
     * Singleton экземпляр класса CoreApplication
     * @return CoreApplication
     */
    public static CoreApplication getInstance() {
        return instance;
    }

    private Set<LocalCoreDataReceiver> RECEIVERS = Collections.synchronizedSet(new HashSet<LocalCoreDataReceiver>());

    /***
     * Используется для получения штрих-кода от RSCore
     */
    private BroadcastReceiver CORE_EVENT_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Parcelable data = intent.getParcelableExtra(Constants.BARCODE_DATA_EXTRA);

            // вызываем обработчик получения штрих-кода
            onCoreEvent(intent.getStringExtra(Constants.BARCODE_SENDER_EXTRA), data);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // определеяем контекст Application
        instance = this;

        // регистрируем BroadcastReceiver для получения штрих-кода от сервиса RSCore
        registerReceiver(CORE_EVENT_RECEIVER, new IntentFilter(MESSAGE_TAG));

        // привязываемся к сервису RSCore
        attachService();

        // получаем настройки приложения по умолчанию
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // BASE_URL из настроек
        setBaseUrl();

        // инициализация объекта Retrofit
        setRetrofit();

        // генерируем объект, который будет испольваться для выполнения запросов к API
        setServiceApi();
    }

    /**
     * Регистрация слушателя LocalCoreDataReceiver для отправки слушателю данных от сервиса RSCore
     * @param rcvr - объект LocalCireDataReceiver
     */
    public void registerLocalListener(LocalCoreDataReceiver rcvr) {
        RECEIVERS.add(rcvr);
    }

    /**
     * Отмена регистрации слушателя LocalCoreDataReceiver
     * @param rcvr - объект LocalCireDataReceiver
     */
    public void unregisterLocalListener(LocalCoreDataReceiver rcvr) {
        RECEIVERS.remove(rcvr);
    }

    /**
     * ServiceConnection, который используется для установления связи (посредством ServiceConnection)
     * с сервисом RSCore
     * Коммуникация осуществляется через IPC
     */
    private ServiceConnection mCoreServiceConnector = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mRSCore = CoreInterface.Stub.asInterface(service);

            try {
                mRSCore.subscribe(MESSAGE_TAG);
            } catch(RemoteException re){
                Log.e(MESSAGE_TAG, "Ошибка при вызове RSCore " + re.getLocalizedMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            detachService();
        }
    };

    /**
     * Метод выполняется при нажатии на кнопку сканирования
     * @param sender -
     * @param data
     */
    public void onCoreEvent(String sender, Parcelable data) {
        // работаем только, если Sender = "barcodeReader"
        if (DEVICE.equalsIgnoreCase(sender)) {
            if (data instanceof Barcode) {

                Barcode barcode = (Barcode)data;
                final String barcodeValue = barcode.value;

                if (!barcodeValue.isEmpty()) {
                    for(LocalCoreDataReceiver receiver : RECEIVERS) {
                        if(receiver.onReceivedData(barcode))
                            break;
                    }
                }
            }
            else if(data instanceof NfcTag){
                // можно вызвать свобе событие Nfc
            }
            else if(data instanceof KeyEvent) {
                // обработка KeyCode
            }
        }
    }

    /**
     * Метод осуществляет связывание (binding) с сервисом RSCore
     */
    private void attachService() {
        Intent service = new Intent(Utils.CORE_SERVICE_INTENT);
        bindService(service, mCoreServiceConnector, Service.BIND_AUTO_CREATE);
    }


    /**
     * Метод осуществляет отсоединение от сервиса RSCore
     */
    private void detachService() {
        try {
            mRSCore.unsubscribe(MESSAGE_TAG);
        } catch (RemoteException e) {
            mRSCore = null;
        }
        //unbindService(mCoreServiceConnector);
    }

    /**
     * Возвращает экземпляр класса ServiceApi
     */
    public static ServiceApi getApi() {
        return getServiceApi();
    }

    /**
     * Инициализирует ServiceApi
     */
    private static void setServiceApi() {
        mServiceApi = getRetrofit().create(ServiceApi.class);
    }

    private static ServiceApi getServiceApi() {
        return mServiceApi;
    }

    /**
     * Возвращает экземпляр класса Retrofit - для работы с API сервиса
     * @return
     */
    private static Retrofit getRetrofit() {
        return mRetrofit;
    }

    /**
     * Инициализирует экземпляр класса Retrofit
     */
    public static void setRetrofit() {
        // инициализируем Retrofit
        mRetrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Инициализирует BASE_URL для сервиса из настроек
     */
    private static void setBaseUrl() {
        // получаем Service API URL из настроек, либо из константы SERVICE_API_URL
        mBaseUrl = mSharedPreferences.getString("pref_service_api_url", "").isEmpty()
                ? SERVICE_API_URL
                : mSharedPreferences.getString("pref_service_api_url", "");
    }

    /**
     * Возвращает BASE_URL для сервиса из настроек
     * @return
     */
    private static String getBaseUrl() {
        return mBaseUrl;
    }


}
