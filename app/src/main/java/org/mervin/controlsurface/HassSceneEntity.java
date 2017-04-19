package org.mervin.controlsurface;

import android.graphics.Color;

import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.mervin.controlsurface.HassConstants.*;

public class HassSceneEntity extends HassEntity implements SceneInterface {

    public HassSceneEntity(String entityId, String friendlyName, String icon, int color, HassEntities hassEntities) {
        super(entityId, friendlyName, icon, color, hassEntities);
        this.state = STATE_ON;
    }

    public void turnOn() {
        sendSwitchRequest(COMMAND_ON);
    }

    private void sendSwitchRequest(String action) {

        try {
            JSONObject payload = new JSONObject();
            payload.put(ATTR_ENTITY_ID, entityName);
            hassEntities.callService(DOMAIN_SCENE, action, payload);
        }
        catch (JSONException e) {

            e.printStackTrace();
        }
    }


}