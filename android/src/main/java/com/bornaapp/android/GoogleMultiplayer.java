package com.bornaapp.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bornaapp.androidlib.R;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationCallback;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.OnRealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateCallback;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mehdi on 4/18/2018.
 */

public class GoogleMultiplayer {

    private Activity activity; //todo: this class must not use R & android resources if supposed to be independent!

    // tag for debug logging
    private static final String TAG = "Mehdi(Multiplayer)";

    // Request codes for the UIs that we show with startActivityForResult:
    private final static int RC_SELECT_PLAYERS = 10000;
    private final static int RC_INVITATION_INBOX = 10001;
    private final static int RC_WAITING_ROOM = 10002;

    // Client used to interact with the real time multiplayer system.
    private RealTimeMultiplayerClient mRealTimeMultiplayerClient = null;

    // Client used to interact with the Invitation system.
    private InvitationsClient mInvitationsClient = null;
    private final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    String mRoomId = null;

    // Holds the configuration of the current room.
    private RoomConfig mRoomConfig;

    // The participants in the currently active game
    public ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    public String mMyId = null;
    private String mPlayerId;

    private InvitationCallback mInvitationCallback;

    private Listener mListener = null;

    boolean networkOk = false;

    public GoogleMultiplayer(Activity activity) {
        this.activity = activity;
    }

    //---------------------------------- Handle Activity Result ------------------------------------

