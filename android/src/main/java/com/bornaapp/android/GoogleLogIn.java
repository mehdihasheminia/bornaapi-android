package com.bornaapp.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bornaapp.androidlib.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Created by Mehdi on 2/13/2018.
 * Encapsulated Google Sign-In functionality
 * Settigs & SH1 signing-certificate fingerprint in google API console
 * https://console.developers.google.com/apis/credentials
 */

public class GoogleLogIn {

    private static final String TAG = "Mehdi(log-in)";
    private static final int RC_SIGN_IN = 5001;

    private Activity activity;
    private Listener mListener = null;
    private boolean justConnected = false;

    // Client used to sign in with Google APIs
    private GoogleSignInClient mGoogleSignInClient;

    public GoogleLogIn(Activity activity) {
        this.activity = activity;
        // Create the client used to sign in to Google services.
        mGoogleSignInClient = GoogleSignIn.getClient(activity,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER) // Add the APPFOLDER scope for Snapshot support.
                        //.requestEmail()                       //access to user email
                        .build());
    }

    public void HandleActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (mListener != null) {
                    mListener.onConnected(account);
                    //if we connected via this method, prevent silentSignIn() or sign-out for next 2 sec
                    justConnected = true;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            justConnected = false;
                        }
                    }, 2000);
                }
            } catch (ApiException e) {
                mListener.onError(e.getMessage() + "\n" + activity.getString(R.string.error_signin_other));
            }
        }
    }

    public void startSignInIntent() {
        //return if not connected
        if (mGoogleSignInClient == null)
            return;

        activity.startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    public void signInSilently() {

        //return if not connected
        if (mGoogleSignInClient == null)
            return;

        if (justConnected)
            return;

        mGoogleSignInClient.silentSignIn().addOnCompleteListener(activity,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            //Log.d(TAG, "signInSilently(): success");
                            if (mListener != null)
                                mListener.onConnected(task.getResult());
                        } else {
                            //Log.d(TAG, "signInSilently(): failure", task.getException());
                            Log.w(TAG, "signInSilently(): failure!");
                            mListener.onDisconnected();
                        }
                    }
                });
    }

    // returns Async
    public boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(activity) != null;
    }

    public void signOut() {
        //return if not connected
        if (mGoogleSignInClient == null)
            return;

        if (justConnected)
            return;

        if (isSignedIn()) {//does this method really works?
            mGoogleSignInClient.signOut().addOnCompleteListener(activity,
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            boolean successful = task.isSuccessful();
                            Log.d(TAG, "signOut(): " + (successful ? "success" : "failed"));
                            mListener.onDisconnected();
                        }
                    });
        }
    }

    //--------------------------------------- Custom UiListener --------------------------------------

    public void setListener(GoogleLogIn.Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        // called when the user connected to google account
        void onConnected(GoogleSignInAccount googleSignInAccount);

        // called when the user disconnects from user account
        void onDisconnected();

        void onError(String msg);
    }
}
