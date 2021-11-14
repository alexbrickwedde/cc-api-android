package dev.brickwedde.curacaomanagement;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.HashMap;
import java.util.Map;

public class CcApi {
    private String endpoint;
    private String sessionKey;
    private RequestQueue queue;

    private static final long SYNC_FREQUENCY = 60 * 60;  // 1 hour (in seconds)
    private static final String CONTENT_AUTHORITY = "dev.brickwedde.curacaomanagement.ccapi";
    private static final String PREF_SETUP_COMPLETE = "setup_complete";
    // Value below must match the account type specified in res/xml/syncadapter.xml
    public static final String ACCOUNT_TYPE = "dev.brickwedde.curacaomanagement.ccapi.account";

    CcApi(String endpoint, Context context) {
        this.endpoint = endpoint;
        queue = Volley.newRequestQueue(context);
    }

    public void setSessionKey(String sessionkey, Context context) {
        this.sessionKey = sessionkey;
        saveSessionKeyToAccount(context, sessionKey);
    }

    public boolean hasSessionKey(Context context) {
        if (sessionKey == null) {
            sessionKey = getSessionKeyFromAccount(context);
        }
        return sessionKey != null;
    }

    public static String getSessionKeyFromAccount(Context context) {
        try {
            Account account = GenericAccountService.GetAccount(ACCOUNT_TYPE);
            AccountManager accountManager =
                    (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
            return accountManager.getPassword(account);
        } catch (Exception e) {
        }
        return null;
    }

    public static void saveSessionKeyToAccount(Context context, String sessionKey) {
        boolean newAccount = false;
        boolean setupComplete = PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = GenericAccountService.GetAccount(ACCOUNT_TYPE);
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, sessionKey, null)) {
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(
                    account, CONTENT_AUTHORITY, new Bundle(),SYNC_FREQUENCY);
            newAccount = true;
        }

        if (newAccount || !setupComplete) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(PREF_SETUP_COMPLETE, true).commit();
        }
    }

    public static void gotoLogin(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        activity.startActivity(intent);
    }

    public static interface Callback {
        void then(JSONObject o, JSONArray a) throws Exception;
        void catchy(Exception e, int status, String content);
    }

    void call(final Handler h, Callback cb, String function, Object ...args) {
        String url = this.endpoint + "/" + function;

        JSONArray a = new JSONArray();
        for(Object o : args) {
            a.put(o);
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        final JSONObject o;
                        final JSONArray a;
                        if(response.startsWith("[")) {
                            a = new JSONArray(response);
                            o = null;
                        } else if(response.startsWith("{")) {
                            a = null;
                            o = new JSONObject(response);
                        } else {
                            a = null;
                            o = null;
                        }
                        h.postDelayed(new Runnable() {
                            public void run() {
                                try {
                                    cb.then(o, a);
                                } catch (Exception e) {
                                    cb.catchy(e, 200, response);
                                }
                            }
                        }, 0);
                    } catch (Exception e) {
                        h.postDelayed(new Runnable() {
                            public void run() {
                                cb.catchy(e, 200, response);
                            }
                        }, 0);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    h.postDelayed(new Runnable() {
                        public void run() {
                            cb.catchy(error, error.networkResponse != null ? error.networkResponse.statusCode : -1, new String(error.networkResponse != null ? error.networkResponse.data : "".getBytes()));
                        }
                    }, 0);
                }
            }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String s = a.toString();
                return s.getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> h = new HashMap<>();
                if (sessionKey != null) {
                    h.put("Authorization", "Bearer " + sessionKey);
                }
                return h;
            }
        };
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }
}
