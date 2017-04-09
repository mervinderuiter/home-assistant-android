package org.mervin.controlsurface;

import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.mervin.controlsurface.HassConstants.*;

public class HassSceneEntity extends HassEntity implements SceneInterface {

    public HassSceneEntity(String entityId, String friendlyName, RequestQueue queue, HassEntities hassEntities) {
        super(entityId, friendlyName, queue, hassEntities);
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