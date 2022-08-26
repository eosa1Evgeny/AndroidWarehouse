package ru.cityprint.arm.warehouse.network;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 *  Класс для работы с сетью WiFi
 */

public class WifiHelper {
    private static boolean isConnectedToWifi;
    private static WifiConnectionChange sListener;

    public interface WifiConnectionChange {
        void wifiConnected(boolean connected);
    }


    /** Используется Broadcast Receiver для отслеживания события подключения к WiFi
     * Only used by Connectivity_Change broadcast receiver
     * @param connected
     */
    public static void setWifiConnected(boolean connected) {
        isConnectedToWifi = connected;
        if (sListener!=null)
        {
            sListener.wifiConnected(connected);
            sListener = null;
        }
    }

    public static void setWifiListener(WifiConnectionChange listener) {
        sListener = listener;
    }

    /**
     * Возвращает подключено ли устройство к WiFi
     * @return
     */
    public static Boolean isConnected(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled()) {      // WiFi адаптер включен
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getNetworkId() == -1) {
                return false;       // нет соединения с Access Point
            }
            return true;            // есть соединение с Access Point
        } else {
            return false;           // WiFi адаптер выключен
        }
    }

}
