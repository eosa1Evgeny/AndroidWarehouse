package ru.cityprint.arm.warehouse.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Класс, отображающий модальное информационное диалоговое окно для вывода сообщения
 * с одной кнопкой OK
 * Created by Alex on 24.03.2018.
 */

public class MessageDialog extends DialogFragment {
    public static final String ARG_TITLE = "MessageDialog.Title";
    public static final String ARG_MESSAGE = "MessageDialog.Message";

    public MessageDialog() {}

    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = getArguments();
        String title = args.getString(ARG_TITLE);
        String message = args.getString(ARG_MESSAGE);

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
                    }
                })
                /*.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()   TODO ????
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
                    }
                })*/
                .create();
    }
}
