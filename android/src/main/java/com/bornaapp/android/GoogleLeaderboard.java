package com.bornaapp.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bornaapp.androidlib.R;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import static com.google.android.gms.games.leaderboard.LeaderboardVariant.COLLECTION_PUBLIC;
import static com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_WEEKLY;

/**
 * Created by Hashemi on 14/02/2018.
 * encapsolates google learboard functionality
 * Settigs & SH1 signing-certificate fingerprint in google API console
 * https://console.developers.google.com/apis/credentials
 */

public class GoogleLeaderboard {

    private static final String TAG = "Mehdi";
    private static final int RC_LEADERBOARD = 6001;

    private Activity activity;

    private LeaderboardsClient mLeaderboardsClient;
    private String leaderboardID;

    private UiListener mUiListener = null;

    public GoogleLeaderboard(Activity _activity, String _id) {
        activity = _activity;
        mLeaderboardsClient = null;
        leaderboardID = _id;
    }

    public void HandleActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == RC_LEADERBOARD) {
            if (mUiListener != null)
                mUiListener.onLeaderboardClientCollapsed();
        }
    }

    public void Connect(GoogleSignInAccount googleSignInAccount) {
        if (mLeaderboardsClient == null)
            mLeaderboardsClient = Games.getLeaderboardsClient(activity, googleSignInAccount);
    }

    public void Disconnect() {
        mLeaderboardsClient = null;
    }

    public void showLeaderboardClient() {

        //return if not connected
        if (mLeaderboardsClient == null)
            return;

        //mLeaderboardsClient.getAllLeaderboardsIntent() //get all leaderboards
        // WEEKLY: Scores reset once per week at 11:59PM PST on Sunday(UTC-7). between saturday & sunday
        mLeaderboardsClient.getLeaderboardIntent(leaderboardID, TIME_SPAN_WEEKLY, COLLECTION_PUBLIC)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        activity.startActivityForResult(intent, RC_LEADERBOARD); //Show client
                        //LoadAllScores();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mUiListener.onLeaderboardError(e.getMessage() + "\n" + activity.getString(R.string.exception_leaderboards));
                    }
                });
    }

    public void SubmitScore(long score) {
        //return if not connected
        if (mLeaderboardsClient == null)
            return;

        mLeaderboardsClient.submitScore(leaderboardID, score);
    }

    //attention: Loading from leaderboard is time-taking and leaderboard is updated with delay
    void LoadCurrentPlayerScore(final ScoreListener scoreListener) {

        //return if not connected
        if (mLeaderboardsClient == null)
            return;

        mLeaderboardsClient.loadCurrentPlayerLeaderboardScore(leaderboardID, TIME_SPAN_WEEKLY, COLLECTION_PUBLIC)
                .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
                    @Override
                    public void onSuccess(AnnotatedData<LeaderboardScore> leaderboardScoreAnnotatedData) {
                        try {
                            LeaderboardScore score = leaderboardScoreAnnotatedData.get();
                            //Log.d(TAG, score.getDisplayRank() + ": " + score.getScoreHolderDisplayName() + " , " + score.getDisplayScore());
                            if (scoreListener != null)
                                scoreListener.onCurrentPlayerScoreLoaded(score);
                        } catch (Exception e) {
                            Log.e(TAG, "Error on LoadCurrentPlayerScore: " + e);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "LoadCurrentPlayerScore failed: " + e.getMessage());
                        scoreListener.onCurrentPlayerScoreLoaded(null);
                    }
                });
    }

    void LoadAllScores() {

        //return if not connected
        if (mLeaderboardsClient == null)
            return;

        mLeaderboardsClient.loadPlayerCenteredScores(leaderboardID, TIME_SPAN_WEEKLY, COLLECTION_PUBLIC, 25, true)  //or TIME_SPAN_ALL_TIME
                .addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardsClient.LeaderboardScores>>() {
                    @Override
                    public void onComplete(@NonNull Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> task) {
                        try {
                            if (task.isSuccessful()) {
                                LeaderboardScoreBuffer scores = task.getResult().get().getScores();
                                for (LeaderboardScore score : scores) {
                                    Log.d(TAG, score.getDisplayRank() + ": " + score.getScoreHolderDisplayName() + " , " + score.getDisplayScore());
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error on LoadAllScores" + e);
                        }
                    }
                });
    }

    //--------------------------------- custom UiListener --------------------------------------------

    public void setListener(UiListener uiListener) {
        mUiListener = uiListener;
    }

    public interface UiListener {

        void onLeaderboardClientCollapsed();

        void onLeaderboardError(String msg);
    }

    interface ScoreListener {
        void onCurrentPlayerScoreLoaded(LeaderboardScore score);
    }
}

