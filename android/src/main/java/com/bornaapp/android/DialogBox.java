package com.bornaapp.android;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bornaapp.androidlib.R;
import com.google.android.gms.common.images.ImageManager;

/**
 * Created by Mehdi on 4/20/2018.
 */

public class DialogBox extends Dialog implements View.OnClickListener {

    private static final String TAG = "Mehdi(DialogBox)";

    private Context context;

    private Button mOKBtn, mYesBtn, mNoBtn;
    private ImageView mIconImageView;
    private TextView mMessageTextView;

    DialogBoxListener mListener = null;

    public DialogBox(Context context) {
        super(context);
        this.context = context;
    }

    //------------------------------------- Life cycle ---------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialogbox);

        //referencing views for easy access
        mMessageTextView = (TextView) findViewById(R.id.txt_dialog_message);
        mOKBtn = (Button) findViewById(R.id.btn_dialog_ok);
        mYesBtn = (Button) findViewById(R.id.btn_dialog_yes);
        mNoBtn = (Button) findViewById(R.id.btn_dialog_no);
        mIconImageView = (ImageView) findViewById(R.id.img_icon);

        mOKBtn.setOnClickListener(this);
        mYesBtn.setOnClickListener(this);
        mNoBtn.setOnClickListener(this);

        this.setCanceledOnTouchOutside(true); //default = can be cancelled when touched outside
        this.setCancelable(true);             //default = can be cancelled with back-key
        this.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dismiss();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btn_dialog_ok) {
            if (mListener != null)
                mListener.onOKPressed();

        } else if (i == R.id.btn_dialog_yes) {
            if (mListener != null)
                mListener.onYesPressed();

        } else if (i == R.id.btn_dialog_no) {
            if (mListener != null)
                mListener.onNoPressed();

        } else {
        }
        dismiss();
    }

    //--------------------------------------- Public methods ---------------------------------------

    void showText(final String msg, boolean isSingleBtn, boolean isCancelable) {
        show(msg, isSingleBtn, isCancelable);
        mIconImageView.setVisibility(View.GONE);
    }

    public void showWarning(final String msg, boolean isSingleBtn, boolean isCancelable) {
        show(msg, isSingleBtn, isCancelable);
        mIconImageView.setImageResource(R.drawable.warning_icon);
    }

    public void showInfo(final String msg, boolean isSingleBtn, boolean isCancelable) {
        show(msg, isSingleBtn, isCancelable);
        mIconImageView.setImageResource(R.drawable.info_icon);
    }

    public void showInvitation(String inviterName, ImageManager imageManager, Uri imageUri, boolean canAccept) {
        String msg;
        if (canAccept)
            msg = inviterName + "\n" + context.getString(R.string.txt_is_inviting_you);
        else
            msg = inviterName + "\n" + context.getString(R.string.txt_cant_accept_invitation);
        //
        show(msg, !canAccept, false);
        mMessageTextView.setGravity(Gravity.CENTER);
        //
        if (imageUri != null)
            imageManager.loadImage(mIconImageView, imageUri);
        else
            mIconImageView.setImageResource(R.drawable.anonymous_user);

    }

    //------------------------------------- Private methods ----------------------------------------

    private void show(final String msg, boolean isSingleBtn, boolean isCancelable) {
        super.show();

        if (isSingleBtn) {
            mOKBtn.setVisibility(View.VISIBLE);
            mYesBtn.setVisibility(View.GONE);
            mNoBtn.setVisibility(View.GONE);
        } else {
            mOKBtn.setVisibility(View.GONE);
            mYesBtn.setVisibility(View.VISIBLE);
            mNoBtn.setVisibility(View.VISIBLE);
        }

        this.setCancelable(isCancelable);
        this.setCanceledOnTouchOutside(isCancelable);

        mMessageTextView.setText(msg);
    }

    //------------------------------------- Listeners ----------------------------------------------

    public void setListener(DialogBoxListener listener) {
        mListener = listener;
    }

    public interface DialogBoxListener {
        void onOKPressed();

        void onYesPressed();

        void onNoPressed();
    }
}