    public void HandleActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RC_SELECT_PLAYERS) {
            if (mListener != null)
                mListener.onSelectplayersClientCollapsed();
            // we got the result from the "select players" UI -- ready to create the room
            handleSelectPlayersClientResult(resultCode, intent);

        } else if (requestCode == RC_INVITATION_INBOX) {
            // we got the result from the "select invitation" UI (invitation inbox). We're
            // ready to accept the selected invitation:
            handleInvitationInboxResult(resultCode, intent);

        } else if (requestCode == RC_WAITING_ROOM) {
            // we got the result from the "waiting room" UI.
            if (resultCode == Activity.RESULT_OK) {
                disableInvitations();
                // ready to start playing
                if (mListener != null)
                    mListener.onReadyToPlay();

            } else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // player indicated that they want to leave the room
                leaveRoom();

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Dialog was cancelled (user pressed back key, for instance). In our game,
                // this means leaving the room too. In more elaborate games, this could mean
                // something else (like minimizing the waiting room UI).
                leaveRoom();
            }
        }
    }

    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersClientResult(int response, Intent data) {
        //return if not connected
        if (mRealTimeMultiplayerClient == null)
            return;

        if (response != Activity.RESULT_OK) {
            //Log.w(TAG, "*** select players UI cancelled, " + response);
            //switchToMainScreen();
            return;
        }

        //Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        //Log.d(TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
        }

        // create the room
        //Log.d(TAG, "Creating room...");

        mRoomConfig = RoomConfig.builder(mRoomUpdateCallback)
                .addPlayersToInvite(invitees)
                .setOnMessageReceivedListener(mOnRealTimeMessageReceivedListener)
                .setRoomStatusUpdateCallback(mRoomStatusUpdateCallback)
                .setAutoMatchCriteria(autoMatchCriteria).build();

        mRealTimeMultiplayerClient.create(mRoomConfig);
        //Log.d(TAG, "Room created, waiting for it to be ready...");

        if (mListener != null)
            mListener.onCreateRoomRequested();
    }

    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            //Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            //switchToMainScreen();
            return;
        }

        //Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation invitation = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        if (invitation != null) {
            acceptInvitation(invitation.getInvitationId());
        }
    }

    //------------------------------------ Show/Hide clients ---------------------------------------

    void showInvitationInbox() {
        //return if not connected
        if (mInvitationsClient == null)
            return;

        mInvitationsClient.getInvitationInboxIntent().addOnSuccessListener(
                new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        activity.startActivityForResult(intent, RC_INVITATION_INBOX);
                    }
                }
        ).addOnFailureListener(createFailureListener("There was a problem getting the inbox."));
    }

    public void showSelectPlayersClient() {
        //return if not connected
        if (mRealTimeMultiplayerClient == null)
            return;

        // show list of invitable players

        mRealTimeMultiplayerClient.getSelectOpponentsIntent(MIN_OPPONENTS, MAX_OPPONENTS).addOnSuccessListener(
                new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        activity.startActivityForResult(intent, RC_SELECT_PLAYERS);
                    }
                }
        ).addOnFailureListener(createFailureListener("There was a problem selecting opponents."));
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    private void showWaitingRoom(Room room) {
        //return if not connected
        if (mRealTimeMultiplayerClient == null)
            return;

        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        mRealTimeMultiplayerClient.getWaitingRoomIntent(room, MIN_PLAYERS)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        // show waiting room UI
                        activity.startActivityForResult(intent, RC_WAITING_ROOM);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem getting the waiting room!"));
    }

    //---------------------------------------- Connection ------------------------------------------

    public void connect(GoogleSignInAccount googleSignInAccount) {

        if (mRealTimeMultiplayerClient != null && mInvitationsClient != null)
            return;

        mRealTimeMultiplayerClient = Games.getRealTimeMultiplayerClient(activity, googleSignInAccount);
        mInvitationsClient = Games.getInvitationsClient(activity, googleSignInAccount);

        enableInvitations(); //<------not working right now

        // get the playerId from the PlayersClient
        PlayersClient playersClient = Games.getPlayersClient(activity, googleSignInAccount);
        playersClient.getCurrentPlayer()
                .addOnSuccessListener(new OnSuccessListener<Player>() {
                    @Override
                    public void onSuccess(Player player) {
                        mPlayerId = player.getPlayerId();
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem getting the player id!"));

        // get the invitation from the connection hint
        // Retrieve the TurnBasedMatch from the connectionHint
        GamesClient gamesClient = Games.getGamesClient(activity, googleSignInAccount);
        gamesClient.getActivationHint()
                .addOnSuccessListener(new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle hint) {
                        if (hint != null) {
                            Invitation invitation =
                                    hint.getParcelable(Multiplayer.EXTRA_INVITATION);

                            if (invitation != null && invitation.getInvitationId() != null) {
                                // retrieve and cache the invitation ID
                                //Log.d(TAG, "onConnected: connection hint has a room invite!");
                                acceptInvitation(invitation.getInvitationId());
                            }
                        }
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem getting the activation hint!"));
    }

    public void disconnect() {
        leaveRoom();
        mRealTimeMultiplayerClient = null;
        mInvitationsClient = null;
    }

    //-------------------------------------- Invitations -------------------------------------------

    private void enableInvitations() {
        //Log.i(TAG, "Invitations registered");
        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        if (mInvitationsClient != null)
            mInvitationsClient.registerInvitationCallback(mInvitationCallback);
    }

    private void disableInvitations() {
        //Log.i(TAG, "Invitations unregistered");
        // unregister our listeners.  They will be re-registered via onResume->signInSilently->onConnected.
        if (mInvitationsClient != null)
            mInvitationsClient.unregisterInvitationCallback(mInvitationCallback);
    }

    // Accept the given invitation.
    public void acceptInvitation(String invitationId) {
        //return if not connected
        if (mRealTimeMultiplayerClient == null)
            return;

        // accept the invitation
        //Log.d(TAG, "Accepting invitation: " + invitationId);

        mRoomConfig = RoomConfig.builder(mRoomUpdateCallback)
                .setInvitationIdToAccept(invitationId)
                .setOnMessageReceivedListener(mOnRealTimeMessageReceivedListener)
                .setRoomStatusUpdateCallback(mRoomStatusUpdateCallback)
                .build();

        mRealTimeMultiplayerClient.join(mRoomConfig)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Log.d(TAG, "Room Joined Successfully!");
                    }
                });

        if (mListener != null)
            mListener.onCreateRoomRequested();
    }

    public void declineInvitation(String invitationId) {
        //return if not connected
        if (mRealTimeMultiplayerClient == null)
            return;

        mRealTimeMultiplayerClient.declineInvitation(invitationId)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Log.d(TAG, "invitation declined!");
                    }
                });
    }

    public void setInvitationCallback(InvitationCallback invitationCallback) {
        mInvitationCallback = invitationCallback;
    }

    //-------------------------------------- Room --------------------------------------------------

    // Leave the room.
    public void leaveRoom() {
        //return if not connected
        if (mRealTimeMultiplayerClient == null)
            return;

        //Log.d(TAG, "Leaving room...");
        if (mRoomId != null) {
            mRealTimeMultiplayerClient.leave(mRoomConfig, mRoomId)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mRoomId = null;
                            mRoomConfig = null;
                        }
                    });
        }
        enableInvitations();
    }

    private RoomUpdateCallback mRoomUpdateCallback = new RoomUpdateCallback() {

        // this is the first callback after we request building a room
        // Called when a room has been created(even without any opponents)
        @Override
        public void onRoomCreated(int statusCode, Room room) {
            //Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");

            if (statusCode != GamesCallbackStatusCodes.OK) {
                Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
                if (mListener != null) {
                    mListener.onError(activity.getString(R.string.exception_multiplayer));
                    mListener.onLeftRoom();
                }
                return;
            }

            if (mListener != null)
                mListener.onEmptyRoomCreated();

            // save room ID so we can leave cleanly before the game starts.
            if (mRoomId == null) {
                mRoomId = room.getRoomId();
            }

            // show the waiting room UI
            showWaitingRoom(room);
        }

        // Called when room is fully connected.
        @Override
        public void onRoomConnected(int statusCode, Room room) {
            //Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
            if (statusCode != GamesCallbackStatusCodes.OK) {
                Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
                if (mListener != null) {
                    mListener.onError(activity.getString(R.string.exception_multiplayer));
                    mListener.onLeftRoom();
                }
                return;
            }
            updateParticipants(room);
        }

        @Override
        public void onJoinedRoom(int statusCode, Room room) {
            //todo: what if we leave in the middle of these events?
            //Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
            if (statusCode != GamesCallbackStatusCodes.OK) {
                Log.e(TAG, "*** Error: onJoinedRoom, status " + statusCode); //made an exception here once!
                if (mListener != null) {
                    mListener.onError(activity.getString(R.string.exception_multiplayer));
                    mListener.onLeftRoom();
                }
                return;
            }

            // show the waiting room UI
            showWaitingRoom(room);
        }

        // Called when we've successfully left the room (this happens a result of voluntarily leaving
        // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
        @Override
        public void onLeftRoom(int statusCode, @NonNull String roomId) {
            // we have left the room; return to main screen.
            //Log.d(TAG, "onLeftRoom, code " + statusCode);
            //switchToMainScreen();
            if (mListener != null)
                mListener.onLeftRoom();
        }
    };

    private RoomStatusUpdateCallback mRoomStatusUpdateCallback = new RoomStatusUpdateCallback() {
        // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
        // is connected yet).
        @Override
        public void onConnectedToRoom(Room room) {
            //get participants and my ID:
            mParticipants = room.getParticipants();
            mMyId = room.getParticipantId(mPlayerId);

            // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
            if (mRoomId == null) {
                mRoomId = room.getRoomId();
            }

            //Log.d(TAG, "My ID:(" + mMyId + ") CONNECTED TO " + "Room ID:(" + mRoomId + ")");
        }

        // Called when we get disconnected from the room.
        @Override
        public void onDisconnectedFromRoom(Room room) {
            mRoomId = null;
            mRoomConfig = null;
            //Log.d(TAG, "Disconnected From Room");
            if (mListener != null)
                mListener.onLeftRoom();
        }

        // We treat most of the room update callbacks in the same way: we update our list of
        // participants and update the display. In a real game we would also have to check if that
        // change requires some action like removing the corresponding player avatar from the screen,
        // etc.
        @Override
        public void onPeerDeclined(Room room, @NonNull List<String> arg1) {
            updateParticipants(room);
        }

        @Override
        public void onPeerInvitedToRoom(Room room, @NonNull List<String> arg1) {
            updateParticipants(room);
        }

        @Override
        public void onP2PDisconnected(@NonNull String participant) {
        }

        @Override
        public void onP2PConnected(@NonNull String participant) {
        }

        @Override
        public void onPeerJoined(Room room, @NonNull List<String> arg1) {
            updateParticipants(room);
        }

        @Override
        public void onPeerLeft(Room room, @NonNull List<String> peersWhoLeft) {
            updateParticipants(room);
            //Log.d(TAG, "Peer Left");
            if (mListener != null)
                mListener.onPeerLeft();
        }

        @Override
        public void onRoomAutoMatching(Room room) {
            updateParticipants(room);
        }

        @Override
        public void onRoomConnecting(Room room) {
            updateParticipants(room);
        }

        @Override
        public void onPeersConnected(Room room, @NonNull List<String> peers) {
            updateParticipants(room);
        }

        @Override
        public void onPeersDisconnected(Room room, @NonNull List<String> peers) {
            updateParticipants(room);
            //Log.d(TAG, "Peer Disconnected");
            if (mListener != null)
                mListener.onPeerLeft();
        }
    };

    private void updateParticipants(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
    }

    //--------------------------------------- SYNC Data --------------------------------------------

    // Called when we receive a real-time message from the network.
    // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
    // indicating
    // whether it's a final or interim score. The second byte is the score.
    // There is also the
    // 'S' message, which indicates that the game should start.
    private OnRealTimeMessageReceivedListener mOnRealTimeMessageReceivedListener = new OnRealTimeMessageReceivedListener() {
        @Override
        public void onRealTimeMessageReceived(@NonNull RealTimeMessage realTimeMessage) {
            byte[] buf = realTimeMessage.getMessageData();
            String sender = realTimeMessage.getSenderParticipantId();
//            Log.d(TAG, "Message received:" + new String(buf));

            String netMessage = new String(buf);

            if (mListener != null)
                mListener.onMessageReceived(sender, netMessage);
        }
    };

    /**
     * @param sMessage   json-formatted string message
     * @param isReliable true for sending non-time-sensitive but traceable data.
     */
    public void broadcastMessage(String sMessage, boolean isReliable) {

        //return if not connected
        if (mRealTimeMultiplayerClient == null || mRoomId == null)
            return;

        //prepare message array
        byte[] bMessage = sMessage.getBytes();

        // Send message to every other participant.
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId)) {
                continue;
            }
            if (p.getStatus() != Participant.STATUS_JOINED) {
                continue;
            }
            if (isReliable) {
                // Data delivery, integrity, and ordering are guaranteed but might have high latency.
                // Reliable messaging is suitable for sending non-time-sensitive data.
                mRealTimeMultiplayerClient.sendReliableMessage(bMessage,
                        mRoomId, p.getParticipantId(), new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
                            @Override
                            public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
                                //Log.d(TAG, "RealTime message sent");
                                //Log.d(TAG, "  statusCode: " + statusCode + " (" + GamesCallbackStatusCodes.getStatusCodeString(statusCode) + ")");
                                //Log.d(TAG, "  tokenId: " + tokenId);
                                //Log.d(TAG, "  recipientParticipantId: " + recipientParticipantId);
                                networkOk = (statusCode == GamesCallbackStatusCodes.OK);
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                            @Override
                            public void onSuccess(Integer tokenId) {
                                //Log.d(TAG, "Created a reliable message with tokenId: " + tokenId);
                            }
                        });
            } else {
                // Data is sent only once 'fire-and-forget' with low latency
                // no guarantee of data delivery or data arriving in order. However, integrity is guaranteed.
                // Unreliable messaging is suitable for sending data that is time-sensitive.
                //Log.d(TAG, "sending unreliable message:" + sMessage);
                mRealTimeMultiplayerClient.sendUnreliableMessage(bMessage, mRoomId, p.getParticipantId());
            }
        }
    }

    //---------------------------------- accessory methods -----------------------------------------

    public void quickGame() {
        //return if not connected
        if (mRealTimeMultiplayerClient == null)
            return;

        // quick-start a game with a randomly selected opponent
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS, MAX_OPPONENTS, 0);

        mRoomConfig = RoomConfig.builder(mRoomUpdateCallback)
                .setOnMessageReceivedListener(mOnRealTimeMessageReceivedListener)
                .setRoomStatusUpdateCallback(mRoomStatusUpdateCallback)
                .setAutoMatchCriteria(autoMatchCriteria)
                .build();
        mRealTimeMultiplayerClient.create(mRoomConfig);

        if (mListener != null)
            mListener.onCreateRoomRequested();
    }

    public boolean isNetworkOK() {
        return networkOk;
    }

    //--------------------------------- custom UiListener --------------------------------------------

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        /**
         * no matter what method we use to join a multiplayer room,
         * it always starts here
         */
        void onCreateRoomRequested();

        void onEmptyRoomCreated();

        void onReadyToPlay();

        void onLeftRoom();

        void onPeerLeft();

        void onMessageReceived(String senderID, String netMessage);

        void onSelectplayersClientCollapsed();

        void onError(String msg);

    }

    //----------------------------------- other methods --------------------------------------------

    private OnFailureListener createFailureListener(final String string) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mListener.onError(e.getMessage());
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
