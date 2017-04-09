package org.mervin.controlsurface;

/**
 * Created by mervi on 2-4-2017.
 */

public final class HassConstants {

    public static final int COLOR_TEMP_OFFSET = 153;

    public static final String STATE_OFF = "off";
    public static final String STATE_ON = "on";
    public static final String STATE_HEAT = "heat";
    public static final String STATE_ECO = "eco";

    public static final String COMMAND_ON = "turn_on";
    public static final String COMMAND_OFF = "turn_off";

    public static final String ATTR_ENTITY_ID = "entity_id";
    public static final String ATTR_BRIGHTNESS = "brightness";
    public static final String ATTR_COLOR_TEMP = "color_temp";
    public static final String ATTR_RGB_COLOR = "rgb_color";
    public static final String ATTR_EFFECT_LIST = "effect_list";
    public static final String ATTR_EFFECT = "effect";
    public static final String ATTR_STATE = "state";
    public static final String ATTR_FRIENDLY_NAME = "friendly_name";
    public static final String ATTR = "attributes";
    public static final String EFFECT_RANDOM = "random";
    public static final String EFFECT_COLOR_LOOP = "colorloop";

    public static final String URL_STATES = "states/";
    public static final String URL_SERVICES = "services/";
    public static final String URL_DOMAIN_SCENE = "scene/";
    public static final String URL_DOMAIN_LIGHT = "light/";

    public enum EntityType {
        GROUP,
        SCENE,
        LIGHT,
        CLIMATE,
        UNKNOWN
    }

    public enum ColorType {
        COLOR_TEMP,
        COLOR_RGB,
        COLOR_LOOP,
        RANDOM,
        UNKNOWN
    }

    public static EntityType getEntityType(String entityName) {
        if (entityName.substring(0,5).equals("scene")){
            return EntityType.SCENE;
        }

        if (entityName.substring(0,5).equals("group")){
            return EntityType.GROUP;
        }

        if (entityName.substring(0,5).equals("light")){
            return EntityType.LIGHT;
        }

        return EntityType.UNKNOWN;
    }
}