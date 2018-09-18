package com.bornaapp.android;

import android.app.ProgressDialog;
import android.content.Context;


/**
 * Created by Mehdi on 2/27/2018.
 */

public class DialogLoading {

    private Context context;
    private ProgressDialog mLoadingDialog = null;

    public DialogLoading(Context _context) {
        context = _context;
    }

    public void Show(String message) {
        if (mLoadingDialog == null) {
            mLoadingDialog = new ProgressDialog(context);
            //
            mLoadingDialog.setCanceledOnTouchOutside(false);
            mLoadingDialog.setCancelable(false);
            //
            mLoadingDialog.setMessage(message);
        }
        mLoadingDialog.show();
    }

    public void Dismiss() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }
}
