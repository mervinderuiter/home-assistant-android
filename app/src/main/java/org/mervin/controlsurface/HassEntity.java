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




    public void processState(JSONObject row) {
        //To be implemented from derived class
    }





}
