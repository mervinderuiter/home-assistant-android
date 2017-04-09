package org.mervin.controlsurface;

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

    public HassClimateEntity(String entityId, String friendlyName, RequestQueue queue, HassEntities hassEntities) {
        super(entityId, friendlyName, queue, hassEntities);
    }

    public void setCallback(Surface.ClimateControlInterfaceCallback callback) {
        this.callback = callback;
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
        return true;
    }

    public float getTemp(){
        return currentTemp;
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
            if (getEntityType(row.getString(ATTR_ENTITY_ID)).equals(HassConstants.EntityType.GROUP)){
                String newState = row.getString(ATTR_STATE);
                JSONObject attributes = row.getJSONObject(ATTR);

                if (!state.equals(newState)) {
                    updateState(newState);
                }
            }

        } catch (Exception e) {
            Log.e("HassEntity", "exception", e);
        }

    }

    private void updateState(String newState) {
        if (!state.equals(newState)) {
            state = newState;
            updateClimateControls();
        }
    }

    private void updateClimateControls() {
        if (callback != null) {
            callback.updateClimateControlCallback(this);
        }
    }

}
