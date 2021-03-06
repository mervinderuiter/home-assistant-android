package org.mervin.controlsurface;

import android.graphics.Color;
import android.util.Log;

import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static org.mervin.controlsurface.HassConstants.*;

public class HassLightEntity extends HassEntity implements LightControlInterface {

    Surface.LightControlInterfaceCallback callback;

    private ArrayList<HassGroupEntity.GroupCallback> groupCallbacks = new ArrayList<>();

    private ColorType colorType = ColorType.UNKNOWN;
    private int COLOR_TEMP_OFFSET = 153;

    public int brightness = 0;
    private int[] rgb;
    public int colorTemp = COLOR_TEMP_OFFSET;
    public boolean hasBrightness = false;
    public boolean hasColorTemp = false;
    public boolean hasRgb = false;
    public boolean hasRandom = false;
    public boolean hasColorLoop = false;




    public HassLightEntity(String entityId, String friendlyName, String icon, int color, HassEntities hassEntities) {
        super(entityId, friendlyName, icon, color, hassEntities);
    }

    public void setCallback(Surface.LightControlInterfaceCallback callback) {
        this.callback = callback;
        updateLightControls();
    }

    public void addGroupCallback(HassGroupEntity.GroupCallback callback) {
        groupCallbacks.add(callback);
    }

    public void setRgb(int[] rgb) {
        colorType = ColorType.COLOR_RGB;
        this.rgb = rgb;
        sendSwitchRequest(COMMAND_ON);
    }

    public void setColorTemp(int newColorTemp) {
        if (hasColorTemp) {
            colorTemp = (newColorTemp + COLOR_TEMP_OFFSET);
            colorType = ColorType.COLOR_TEMP;
            sendSwitchRequest(COMMAND_ON);
        } else if (hasRgb) {
            // Calculate Kelvin
            double colorTempPercent = 347d / (double)newColorTemp;
            double kelvin = 6500d - (4300d / colorTempPercent);
            int[] rgb = ColorTempToRgb.getRGBFromK((int)kelvin);
            rgb[0] *= 0.7;
            setRgb(rgb);
        }
    }

    public void setColorLoop() {
        colorType = ColorType.COLOR_LOOP;
        sendSwitchRequest(COMMAND_ON);
    }

    public void setRandom() {
        colorType = ColorType.RANDOM;
        sendSwitchRequest(COMMAND_ON);
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
        sendSwitchRequest(COMMAND_ON);
    }

    public int getBrightness() {
        return brightness;
    }

    public int getColorTemp() {
        return colorTemp - COLOR_TEMP_OFFSET;
    }

    public int[] getRgb() {
        return rgb;
    }

    public boolean hasBrightness() {
        return hasBrightness;
    }

    public boolean hasColorTemp() {
        return hasColorTemp || hasRgb;
    }

    public boolean hasRandom() {
        return hasRandom;
    }

    public boolean hasColorLoop() {
        return hasColorLoop;
    }

    public boolean hasRgb() {
        return hasRgb;
    }

    public boolean isOn() {
        return state.equals(STATE_ON);
    }

    private void updateLightControls() {
        if (callback != null) {
            callback.updateLightControlCallback();
            callback.updateButtonCallback();
        }
    }

    private void unsetLightControls() {
        if (callback != null) {
            callback.unsetLightControlCallback();
        }
    }

    private void resetLightControls() {
        if (callback != null) {
            callback.resetLightControlCallback();
        }
        for (HassGroupEntity.GroupCallback groupCallback : groupCallbacks) {
            groupCallback.resetLightControlCallback();
        }
    }

    public void switchEntity() {
        if (state.equals(STATE_ON)){
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
        sendSwitchRequest(COMMAND_OFF);
        updateState(STATE_OFF);
    }

    private void sendSwitchRequest(String action) {
        try {
            JSONObject payload = new JSONObject();
            payload.put(ATTR_ENTITY_ID, entityName);
            if (state.equals(STATE_ON) && action.equals(COMMAND_ON)) {

                if (hasBrightness) {
                    payload.put(ATTR_BRIGHTNESS, brightness);
                }
                if (colorType == ColorType.COLOR_TEMP && hasColorTemp){
                    payload.put(ATTR_COLOR_TEMP, colorTemp);
                }
                if (colorType == ColorType.COLOR_RGB && hasRgb) {
                    JSONArray rgbArray = new JSONArray(rgb);
                    payload.put(ATTR_RGB_COLOR, rgbArray);
                }
                if (colorType == ColorType.RANDOM && hasRandom) {
                    payload.put(ATTR_EFFECT, EFFECT_RANDOM);
                }
                if (colorType == ColorType.COLOR_LOOP && hasColorLoop) {
                    payload.put(ATTR_EFFECT, EFFECT_COLOR_LOOP);
                }
            }
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
            boolean reset = false;

            if (attributes.has(ATTR_EFFECT_LIST)) {
                // Only enable this once
                JSONArray effectArray = attributes.getJSONArray(ATTR_EFFECT_LIST);
                for(int i = 0, count = effectArray.length(); i< count; i++)
                {
                    if (effectArray.getString(i).equals(EFFECT_RANDOM))
                        hasRandom = true;

                    if (effectArray.getString(i).equals(EFFECT_COLOR_LOOP))
                        hasColorLoop = true;
                }
            }

            if (attributes.has(ATTR_BRIGHTNESS)) {
                if (!hasBrightness && attributes.has(ATTR_BRIGHTNESS)) {
                    hasBrightness = true;
                    reset = true;
                }
                brightness = Integer.parseInt(attributes.getString(ATTR_BRIGHTNESS));
            }

            if (attributes.has(ATTR_COLOR_TEMP)) {
                if (!hasColorTemp && attributes.has(ATTR_COLOR_TEMP)) {
                    hasColorTemp = true;
                    reset = true;
                }
                colorTemp = Integer.parseInt(attributes.getString(ATTR_COLOR_TEMP));
            }

            if (attributes.has(ATTR_RGB_COLOR)) {
                if (!hasRgb && attributes.has(ATTR_RGB_COLOR)) {
                    hasRgb = true;
                    reset = true;
                }
            }

            if (reset) {
                resetLightControls();
            }

            if (!state.equals(newState)) {
                updateState(newState);
            }

            updateLightControls();

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
        }
    }
}
