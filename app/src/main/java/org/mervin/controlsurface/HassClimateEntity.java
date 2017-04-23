package org.mervin.controlsurface;

import android.graphics.Color;
import android.util.Log;

import com.android.volley.RequestQueue;

import org.json.JSONObject;

import static org.mervin.controlsurface.HassConstants.*;
import static org.mervin.controlsurface.HassConstants.getEntityType;

public class HassClimateEntity extends HassEntity implements ClimateControlInterface {

    Surface.ClimateControlInterfaceCallback callback;

    private boolean isAway;
    private float currentTemp;
    private float targetTemp;
    private String unit;

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

    public void setTargetTemp(float target){
        targetTemp = target;
    }

    public float getTargetTemp(){
        return targetTemp;
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

            if (attributes.has(TEMP_UNIT)) {
                unit = attributes.getString(TEMP_UNIT);
            }

            if (callback != null) {
                callback.updateClimateControlCallback(this);
            }

        } catch (Exception e) {
            Log.e("HassEntity", "exception", e);
        }
    }
}
