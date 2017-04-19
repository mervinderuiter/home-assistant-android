package org.mervin.controlsurface;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static org.mervin.controlsurface.HassConstants.*;

public class HassEntities {

    Surface.PlatformInitializedCallback callback;

    private ArrayList<HassGroupEntity> groups = new ArrayList<>();
    private ArrayList<HassLightEntity> lights = new ArrayList<>();
    private ArrayList<HassSceneEntity> scenes = new ArrayList<>();
    private ArrayList<HassClimateEntity> climates = new ArrayList<>();

    private HashMap<String, HassEntity> entities = new HashMap<>();

    private int subscriberEventId = 10000;
    private int getStatesId = 20000;
    private int callServiceId = 30000;

    AsyncHttpClient asyncHttpClient;
    WebSocket webSocket;

    private Context context;

    public boolean initialized = false;

    private int port;
    private String ip;
    private String webSocketUrl;

    public HassEntities(Context context, String ip, int port) {
        this.context = context;
        this.ip = ip;
        this.port = port;

        webSocketUrl = "ws://" + ip + ":" + Integer.toString(port) + "/api/websocket";

        start();
    }

    public void stop(){
        closeWebSocket();
        entities.clear();
        groups.clear();
        lights.clear();
        scenes.clear();
        climates.clear();
        initialized = false;
    }

    public void start() {
        if (!ip.equals(R.string.settings_hass_default_ip)) {
            createWebsocket();
        }
    }

    public void restart() {
        stop();
        start();
    }

    public HassLightEntity searchLightEntity(String entityId) {
        for (HassLightEntity entity: lights) {
            if (entity.getId().equals(entityId))
                return entity;
        }
        return null;
    }

    public HassGroupEntity searchGroupEntity(String entityId) {
        for (HassGroupEntity entity: groups) {
            if (entity.getId().equals(entityId))
                return entity;
        }
        return null;
    }

    public LightControlInterface searchLightControl(String id) {
        for (HassGroupEntity entity: groups) {
            if (entity.getId().equals(id))
                return entity;
        }
        for (HassLightEntity entity: lights) {
            if (entity.getId().equals(id))
                return entity;
        }
        return null;
    }

    public HassEntity searchEntity(String id) {
        if (entities.containsKey(id)) {
            return entities.get(id);
        } else {
            return null;
        }
    }

    public ArrayList<LightControlInterface> getLightControlGroups() {
        ArrayList<LightControlInterface> result = new ArrayList<>();
        for (HassGroupEntity entity: groups) {
            result.add(entity);
        }
        return result;
    }

    public ArrayList<LightControlInterface> getLightControlLights() {
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

                    String icon;
                    if (attributes.has(ATTR_ICON)) {
                        icon = attributes.getString(ATTR_ICON);
                    } else if (attributes.has(SURFACE_ICON)) {
                        icon = attributes.getString(ATTR_ICON);
                    } else {
                        icon = "";
                    }

                    int buttonColor = Color.parseColor("#060606");
                    try {
                        boolean hasButtonColor = attributes.has(SURFACE_BUTTON_COLOR);
                        if (hasButtonColor)
                            buttonColor = Color.parseColor(attributes.getString(SURFACE_BUTTON_COLOR));
                    } catch (Exception e) {}


                    if (entityType == EntityType.GROUP) {
                        HassGroupEntity entity = new HassGroupEntity(entityId, friendlyName, icon, buttonColor, this);
                        groups.add(entity);
                        entities.put(entityId, entity);
                    }

                    if (entityType == EntityType.LIGHT) {
                        HassLightEntity entity = new HassLightEntity(entityId, friendlyName, icon, buttonColor, this);
                        lights.add(entity);
                        entities.put(entityId, entity);
                    }

                    if (entityType == EntityType.SCENE) {
                        HassSceneEntity entity = new HassSceneEntity(entityId, friendlyName, icon, buttonColor, this);
                        scenes.add(entity);
                        entities.put(entityId, entity);
                    }

                    if (entityType == EntityType.CLIMATE) {
                        HassClimateEntity entity = new HassClimateEntity(entityId, friendlyName, icon, buttonColor, this);
                        climates.add(entity);
                        entities.put(entityId, entity);
                    }
                }
            }
        } catch (Throwable t) {

        }
    }

    public void onPause() {
        closeWebSocket();
    }

    public void onResume() {
        if (webSocket != null) {
            closeWebSocket();

        }
        createWebsocket();
    }

    private void closeWebSocket() {
        if (webSocket != null) {
            webSocket.end();
            webSocket.close();
            webSocket = null;
            asyncHttpClient = null;
        }
    }

    private void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
        this.webSocket.setStringCallback(getStringCallback());
        subscribeStates();
        getStates();
    }

    private void dispatchState(final JSONObject state) {
        try {
            HassEntity entity = searchEntity(state.getString(ATTR_ENTITY_ID));
            if (entity != null) {
                entity.processState(state);
            }
        } catch (JSONException e) {
            Log.e("DispatchState", e.getMessage());
        }
    }

    private void processData(JSONObject row) {
        try {
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

    private WebSocket.StringCallback getStringCallback() {
        return new WebSocket.StringCallback(){
            public void onStringAvailable(String s){
                try {
                    final JSONObject row = new JSONObject(s);
                    Runnable dispatchState = new Runnable() {
                        @Override
                        public void run() {
                            processData(row);
                        }
                    };
                    Handler mainHandler = new Handler(context.getMainLooper());
                    mainHandler.post(dispatchState);
                } catch (JSONException e) {}
            }
        };
    }

    private void subscribeStates() {
        try {
            JSONObject subscribeStates = new JSONObject();
            subscriberEventId++;
            subscribeStates.put("id", subscriberEventId);
            subscribeStates.put("type", "subscribe_events");
            subscribeStates.put("event_type", "state_changed");
            webSocket.send(subscribeStates.toString());
        } catch (JSONException e) {}
    }

    private void getStates() {
        try {
            JSONObject getStates = new JSONObject();
            getStatesId++;
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
        AsyncHttpClient.WebSocketConnectCallback websocketConnectCallback = new AsyncHttpClient.WebSocketConnectCallback() {
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
        asyncHttpClient.websocket(webSocketUrl, null, websocketConnectCallback);
    }

}




