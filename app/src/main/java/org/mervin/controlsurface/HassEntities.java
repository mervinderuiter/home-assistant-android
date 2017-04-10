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
    private int callServiceId = 1339;

    AsyncHttpClient.WebSocketConnectCallback websocketConnectCallback;
    AsyncHttpClient asyncHttpClient;
    WebSocket webSocket;

    public boolean initialized = false;

    public HassEntities(RequestQueue queue) {
        this.queue = queue;
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



    private void initEntities(JSONArray array) {
        try {
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
        } catch (Throwable t) {

        }
    }

    public void onPause() {
        webSocket.close();
        webSocket = null;
    }

    public void onResume() {
        if (webSocket != null) {
            webSocket.end();
            webSocket.close();
            webSocket = null;
        }
        createWebsocket();
    }

    private void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
        this.webSocket.setStringCallback(getStringCallback());
        subscribeStates();
        getStates();
    }

    private void dispatchState(JSONObject state) {
        try {
            HassEntity entity = getEntity(state.getString(ATTR_ENTITY_ID));
            if (entity != null) {
                entity.processState(state);
            }
        } catch (JSONException e) {}
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
                        dispatchState(newState);
                    }
                    if (row.getInt("id") == getStatesId) {
                        JSONArray array = row.getJSONArray("result");
                        if (!initialized) {
                            initEntities(array);
                        }
                        for(int i = 0, count = array.length(); i< count; i++)
                        {
                            JSONObject stateRow = array.getJSONObject(i);
                            dispatchState(stateRow);
                        }
                        if (!initialized) {
                            for (HassGroupEntity group : groups) {
                                group.setChildEntities();
                            }
                            initialized = true;
                            callback.platformInitialized();
                        }
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

    public void callService(String domain, String service, JSONObject service_data) {
        try {
            callServiceId += 1;
            JSONObject callService = new JSONObject();
            callService.put("id", callServiceId);
            callService.put("type", "call_service");
            callService.put("domain", domain);
            callService.put("service", service);
            if (service_data != null) {
                callService.put("service_data", service_data);
            }
            webSocket.send(callService.toString());
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




