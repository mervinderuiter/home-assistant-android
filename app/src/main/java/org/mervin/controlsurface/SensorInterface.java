package org.mervin.controlsurface;

/**
 * Created by mervi on 7-4-2017.
 */

public interface SensorInterface {
    String getId();
    String getState();
    void setCallback(Surface.SensorInterfaceCallback callback);
    String getName();
}