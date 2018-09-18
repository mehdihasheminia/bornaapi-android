package com.bornaapp.android.database;

import android.app.Activity;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mehdi on 3/2/2018.
 * https://www.tutorialspoint.com/android/android_php_mysql.htm
 */

public class SqlDatabase {

    private Activity mActivity;

    private static final String TAG = "Mehdi(SqlDatabase)";

    private final String URL_ROOT = "http://www.bornaapp.com/php/breakarecord2/"; //78.157.60.17 todo: should be an argument
    private final String URL_REGISTER = URL_ROOT + "registerUser.php";
    private final String URL_GET_ALL = URL_ROOT + "getAll.php";
    private final String URL_QUERY_BY_NAME = URL_ROOT + "getByName.php";
    private final String URL_QUERY_BY_DATE = URL_ROOT + "getByDate.php";

    public SqlDatabase(Activity _activity) {
        mActivity = _activity;
    }

    public void getAll(final ResultListener listener) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_GET_ALL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.i(TAG, "Query Success: " + response);
                        listener.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Query error: " + error.getMessage());
                        listener.onFailure();
                    }
                });

        RequestHandler.getInstance(mActivity).addToRequestQueue(stringRequest);
    }

    public void queryByName(final String value) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_QUERY_BY_NAME,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.i(TAG, "Query Success: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Query error: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("name", value);
                return params;
            }
        };

        RequestHandler.getInstance(mActivity).addToRequestQueue(stringRequest);
    }

    public void queryByDate(final String value, final ResultListener listener) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_QUERY_BY_DATE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.i(TAG, "Query Success: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        listener.onFailure();
                        Log.e(TAG, "Query error: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("date", value);
                return params;
            }
        };

        RequestHandler.getInstance(mActivity).addToRequestQueue(stringRequest);
    }

    public void write(String sData) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_REGISTER,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.i(TAG, "Write Success: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Write error: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("name", "n_" + Double.toString(Math.random()).substring(0, 5));  // fill in dummy data
                params.put("score", "s_" + Double.toString(Math.random()).substring(0, 5)); //key, value
                params.put("reward", "r_" + Double.toString(Math.random()).substring(0, 5));
                return params;
            }
        };

        RequestHandler.getInstance(mActivity).addToRequestQueue(stringRequest);
    }

    public interface ResultListener {
        void onSuccess(String response);

        void onFailure();
    }

}
