package com.usepace.android.messagingcenter.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
import com.usepace.android.messagingcenter.R;

public class LoadingUtils {

    private Activity context;
    private Dialog onScreenDialog;

    public LoadingUtils(Activity context) {
        this.context = context;
    }

    /**
     *
     */
    public void showOnScreenLoading() {
        if (onScreenDialog != null && onScreenDialog.isShowing())
            return;
        onScreenDialog = new Dialog(context);
        onScreenDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        onScreenDialog.setTitle(null);
        onScreenDialog.setCancelable(false);
        onScreenDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                    return false;
                return false;
            }
        });
        onScreenDialog.setContentView(R.layout.dialog_loading);
        onScreenDialog.show();
    }

    /**
     *
     */
    public void hideOnScreenLoading() {
        if (context != null && !context.isFinishing()) {
            if (onScreenDialog != null && onScreenDialog.isShowing())
                try {onScreenDialog.dismiss();} catch (IllegalArgumentException e) {} //View not attached to window !
        }
    }


}
