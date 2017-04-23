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
    private ArrayList<HassSensorEntity> sensors = new ArrayList<>();

    private HashMap<String, HassEntity> entities = new HashMap<>();

    private int subscriberEventId = 10000;
    private int getStatesId = 20000;
    private int callServiceId = 30000;

    AsyncHttpClient asyncHttpClient;
    WebSocket webSocket = null;

    private Context context;

    public boolean initialized = false;

    private int port;
    private boolean authenticated = false;
    private String password;
    private String ip;
    private String webSocketUrl;

    public HassEntities(Context context, String ip, int port, String password) {
        this.context = context;
        this.ip = ip;
        this.port = port;
        this.password = password;

        webSocketUrl = "ws://" + ip + ":" + Integer.toString(port) + "/api/websocket";
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

                    int buttonColor = Color.parseColor("#404040");
                    try {
                        boolean hasButtonColor = attributes.has(SURFACE_BUTTON_COLOR);
                        if (hasButtonColor)
                            buttonColor = Color.parseColor(attributes.getString(SURFACE_BUTTON_COLOR));
                    } catch (Exception e) {}


                    switch(entityType) {
                        case GROUP:
                            HassGroupEntity groupEntity = new HassGroupEntity(entityId, friendlyName, icon, buttonColor, this);
                            groups.add(groupEntity);
                            entities.put(entityId, groupEntity);
                            break;
                        case LIGHT:
                            HassLightEntity lightEntity = new HassLightEntity(entityId, friendlyName, icon, buttonColor, this);
                            lights.add(lightEntity);
                            entities.put(entityId, lightEntity);
                            break;
                        case SCENE:
                            HassSceneEntity sceneEntity = new HassSceneEntity(entityId, friendlyName, icon, buttonColor, this);
                            scenes.add(sceneEntity);
                            entities.put(entityId, sceneEntity);
                            break;
                        case CLIMATE:
                            HassClimateEntity climateEntity = new HassClimateEntity(entityId, friendlyName, icon, buttonColor, this);
                            climates.add(climateEntity);
                            entities.put(entityId, climateEntity);
                            break;
                        case SENSOR:
                            HassSensorEntity sensorEntity = new HassSensorEntity(entityId, friendlyName, icon, buttonColor, this);
                            sensors.add(sensorEntity);
                            entities.put(entityId, sensorEntity);
                            break;
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

    private void sendAuth() {
        try {
            JSONObject auth = new JSONObject();
            auth.put("type", "auth");
            auth.put("api_password", password);
            webSocket.send(auth.toString());
        } catch (JSONException e) {}
    }

    private void authFailed() {
        callback.authFailed();
    }

    private void processData(JSONObject row) {
        try {
            if (row.has("type") && row.getString("type").equals("auth_ok")) {
                authenticated = true;
                subscribeStates();
                getStates();
            } else if (row.has("type") && row.getString("type").equals("auth_required")) {
                sendAuth();
            } else if (row.has("type") && row.getString("type") == "auth_invalid") {
                authFailed();
            } else if (row.has("id") && row.getInt("id") == subscriberEventId) {
                JSONObject event = row.getJSONObject("event");
                JSONObject data = event.getJSONObject("data");
                JSONObject newState = data.getJSONObject("new_state");
                dispatchState(newState);
            } else if (row.has("id") && row.getInt("id") == getStatesId) {
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

        } catch (JSONException e) {
            Log.e("Init", e.getMessage());
        }
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
            callServiceId++;
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
        if (webSocket == null) {
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

}




