package dev.brickwedde.ccApiAndroid;

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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CcApi {
    public String endpoint;
    private String sessionKey;
    private String token;
    private RequestQueue queue;

    public CcApi(String endpoint, String sessionKey, Context context) {
        this.endpoint = endpoint;
        this.sessionKey = sessionKey;
        queue = Volley.newRequestQueue(context);
    }

    public void setSessionKey(String sessionkey, Context context) {
        this.sessionKey = sessionkey;
    }

    public void setFCMToken(Context context, String token) {
        this.token = token;
        updateFCMToken(context);
    }

    public void updateFCMToken(Context context) {
        call(new Handler(context.getMainLooper()), new Callback() {
            public void then(JSONObject o, JSONArray a) throws Exception {
            }
            public void catchy(Exception e, int status, String content) {
            }
            public void finallie() {
            }
        }, "updateFCMToken", token);
    }

    public static interface Callback {
        void then(JSONObject o, JSONArray a) throws Exception;
        void catchy(Exception e, int status, String content);
        void finallie();
    }

    public void call(final Handler h, Callback cb, String function, Object... args) {
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
                                } finally {
                                    cb.finallie();
                                }
                            }
                        }, 0);
                    } catch (Exception e) {
                        h.postDelayed(new Runnable() {
                            public void run() {
                                try {
                                    cb.catchy(e, 200, response);
                                } finally {
                                    cb.finallie();
                                }
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
                return s.getBytes(StandardCharsets.UTF_8);
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
