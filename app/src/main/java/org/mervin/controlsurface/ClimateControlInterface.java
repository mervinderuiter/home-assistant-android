package org.mervin.controlsurface;

/**
 * Created by mervi on 8-4-2017.
 */

public interface ClimateControlInterface {
    boolean isOn();
    boolean isEco();
    boolean isHeating();
    float getTemp();
    float getTargetTemp();
    String getUnit();
    void tempUp();
    void tempDown();
    boolean isAway();
    void setCallback(Surface.ClimateControlInterfaceCallback callback);
}
