package com.bornaapp.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.bornaapp.androidlib.R;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClientStatusCodes;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.Result;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Mehdi on 2/15/2018.
 * encapsulates cloud save functionality
 * https://developers.google.com/games/services/android/savedgames
 */

public class GoogleSave {

    private static final String TAG = "Mehdi(GoogleSave)";

    private static final int RC_SAVED_GAMES = 7001;
    private static final int RC_LIST_SAVED_GAMES = 7002;
    private static final int RC_SELECT_SNAPSHOT = 7003;
    private static final int RC_SAVE_SNAPSHOT = 7004;
    private static final int RC_LOAD_SNAPSHOT = 7005;

    private Activity activity;

    private Gson gson;

    private final static int MAX_SNAPSHOT_RESOLVE_RETRIES = 50;

    private SnapshotsClient mSnapshotsClient = null;

    private String currentSaveName = "BaR_GSAVE_01";  //old name="snapshotBaR1"
    public GoogleSaveData saveData = new GoogleSaveData();

    public GoogleSave(Activity _activity) {
        activity = _activity;
        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
    }

    public void HandleActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == RC_LIST_SAVED_GAMES) {
            // the standard snapshot selection intent
            if (intent != null) {
                if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    SnapshotMetadata snapshotMetadata = intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
                    currentSaveName = snapshotMetadata.getUniqueName();
                    Load(snapshotMetadata, true, null);
                } else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
                    // Create a new snapshot named with a unique string
                    String unique = Long.toString(System.currentTimeMillis());
                    currentSaveName = "snapshotTemp-" + unique;
                    Save(null, null);
                }
            }
        }
        // the example use of Snapshot.load() which displays a custom list of snapshots.
        else if (requestCode == RC_SELECT_SNAPSHOT) {
            Log.d(TAG, "Selected a snapshot!");
            if (resultCode == RESULT_OK) {
                if (intent != null && intent.hasExtra(GoogleSaveActivity.SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    SnapshotMetadata snapshotMetadata = intent.getParcelableExtra(GoogleSaveActivity.SNAPSHOT_METADATA);
                    currentSaveName = snapshotMetadata.getUniqueName();
                    Log.d(TAG, "ok - loading " + currentSaveName);
                    Load(snapshotMetadata, true, null);
                } else {
                    Log.w(TAG, "Expected snapshot metadata but found none.");
                }
            }
        }
        // loading a snapshot into the MyGame.
        else if (requestCode == RC_LOAD_SNAPSHOT) {
            Log.d(TAG, "Loading a snapshot resultCode = " + resultCode);
            if (resultCode == RESULT_OK) {
                if (intent != null && intent.hasExtra(GoogleSaveActivity.SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    String conflictId = intent.getStringExtra(GoogleSaveActivity.CONFLICT_ID);
                    int retryCount = intent.getIntExtra(GoogleSaveActivity.RETRY_COUNT, MAX_SNAPSHOT_RESOLVE_RETRIES);
                    SnapshotMetadata snapshotMetadata = intent.getParcelableExtra(GoogleSaveActivity.SNAPSHOT_METADATA);
                    if (conflictId == null) {
                        Load(snapshotMetadata, true, null);
                    } else {
                        Log.d(TAG, "resolving " + snapshotMetadata);
                        resolveSnapshotConflict(requestCode, conflictId, retryCount,
                                snapshotMetadata);
                    }
                }
            }

        }
        // saving the MyGame into a snapshot.
        else if (requestCode == RC_SAVE_SNAPSHOT) {
            if (resultCode == RESULT_OK) {
                if (intent != null && intent.hasExtra(GoogleSaveActivity.SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    String conflictId = intent.getStringExtra(GoogleSaveActivity.CONFLICT_ID);
                    int retryCount = intent.getIntExtra(GoogleSaveActivity.RETRY_COUNT, MAX_SNAPSHOT_RESOLVE_RETRIES);
                    SnapshotMetadata snapshotMetadata = intent.getParcelableExtra(GoogleSaveActivity.SNAPSHOT_METADATA);
                    if (conflictId == null) {
                        Save(snapshotMetadata, null);
                    } else {
                        Log.d(TAG, "resolving " + snapshotMetadata);
                        resolveSnapshotConflict(requestCode, conflictId, retryCount, snapshotMetadata);
                    }
                }
            }
        }
    }

    public void Connect(GoogleSignInAccount googleSignInAccount) {
        if (mSnapshotsClient == null)
            mSnapshotsClient = Games.getSnapshotsClient(activity, googleSignInAccount);
    }

    public void Disconnect() {
        mSnapshotsClient = null;
    }

    void onShowSavesRequested() {

        //return if not connected
        if (mSnapshotsClient == null)
            return;

        int maxNumberOfSavedGamesToShow = 5;
        mSnapshotsClient.getSelectSnapshotIntent("See My Saves", true, true, maxNumberOfSavedGamesToShow)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        activity.startActivityForResult(intent, RC_SAVED_GAMES);
                    }
                });
    }

    public void Load(final SnapshotMetadata snapshotMetadata, boolean showProgress, final Listener mListener) {

        //return if not connected
        if (mSnapshotsClient == null)
            return;

        //prepare a progress dialog to show progress to users
        ProgressDialog mLoadingDialog = new ProgressDialog(activity);
        mLoadingDialog.setMessage(activity.getString(R.string.msg_loading_from_cloud));

        if (showProgress)
            mLoadingDialog.show();

        waitForClosedAndOpen(snapshotMetadata)
                .addOnSuccessListener(new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                    @Override
                    public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> result) {

                        // if there is a conflict  - then resolve it.
                        Snapshot snapshot = processOpenDataOrConflict(RC_LOAD_SNAPSHOT, result, 0);

                        if (snapshot == null) {
                            Log.w(TAG, "Conflict was not resolved automatically, waiting for user to resolve."); //<-------------***(6)
                        } else {
                            try {
                                readSavedGame(snapshot);
                                Log.i(TAG, "Snapshot loaded.");
                                Log.d(TAG, gson.toJson(saveData));
                                //on sucess
                                if (mListener != null)
                                    mListener.onSuccess();
                            } catch (IOException e) {
                                Log.e(TAG, "Error while reading snapshot contents: " + e.getMessage());
                                if (mListener != null)
                                    mListener.onFailure();
                            }
                            //<----to here
                            GoogleSaveCoordinator.getInstance().discardAndClose(mSnapshotsClient, snapshot)
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            handleException(e, "There was a problem discarding the snapshot!");
                                        }
                                    });
                        }

                        //<---from here

                    }
                });

        if (mLoadingDialog.isShowing())
            mLoadingDialog.dismiss();
    }

    private void readSavedGame(Snapshot snapshot) throws IOException {
        saveData = gson.fromJson(new String(snapshot.getSnapshotContents().readFully()), GoogleSaveData.class);
        //first time
        if (saveData == null)
            saveData = new GoogleSaveData();
    }

    public void Save(final SnapshotMetadata snapshotMetadata, final Listener mListener) {

        //return if not connected
        if (mSnapshotsClient == null)
            return;

        waitForClosedAndOpen(snapshotMetadata)
                .addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                    @Override
                    public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {

                        SnapshotsClient.DataOrConflict<Snapshot> result = task.getResult();
                        Snapshot snapshotToWrite = processOpenDataOrConflict(RC_SAVE_SNAPSHOT, result, 0);

                        if (snapshotToWrite == null) {
                            // No snapshot available yet; waiting on the user to choose one.
                            return;
                        }

                        Log.d(TAG, "Writing data to snapshot: " + snapshotToWrite.getMetadata().getUniqueName());
                        writeSnapshot(snapshotToWrite)
                                .addOnCompleteListener(new OnCompleteListener<SnapshotMetadata>() {
                                    @Override
                                    public void onComplete(@NonNull Task<SnapshotMetadata> task) {
                                        if (task.isSuccessful()) {
                                            Log.i(TAG, "Snapshot saved!");
                                            if (mListener != null)
                                                mListener.onSuccess();
                                        } else {
                                            handleException(task.getException(), activity.getString(R.string.error_write_snapshot));
                                            if (mListener != null)
                                                mListener.onFailure();
                                        }
                                    }
                                });
                    }
                });
    }

    private Task<SnapshotMetadata> writeSnapshot(Snapshot snapshot) {
        // Set the data payload for the snapshot.
        snapshot.getSnapshotContents().writeBytes(gson.toJson(saveData).getBytes());//mSaveGame.toBytes()

        // Save the snapshot.
        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
//                .setCoverImage(getScreenShot())
                .setDescription("Modified data at: " + Calendar.getInstance().getTime())
                .build();
        return GoogleSaveCoordinator.getInstance().commitAndClose(mSnapshotsClient, snapshot, metadataChange);
    }

    private Snapshot processOpenDataOrConflict(int requestCode,
                                               SnapshotsClient.DataOrConflict<Snapshot> result,
                                               int retryCount) {

        retryCount++;

        if (!result.isConflict()) {
            return result.getData();
        }

        SnapshotsClient.SnapshotConflict conflict = result.getConflict();
        final Snapshot snapshot = conflict.getSnapshot();
        final Snapshot conflictSnapshot = conflict.getConflictingSnapshot();

        ArrayList<Snapshot> snapshotList = new ArrayList<Snapshot>(2);
        snapshotList.add(snapshot);
        snapshotList.add(conflictSnapshot);

        // Display both snapshots to the user and allow them to select the one to resolve.
        selectSnapshotItem(requestCode, snapshotList, conflict.getConflictId(), retryCount);

        // Since we are waiting on the user for input, there is no snapshot available; return null.
        return null;
    }

    private void selectSnapshotItem(int requestCode,
                                    ArrayList<Snapshot> items,
                                    String conflictId,
                                    int retryCount) {

        ArrayList<SnapshotMetadata> snapshotList = new ArrayList<SnapshotMetadata>(items.size());
        for (Snapshot m : items) {
            snapshotList.add(m.getMetadata().freeze());
        }
        Intent intent = new Intent(activity, GoogleSaveActivity.class);
        intent.putParcelableArrayListExtra(GoogleSaveActivity.SNAPSHOT_METADATA_LIST,
                snapshotList);

        intent.putExtra(GoogleSaveActivity.CONFLICT_ID, conflictId);
        intent.putExtra(GoogleSaveActivity.RETRY_COUNT, retryCount);

        activity.startActivityForResult(intent, requestCode);
    }

    private void selectSnapshotItem(int requestCode, ArrayList<SnapshotMetadata> items) {

        ArrayList<SnapshotMetadata> metadataArrayList =
                new ArrayList<SnapshotMetadata>(items.size());
        for (SnapshotMetadata m : items) {
            metadataArrayList.add(m.freeze());
        }
        Intent intent = new Intent(activity, GoogleSaveActivity.class);
        intent.putParcelableArrayListExtra(GoogleSaveActivity.SNAPSHOT_METADATA_LIST,
                metadataArrayList);

        activity.startActivityForResult(intent, requestCode);
    }

    //Originally opened RESOLUTION_POLICY_MANUAL, but we changed it in order to solve conflicts automatically to have stable data
    private Task<SnapshotsClient.DataOrConflict<Snapshot>> waitForClosedAndOpen(final SnapshotMetadata snapshotMetadata) {

        final boolean useMetadata = snapshotMetadata != null && snapshotMetadata.getUniqueName() != null;
        if (useMetadata) {
            Log.i(TAG, "Opening snapshot using metadata: " + snapshotMetadata);
        } else {
            Log.i(TAG, "Opening snapshot using currentSaveName: " + currentSaveName);
        }

        final String filename = useMetadata ? snapshotMetadata.getUniqueName() : currentSaveName;

        return GoogleSaveCoordinator.getInstance()
                .waitForClosed(filename)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleException(e, "There was a problem waiting for the file to close!");
                    }
                })
                .continueWithTask(new Continuation<Result, Task<SnapshotsClient.DataOrConflict<Snapshot>>>() {
                    @Override
                    public Task<SnapshotsClient.DataOrConflict<Snapshot>> then(@NonNull Task<Result> task) throws Exception {
                        Task<SnapshotsClient.DataOrConflict<Snapshot>> openTask = useMetadata
                                ? GoogleSaveCoordinator.getInstance().open(mSnapshotsClient, snapshotMetadata)
                                : GoogleSaveCoordinator.getInstance().open(mSnapshotsClient, filename, true, SnapshotsClient.RESOLUTION_POLICY_LAST_KNOWN_GOOD);
                        return openTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleException(e,
                                        useMetadata
                                                ? activity.getString(R.string.error_opening_metadata)
                                                : activity.getString(R.string.error_opening_filename)
                                );
                            }
                        });
                    }
                });
    }

    private Task<SnapshotsClient.DataOrConflict<Snapshot>> resolveSnapshotConflict(final int requestCode,
                                                                                   final String conflictId,
                                                                                   final int retryCount,
                                                                                   final SnapshotMetadata snapshotMetadata) {

        Log.i(TAG, "Resolving conflict retry count = " + retryCount + " conflictid = " + conflictId);
        return waitForClosedAndOpen(snapshotMetadata)
                .continueWithTask(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<SnapshotsClient.DataOrConflict<Snapshot>>>() {
                    @Override
                    public Task<SnapshotsClient.DataOrConflict<Snapshot>> then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        return GoogleSaveCoordinator.getInstance().resolveConflict(
                                mSnapshotsClient,
                                conflictId,
                                task.getResult().getData())
                                .addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                                    @Override
                                    public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
                                        if (!task.isSuccessful()) {
                                            handleException(
                                                    task.getException(),
                                                    "There was a problem opening a file for resolving the conflict!");
                                            return;
                                        }

                                        Snapshot snapshot = processOpenDataOrConflict(requestCode,
                                                task.getResult(),
                                                retryCount);
                                        Log.d(TAG, "resolved snapshot conflict - snapshot is " + snapshot);
                                        // if there is a snapshot returned, then pass it along to onActivityResult.
                                        // otherwise, another activity will be used to resolve the conflict so we
                                        // don't need to do anything here.
                                        if (snapshot != null) {
                                            Intent intent = new Intent("");
                                            intent.putExtra(GoogleSaveActivity.SNAPSHOT_METADATA, snapshot.getMetadata().freeze());
                                            activity.startActivityForResult(intent, requestCode);
                                        }
                                    }
                                });
                    }
                });
    }

    private void handleException(Exception exception, String details) {
        int status = 0;

        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            status = apiException.getStatusCode();
        }

        String message = activity.getString(R.string.exception_status_error, details, status, exception);
        Log.e(TAG, message);

        // Note that showing a toast is done here for debugging. Your application should
        // resolve the error appropriately to your app.
        if (status == GamesClientStatusCodes.SNAPSHOT_NOT_FOUND) {
            Log.e(TAG, "Error: Snapshot not found");
            Toast.makeText(activity.getBaseContext(), "Error: Snapshot not found",
                    Toast.LENGTH_SHORT).show();
        } else if (status == GamesClientStatusCodes.SNAPSHOT_CONTENTS_UNAVAILABLE) {
            Log.e(TAG, "Error: Snapshot contents unavailable");
            Toast.makeText(activity.getBaseContext(), "Error: Snapshot contents unavailable",
                    Toast.LENGTH_SHORT).show();
        } else if (status == GamesClientStatusCodes.SNAPSHOT_FOLDER_UNAVAILABLE) {
            Log.e(TAG, "Error: Snapshot folder unavailable");
            Toast.makeText(activity.getBaseContext(), "Error: Snapshot folder unavailable.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //------------------------------------- custom listener ----------------------------------------

    public interface Listener {
        void onSuccess();

        void onFailure();
    }
}

