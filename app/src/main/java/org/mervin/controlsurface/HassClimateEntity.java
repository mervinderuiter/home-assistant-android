package org.mervin.controlsurface;

import android.graphics.Color;
import android.util.Log;

import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.mervin.controlsurface.HassConstants.*;
import static org.mervin.controlsurface.HassConstants.getEntityType;

public class HassClimateEntity extends HassEntity implements ClimateControlInterface {

    Surface.ClimateControlInterfaceCallback callback;

    private boolean isAway;
    private float currentTemp;
    private float targetTemp;
    private String unit = "";

    public HassClimateEntity(String entityId, String friendlyName, String icon, int color, HassEntities hassEntities) {
        super(entityId, friendlyName, icon, color, hassEntities);
    }

    public void setCallback(Surface.ClimateControlInterfaceCallback callback) {
        this.callback = callback;
        callback.updateClimateControlCallback(this);
    }

    public boolean isOn() {
        return state.equals(STATE_HEAT); //Dunno yet
    }

    public boolean isEco() {
        return state.equals(STATE_ECO);
    }

    public boolean isHeating() {
        return state.equals(STATE_HEAT);
    }

    public boolean isAway(){
        return isAway;
    }

    public float getTemp(){
        return currentTemp;
    }

    public String getUnit(){
        return unit;
    }

    public float getTargetTemp(){
        return targetTemp;
    }

    public void tempUp() {
        setTargetTemp(targetTemp + 0.5f);
    }

    public void tempDown() {
        setTargetTemp(targetTemp - 0.5f);
    }

    public void setTargetTemp(float temp) {
        try {
            targetTemp = temp;
            updateClimateControl();
            JSONObject payload = new JSONObject();
            payload.put(ATTR_ENTITY_ID, entityName);
            payload.put(ATTR_TEMPERATURE, Float.toString(targetTemp));
            hassEntities.callService(DOMAIN_CLIMATE, COMMAND_SET_TEMP, payload);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateClimateControl() {
        if (callback != null) {
            callback.updateClimateControlCallback(this);
        }
    }


    @Override
    public void processState(JSONObject row) {
        try {
            state = row.getString(ATTR_STATE);
            JSONObject attributes = row.getJSONObject(ATTR);

            if (attributes.has(TEMP)) {
                targetTemp = Float.parseFloat(attributes.getString(TEMP));
            }

            if (attributes.has(CURRENT_TEMP)) {
                currentTemp = Float.parseFloat(attributes.getString(CURRENT_TEMP));
            }

            if (attributes.has(TEMP_UNIT) && unit.equals("")) {
                unit = attributes.getString(TEMP_UNIT);
            }

            updateClimateControl();

        } catch (Exception e) {
            Log.e("HassEntity", "exception", e);
        }
    }
}
