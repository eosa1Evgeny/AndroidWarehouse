package ru.cityprint.arm.warehouse.utils;

import android.content.Context;
import android.media.MediaPlayer;

import ru.cityprint.arm.warehouse.R;

/**
 * Created by seregin.aleksey on 13.04.2018.
 * Класс-помощник для работы со звуком (оповещение) при сканировании
 */

public class SoundHelper implements MediaPlayer.OnCompletionListener{
    private MediaPlayer mediaPlayerSuccess;             // воспроизведени звука при успешном сканировании
    private MediaPlayer mediaPlayerError;               // воспроизведени звука при неудачном сканировании
    private Context mContext;                           // контекст

    public SoundHelper(Context context) {
        mContext = context;
    }

    /**
     * Инициализация объекта MediaPlayer для воспроизведения звука при сканировании (успех)
     */
    private void initSuccessMediaPlayer() {
        // успешное сканирование
        mediaPlayerSuccess = MediaPlayer.create(mContext, R.raw.success);
        mediaPlayerSuccess.setOnCompletionListener(this);
    }

    /**
     * Инициализация объекта MediaPlayer для воспроизведения звука при сканировании (ошибка)
     */
    private void initErrorMediaPlayer() {
        // неуспешное сканирование
        mediaPlayerError = MediaPlayer.create(mContext, R.raw.error);
        mediaPlayerError.setOnCompletionListener(this);
    }

    /**
     * Проигрывает звук - успешное сканирование
     */
    public void playSuccessSound() {
        initSuccessMediaPlayer();
        mediaPlayerSuccess.start();
    }

    /**
     * Проигрывет звук при неуспешном сканировании (ошибка)
     */
    public void playErrorSound() {
        initErrorMediaPlayer();
        mediaPlayerError.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.release();
    }
}
