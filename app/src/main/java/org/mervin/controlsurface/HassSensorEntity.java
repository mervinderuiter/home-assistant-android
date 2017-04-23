package org.mervin.controlsurface;

import android.graphics.Color;
import android.util.Log;

import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.mervin.controlsurface.HassConstants.*;

public class HassSensorEntity extends HassEntity implements SensorInterface {

    Surface.SensorInterfaceCallback callback;

    public HassSensorEntity(String entityId, String friendlyName, String icon, int color, HassEntities hassEntities) {
        super(entityId, friendlyName, icon, color, hassEntities);
        this.state = "";
    }

    public void setCallback(Surface.SensorInterfaceCallback callback) {
        this.callback = callback;
        callback.sensorCallback(this);
    }

    @Override
    public void processState(JSONObject row) {
        try {
            state = row.getString(ATTR_STATE);

            if (callback != null) {
                callback.sensorCallback(this);
            }

        } catch (Exception e) {
            Log.e("HassEntity", "exception", e);
        }
    }
}