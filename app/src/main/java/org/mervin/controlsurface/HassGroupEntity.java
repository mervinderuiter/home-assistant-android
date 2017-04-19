package org.mervin.controlsurface;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static org.mervin.controlsurface.HassConstants.*;

public class HassGroupEntity extends HassEntity implements LightControlInterface {

    Surface.LightControlInterfaceCallback callback;

    private ArrayList<HassLightEntity> lightControlEntities = new ArrayList<>();
    private ArrayList<HassEntity> entities = new ArrayList<>();
    private ArrayList<String> childIds = new ArrayList<>();
    private boolean childEntitiesSet = false;

    private ColorType colorType = ColorType.UNKNOWN;
    public int brightness = 0;
    private int[] rgb;
    public int colorTemp = COLOR_TEMP_OFFSET;
    public boolean hasBrightness = false;
    public boolean hasColorTemp = false;
    public boolean hasRgb = false;

    public HassGroupEntity(String entityId, String friendlyName, String icon, int color, HassEntities hassEntities) {
        super(entityId, friendlyName, icon, color, hassEntities);
    }

    @Override
    public boolean isGroup(){
        return true;
    }

    @Override
    public ArrayList<LightControlInterface> getLightControlEntities() {
        ArrayList<LightControlInterface> array = new ArrayList<>();
        for (LightControlInterface entity: lightControlEntities) {
            array.add(entity);
        }
        return array;
    }

    public ArrayList<HassEntity> getEntities() {
        ArrayList<HassEntity> array = new ArrayList<>();
        for (HassEntity entity: entities) {
            array.add(entity);
        }
        return array;
    }

    public void setCallback(Surface.LightControlInterfaceCallback callback) {
        this.callback = callback;
    }

    public void setRgb(int[] rgb) {
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasRgb()){
                entity.setRgb(rgb);
            }
        }
    }

    public void setColorTemp(int newColorTemp) {
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasColorTemp()){
                entity.setColorTemp(newColorTemp);
            }
        }
    }

    public void setRandom() {
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasColorTemp()){
                entity.setRandom();
            }
        }
    }

    public void setColorLoop() {
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasColorTemp()){
                entity.setColorLoop();
            }
        }
    }

    public void setBrightness(int brightness) {
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasBrightness()){
                entity.setBrightness(brightness);
            }
        }
    }

    public int getBrightness() {
        int result = 0;
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasBrightness()){
                result = entity.getBrightness();
                break;
            }
        }
        return result;
    }

    public int getColorTemp() {
        int result = 0;
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasColorTemp()){
                result = entity.getColorTemp();
                break;
            }
        }
        return result;
    }

    public int[] getRgb() {
        int[] result = new int[3];
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasRgb()){
                result = entity.getRgb();
                break;
            }
        }
        return result;
    }

    public boolean hasBrightness() {
        boolean result = false;
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasBrightness()){
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean hasColorTemp() {
        boolean result = false;
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasColorTemp()){
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean hasRgb() {
        boolean result = false;
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasRgb()){
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean hasColorLoop() {
        boolean result = false;
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasColorLoop()){
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean hasRandom() {
        boolean result = false;
        for (HassLightEntity entity : lightControlEntities) {
            if (entity.hasRandom()){
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean isOn() {
        return state.equals(STATE_ON);
    }

    public void switchEntity() {
        if (state.equals(STATE_ON)){
            callback.unsetLightControlCallback();
            sendSwitchRequest(COMMAND_OFF);
            updateState(STATE_OFF);
        } else {
            sendSwitchRequest(COMMAND_ON);
            updateState(STATE_ON);
        }
    }

    public void turnOn() {
        sendSwitchRequest(COMMAND_ON);
        updateState(STATE_ON);
    }

    public void turnOff() {
        callback.unsetLightControlCallback();
        sendSwitchRequest(COMMAND_OFF);
        updateState(STATE_OFF);
    }

    private void updateLightControls() {
        if (callback != null) {
            callback.updateLightControlCallback(this);
            callback.updateButtonCallback(this);
        }
    }

    private void unsetLightControls() {
        if (callback != null) {
            callback.unsetLightControlCallback();
        }
    }

    private void setChildIds(JSONArray jsonArray) {
        try {
            for (int i = 0, count = jsonArray.length(); i < count; i++) {
                childIds.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {}
    }

    public void setChildEntities() {
        if (!hassEntities.initialized) {
            for (String childId : childIds) {
                try {
                    HassLightEntity lightEntity = hassEntities.searchLightEntity(childId);
                    if (lightEntity != null) {
                        lightControlEntities.add(lightEntity);
                    }
                    HassEntity entity = hassEntities.searchEntity(childId);
                    if (entity != null) {
                        entities.add(entity);
                    }
                } catch (Exception e) {
                }

            }
            childEntitiesSet = true;
        }
    }

    private void sendSwitchRequest(String action) {
        String url = base_url + URL_SERVICES + DOMAIN_LIGHT + action;

        try {
            JSONObject payload = new JSONObject();
            payload.put(ATTR_ENTITY_ID, entityName);
            hassEntities.callService(DOMAIN_LIGHT, action, payload);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processState(JSONObject row) {
        try {
            String newState = row.getString(ATTR_STATE);
            JSONObject attributes = row.getJSONObject(ATTR);

            if (!hassEntities.initialized) {
                JSONArray jsonArray = attributes.getJSONArray(ATTR_ENTITY_ID);
                setChildIds(jsonArray);
            }

            if (!state.equals(newState)) {
                updateState(newState);
            }

        } catch (Exception e) {
            Log.e("HassEntity", "exception", e);
        }

    }

    private void updateState(String newState) {
        if (!state.equals(newState)) {
            state = newState;
            if (state.equals(STATE_OFF)){
                unsetLightControls();
            }
            updateLightControls();
        }
    }

}
