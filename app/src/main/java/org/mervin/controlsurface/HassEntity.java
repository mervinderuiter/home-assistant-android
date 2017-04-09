package org.mervin.controlsurface;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static org.mervin.controlsurface.HassConstants.*;

public class HassEntity {

    protected EntityType entityType;
    protected String entityName;
    protected String state = STATE_OFF;

    protected boolean autoUpdate = false;

    protected RequestQueue queue;
    protected String base_url;
    protected String entity_url;
    protected String friendlyName;
    protected Timer timer;

    protected HassEntities hassEntities;

    public HassEntity(String entityId, String friendlyName, RequestQueue queue, HassEntities hassEntities) {
        this.entityName = entityId;
        this.queue = queue;
        this.base_url = "http://192.168.1.199:8123/api/";
        this.entity_url = base_url + URL_STATES + entityId;
        this.friendlyName = friendlyName;
        this.hassEntities = hassEntities;
        entityType = getEntityType(entityId);
        getState();
        startTimer();
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        startTimer();
        if (!this.autoUpdate && timer != null) {
            stopTimer();
        }
    }

    public boolean isGroup(){
        return false;
    }

    public ArrayList<LightControlInterface> getChildEntities() {
        return null;
    }

    public String getId() {
        return entityName;
    }

    public String getName() {
        return friendlyName;
    }

    public void pause() {
        if (autoUpdate) {
            stopTimer();
        }
    }

    public void resume() {
        if (autoUpdate) {
            getState();
            startTimer();
        }
    }

    private void startTimer() {
        if (autoUpdate) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getState();
                }
            }, 0, 5000);
        }
    }

    private void stopTimer() {
        if (entityType != EntityType.SCENE) {
            timer.cancel();
        }
    }

    public void getState() {
        createGetStateRequest(entity_url);
    }

    public void createPostRequest(final String url, final byte[] payload) {

        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        processResult(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return payload;
            }
        };
        queue.add(stringRequest);
    }

    private void processResult(String states) {

        try {
            try {
                JSONObject row = new JSONObject(states);
                //processState(row);
            } catch (Exception e) {

                JSONArray array = new JSONArray(states);
                for(int i = 0, count = array.length(); i< count; i++)
                {
                    try {
                        JSONObject row = array.getJSONObject(i);
                        //Find the corresponding entity and update state
                        HassEntity entity = hassEntities.getEntity(row.getString(ATTR_ENTITY_ID));
                        if (entity != null) {}
                            //entity.processState(row);
                    }
                    catch (Throwable throwable) {
                        throw throwable;
                    }
                }

            }
        } catch (Exception e) {
            Log.e("HassEntity", "exception", e);
        }
    }

    public void processState(JSONObject row) {
        //To be implemented from derived class
    }

    private void createGetStateRequest(final String url){

        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        processResult(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        queue.add(stringRequest);
    }



}
