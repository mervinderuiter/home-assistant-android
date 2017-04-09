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

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void setAutoUpdate(boolean autoUpdate) { }

    @Override
    public void getState() { }

    public void turnOn() {
        sendSwitchRequest(COMMAND_ON);
    }

    private void sendSwitchRequest(String action) {

        String url = base_url + URL_SERVICES + URL_DOMAIN_SCENE + action;

        try {
            JSONObject payload = new JSONObject();
            payload.put(ATTR_ENTITY_ID, entityName);
            createPostRequest(url, payload.toString().getBytes());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }


}