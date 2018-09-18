package com.bornaapp.android;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;

import java.util.ArrayList;

/**
 * Created by Mehdi on 2/13/2018.
 * a wrapper class encapsulating in-app purchase from Bazaar
 */

public class BazaarIAP {

    // <my addition>
    private String TAG = "Mehdi(BazaarIAP)";
    private static final int RC_REQUEST = 11001;
    private String publicKey = "";
    // </my addition>

    private Activity activity;

    public ArrayList<String> ConsumableSKUs = new ArrayList<String>();

    // The helper object
    private IabHelper mHelper;

    public BazaarIAP(Activity activity, String publicKey) {
        this.activity = activity;
        this.publicKey = publicKey;
    }

    //--------------------------------------- Life-Cycle -------------------------------------------

    public boolean HandleActivityResult(int requestCode, int resultCode, Intent intent) {
        if (mHelper == null)
            return false;
        // Pass on the activity result to the helper for handling
        return mHelper.handleActivityResult(requestCode, resultCode, intent);
        // returns false if activity results not related to
        // in-app billing...
    }

    public void Init() {
        // You can find your public key in your Bazaar console, in the Dealers section.
        // It is recommended to add more security than just pasting it in your source code;
        mHelper = new IabHelper(activity.getApplicationContext(), publicKey);

        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    Log.d(TAG, "Problem setting up In-app Billing: " + result);
                }

                // IAB is fully set up!
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    public void Destroy() {
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    //--------------------------------------- Public methods ---------------------------------------

    public void Purchase(String SKU, Listener listener) {
        mListener = listener;

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";

        mHelper.launchPurchaseFlow(activity, SKU, RC_REQUEST, mPurchaseFinishedListener, payload);
    }

    // ResultListener that's called when we finish querying the itemList and subscriptions we own
    private IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                //complain("Failed to inventory: " + result);
                Log.d(TAG, "inventory finished with Error.");
                return;
            }

            Log.d(TAG, "inventory finished successfully.");

            /*
             * Check for itemList we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Check for consumable item delivery -- if we own some, we should consume immediately
            for (int i = 0; i < ConsumableSKUs.size(); i++) {
                String SKU = ConsumableSKUs.get(i);
                Purchase purchase = inventory.getPurchase(SKU);
                if (purchase != null && verifyDeveloperPayload(purchase)) {
                    Log.d(TAG, "Consuming remaining " + SKU);
                    mHelper.consumeAsync(inventory.getPurchase(SKU), mConsumeFinishedListener);
                    return;
                }
            }
//            updateUi();
//            setWaitScreen(false);
//            Log.d(TAG, "Initial inventory  finished; enabling main UI.");
        }
    };

    // Callback for when a purchase is finished
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
//                setWaitScreen(false);
                if (mListener != null)
                    mListener.onFailure(result);
                return;
            }

            if (!verifyDeveloperPayload(purchase)) {
//                setWaitScreen(false);
                Log.d(TAG, "Purchase error: Authenticity verification failed.");
                return;
            }

            // Check for consumable item delivery -- if we own some, we should consume immediately
            for (int i = 0; i < ConsumableSKUs.size(); i++) {
                String SKU = ConsumableSKUs.get(i);
                if (purchase.getSku().equals(SKU)) {
                    Log.d(TAG, "Purchase is " + SKU + ". Starting consumption.");
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                }
            }

            if (mListener != null)
                mListener.onSuccess();
        }
    };

    // Called when consumption is complete
    private IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null)
                return;

            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects
                // of the item in our MyGame world's logic
                Log.d(TAG, "Consumption successful. Provisioning.");

                for (int i = 0; i < ConsumableSKUs.size(); i++) {
                    String SKU = ConsumableSKUs.get(i);
                    if (purchase.getSku().equals(SKU)) {
                        // saveData();
                    }
                }
            } else {
                Log.d(TAG, "Consumption error");
            }
//            updateUi();
//            setWaitScreen(false);
            Log.d(TAG, "End consumption flow.");
        }
    };

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that itemList purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    //-------------------------------------- Listener ----------------------------------------------
    public interface Listener {
        void onSuccess();

        void onFailure(IabResult result);
    }

    private Listener mListener;
}
