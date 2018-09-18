package com.bornaapp.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * Created by Mehdi on 2/15/2018.
 * encapsulates methods to get player info
 */

public class GooglePlayer {

    private static final String TAG = "Mehdi";
    private static final int RC_PLAYER_PROFILE = 8001;

    private Activity activity;

    private PlayersClient mPlayersClient;

    public GooglePlayer(Activity _activity) {
        activity = _activity;
    }

    public void HandleActivityResult(int requestCode, int resultCode, Intent intent) {
        //if (requestCode == RC_PLAYER_PROFILE) {
        //}
    }

    public void Connect(GoogleSignInAccount googleSignInAccount) {
        if (mPlayersClient == null)
            mPlayersClient = Games.getPlayersClient(activity, googleSignInAccount);
    }

    public void Disconnect() {
        mPlayersClient = null;
    }

    public void showPlayerProfile() {
        // return if not connected
        if (mPlayersClient == null)
            return;

        mPlayersClient.getCurrentPlayer().addOnCompleteListener(new OnCompleteListener<Player>() {
            @Override
            public void onComplete(@NonNull Task<Player> task) {
                Player player = task.getResult();
                if (player == null)
                    return;
                mPlayersClient.getCompareProfileIntent(player)
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                activity.startActivityForResult(intent, RC_PLAYER_PROFILE);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, e.getMessage());
                            }
                        });
            }
        });
    }

    public void getPlayerInfo(OnCompleteListener<Player> listener) {
        // return if not connected
        if (mPlayersClient == null)
            return;

        mPlayersClient.getCurrentPlayer().addOnCompleteListener(listener);
    }
}
