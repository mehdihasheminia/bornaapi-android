package com.bornaapp.android.database;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Hashemi on 17/03/2018.
 * Sets up a single instance(singleton) of RequestQueue that will last the lifetime of your app.
 */

public class RequestHandler {
    private static RequestHandler sInstance;
    private RequestQueue mRequestQueue;

    private static Context sCtx;

    private RequestHandler(Context context) {
        sCtx = context;
        mRequestQueue = getRequestQueue();
    }

    static synchronized RequestHandler getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RequestHandler(context);
        }
        return sInstance;
    }

    RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(sCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}