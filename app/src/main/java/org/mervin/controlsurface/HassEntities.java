package org.mervin.controlsurface;

import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mervin.controlsurface.HassConstants.*;

public class HassEntities {

    Surface.PlatformInitializedCallback callback;

    private RequestQueue queue;
    private ArrayList<HassGroupEntity> groups = new ArrayList<>();
    private ArrayList<HassLightEntity> lights = new ArrayList<>();
    private ArrayList<HassSceneEntity> scenes = new ArrayList<>();
    private ArrayList<HassClimateEntity> climates = new ArrayList<>();

    private HashMap<String, HassEntity> entities = new HashMap<>();

    private int subscriberEventId = 1337;
    private int getStatesId = 1338;

    AsyncHttpClient.WebSocketConnectCallback websocketConnectCallback;
    AsyncHttpClient asyncHttpClient;
    WebSocket webSocket;

    public boolean initialized = false;

    public HassEntities(RequestQueue queue) {
        this.queue = queue;
        queue.add(createGetRequest("http://192.168.1.199:8123/api/states"));
        createWebsocket();
    }

    public HassEntity getEntity(String entityId) {
        for (HassEntity entity: groups) {
            if (entity.getId().equals(entityId))
                return entity;
        }
        for (HassEntity entity: lights) {
            if (entity.getId().equals(entityId))
                return entity;
        }
        return null;
    }

    public HassLightEntity getLightEntity(String entityId) {
        for (HassLightEntity entity: lights) {
            if (entity.getId().equals(entityId))
                return entity;
        }
        return null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        if (initialized) {
            // To initialize groups
            getStates();
        }
    }

    public ArrayList<LightControlInterface> getLightControl() {
        ArrayList<LightControlInterface> result = new ArrayList<>();
        for (HassGroupEntity entity: groups) {
            result.add(entity);
        }
        for (HassLightEntity entity: lights) {
            result.add(entity);
        }
        return result;
    }

    public ArrayList<SceneInterface> getScenes() {
        ArrayList<SceneInterface> result = new ArrayList<>();
        for (HassSceneEntity entity: scenes) {
            result.add(entity);
        }
        return result;
    }

    protected ArrayList<HassLightEntity> getLights() {
        return lights;
    }

    private StringRequest createGetRequest(String url){

        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        initEntities(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        return stringRequest;
    }

    private void initEntities(String states) {
        try {
            JSONArray array = new JSONArray(states);
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);

                String entityId = row.getString(ATTR_ENTITY_ID);
                EntityType entityType = HassConstants.getEntityType(entityId);

                if (entityType != EntityType.UNKNOWN) {
                    JSONObject attributes = row.getJSONObject(ATTR);
                    String friendlyName = attributes.getString(ATTR_FRIENDLY_NAME);

                    if (entityType == EntityType.GROUP) {
                        HassGroupEntity entity = new HassGroupEntity(entityId, friendlyName, queue, this);
                        groups.add(entity);
                        entities.put(entityId, entity);
                    }

                    if (entityType == EntityType.LIGHT) {
                        HassLightEntity entity = new HassLightEntity(entityId, friendlyName, queue, this);
                        lights.add(entity);
                        entities.put(entityId, entity);
                    }

                    if (entityType == EntityType.SCENE) {
                        HassSceneEntity entity = new HassSceneEntity(entityId, friendlyName, queue, this);
                        scenes.add(entity);
                        entities.put(entityId, entity);
                    }

                    if (entityType == EntityType.CLIMATE) {
                        HassClimateEntity entity = new HassClimateEntity(entityId, friendlyName, queue, this);
                        climates.add(entity);
                        entities.put(entityId, entity);
                    }

                }
            }
            initialized();
        } catch (Throwable t) {

        }
    }

    private void initialized() {
        initialized = true;
        callback.platformInitialized();
    }

    private void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
        this.webSocket.setStringCallback(getStringCallback());
        subscribeStates();
        getStates();
    }

    private WebSocket.StringCallback getStringCallback() {
        return new WebSocket.StringCallback(){
            public void onStringAvailable(String s){
                try {
                    JSONObject row = new JSONObject(s);
                    if (row.getInt("id") == subscriberEventId) {
                        JSONObject event = row.getJSONObject("event");
                        JSONObject data = event.getJSONObject("data");
                        JSONObject newState = data.getJSONObject("new_state");
                        HassEntity entity = getEntity(data.getString(ATTR_ENTITY_ID));
                        if (entity != null) {
                            entity.processState(newState);
                        }
                    }
                    if (row.getInt("id") == getStatesId) {
                        JSONArray array = row.getJSONArray("result");
                        for(int i = 0, count = array.length(); i< count; i++)
                        {
                            JSONObject stateRow = array.getJSONObject(i);
                            HassEntity entity = getEntity(stateRow.getString(ATTR_ENTITY_ID));
                            if (entity != null) {
                                entity.processState(stateRow);
                            }
                        }
                        setInitialized(true);
                    }

                } catch (JSONException e) {}
            }
        };
    }

    private void subscribeStates() {
        try {
            JSONObject subscribeStates = new JSONObject();
            subscribeStates.put("id", subscriberEventId);
            subscribeStates.put("type", "subscribe_events");
            subscribeStates.put("event_type", "state_changed");
            webSocket.send(subscribeStates.toString());
        } catch (JSONException e) {}
    }

    private void getStates() {
        try {
            JSONObject getStates = new JSONObject();
            getStates.put("id", getStatesId);
            getStates.put("type", "get_states");
            webSocket.send(getStates.toString());
        } catch (JSONException e) {}
    }

    private void createWebsocket() {
        websocketConnectCallback = new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                setWebSocket(webSocket);
            }
        };
        asyncHttpClient = AsyncHttpClient.getDefaultInstance();
        asyncHttpClient.websocket("ws://192.168.1.199:8123/api/websocket", null, websocketConnectCallback);
    }



}




