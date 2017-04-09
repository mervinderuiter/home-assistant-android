package org.mervin.controlsurface;

import java.util.ArrayList;

/**
 * Created by mervi on 1-4-2017.
 */

public interface LightControlInterface {
    void setRgb(int[] rgb);
    void setColorTemp(int colorTemp);
    void setBrightness(int brightness);
    void setColorLoop();
    void setRandom();
    int[] getRgb();
    int getColorTemp();
    int getBrightness();
    boolean hasRgb();
    boolean hasColorTemp();
    boolean hasBrightness();
    boolean hasColorLoop();
    boolean hasRandom();
    void switchEntity();
    void turnOn();
    void turnOff();
    boolean isOn();
    String getName();
    String getId();
    void setCallback(Surface.LightControlInterfaceCallback callback);
    boolean isGroup();
    ArrayList<LightControlInterface> getChildEntities();
    void setAutoUpdate(boolean autoUpdate);
}
