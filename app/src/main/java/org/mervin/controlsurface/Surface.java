package org.mervin.controlsurface;


import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.view.View.GONE;


public class Surface extends AppCompatActivity implements ColorPickerDialogListener  {

    interface PlatformInitializedCallback {
        void platformInitialized();
        void authFailed();
    }

    interface LightControlInterfaceCallback {
        void unsetLightControlCallback();
        void resetLightControlCallback();
        void updateLightControlCallback();
        void updateButtonCallback();
    }

    interface ClimateControlInterfaceCallback {
        void updateClimateControlCallback(ClimateControlInterface entity);
    }

    interface SensorInterfaceCallback {
        void sensorCallback(SensorInterface entity);
    }

    private HorizontalScrollView scrollView;
    private LinearLayout shortcutButtons;
    private LinearLayout scrollViewItems;

    private LinearLayout settingsView;
    private Button settingsSubmitButton;
    private EditText settingsHassIp;
    private EditText settingsHassPort;
    private EditText settingsHassGroup;
    private EditText settingsHassPassword;
    private TextView setupError;

    private Activity thisActivity;

    private ColorPickerDialog colorPicker;
    private ColorPickerDialog.Builder colorPickerBuilder;

    private HassEntities hassEntities;

    private ArrayList<View> childEntityViews = new ArrayList<>();
    private HashMap<LightControlInterface, View> lightControlViews = new HashMap<>();
    private LightControlInterface colorPicketEntity;

    private SharedPreferences sharedPref;
    private String hassIp;
    private int hassPort;
    private String hassGroup;
    private String hassPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thisActivity = this;

        sharedPref = thisActivity.getPreferences(thisActivity.MODE_PRIVATE);

        setContentView(R.layout.main);
        getInterfaceComponents();

        getSettings();

        colorPicker = new ColorPickerDialog();

        initHassEntities();
        // Will be started with onResume
    }

    private void storeSettings() {

        hassIp = settingsHassIp.getText().toString();
        hassPort = Integer.parseInt(settingsHassPort.getText().toString());
        hassGroup = settingsHassGroup.getText().toString();
        hassPassword = settingsHassPassword.getText().toString();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.settings_hass_ip), hassIp);
        editor.putInt(getString(R.string.settings_hass_port), hassPort);
        editor.putString(getString(R.string.settings_hass_group), hassGroup);
        editor.putString(getString(R.string.settings_hass_password), hassPassword);
        editor.commit();

        if (hassEntities != null) {
            if (hassEntities.initialized)
                scrollViewItems.removeAllViews();
            hassEntities.stop();
            hassEntities = null;
        }
        initHassEntities();
        startHassEntities();
    }

    private void initHassEntities() {
        hassEntities = new HassEntities(this, hassIp, hassPort, hassPassword);

        hassEntities.callback = new PlatformInitializedCallback() {
            @Override
            public void platformInitialized(){
                hideSettings();
                setupError.setText("");
                setHassEntities();
                hideSoftKeyboard();
            }
            public void authFailed() {
                setupError.setText("Authentication failed");
            }
        };
    }

    private void startHassEntities() {
        if (hassEntities != null) {
            hassEntities.start();
        }
    }

    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void getSettings() {

        hassIp = sharedPref.getString(getString(R.string.settings_hass_ip), getString(R.string.settings_hass_default_ip));
        hassPort = sharedPref.getInt(getString(R.string.settings_hass_port), Integer.parseInt(getString(R.string.settings_hass_default_port)));
        hassGroup = sharedPref.getString(getString(R.string.settings_hass_group), getString(R.string.settings_hass_default_group));
        hassPassword = sharedPref.getString(getString(R.string.settings_hass_password), "");

        settingsHassIp.setText(hassIp);
        settingsHassPort.setText(Integer.toString(hassPort));
        settingsHassGroup.setText(hassGroup);
        settingsHassPassword.setText(hassPassword);
    }

    private void hideSettings() {
        settingsView.setVisibility(GONE);
    }

    private void showSettings() {
        settingsView.setVisibility(View.VISIBLE);
    }

    private void getInterfaceComponents() {

        //Controlsurface setup
        scrollView = (HorizontalScrollView) findViewById(R.id.scrollView);
        scrollViewItems = (LinearLayout) findViewById(R.id.scrollViewItems);
        shortcutButtons = (LinearLayout) findViewById(R.id.shortcutButtons);
        scrollView.smoothScrollTo(0,0);
        scrollView.setEnabled(false);

        settingsView = (LinearLayout) findViewById(R.id.settingsView);
        settingsHassIp = (EditText) findViewById(R.id.hassIpAddress);
        settingsHassPort = (EditText) findViewById(R.id.hassPort);
        settingsHassGroup = (EditText) findViewById(R.id.hassGroup);
        settingsHassPassword = (EditText) findViewById(R.id.hassPassword);
        setupError = (TextView) findViewById(R.id.setupError);

        settingsSubmitButton = (Button) findViewById(R.id.settingsSubmitButton);
        settingsSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeSettings();
            }
        });
    }

    @Override
    protected void onPause() {
        //TODO: Handle websocket
        hassEntities.onPause();
        unsetLightControlViews();
        hideChildEntityViews();
        super.onPause();
    }

    @Override
    protected void onResume() {
        hassEntities.onResume();
        super.onResume();
    }

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        int[] rgb = new int[3];
        rgb[0] = Color.red(color);
        rgb[1] = Color.green(color);
        rgb[2] = Color.blue(color);
        colorPicketEntity.setRgb(rgb);
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }

    private void setLightControlView(final LightControlInterface entity, final View lightControlView) {

        unsetLightControlView(entity, lightControlView);
        lightControlViews.put(entity, lightControlView);
        lightControlView.setVisibility(View.VISIBLE);

        //LightControl

        ImageButton closeButton = (ImageButton)lightControlView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unsetLightControlView(entity, lightControlView);
            }
        });

        SeekBar brightnessSeekbar = (SeekBar) lightControlView.findViewById(R.id.brightnessSeekbar);
        brightnessSeekbar.setProgress(0);

        SeekBar colorTempSeekbar = (SeekBar) lightControlView.findViewById(R.id.colorTempSeekbar);
        colorTempSeekbar.setProgress(0);

        TextView entityName = (TextView) lightControlView.findViewById(R.id.entityName);
        ImageButton selectColorButton = (ImageButton) lightControlView.findViewById(R.id.selectColorButton);
        ImageButton randomColorButton = (ImageButton) lightControlView.findViewById(R.id.randomColorButton);
        ImageButton colorLoopButton = (ImageButton) lightControlView.findViewById(R.id.colorLoopButton);

        entityName.setText(entity.getName());
        boolean update = false;

        if (entity != null) {
            if (entity.hasBrightness()) {
                brightnessSeekbar.setVisibility(View.VISIBLE);
                brightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            double multiplier = (double) progress / 20d;
                            int brightness = (int)(254d * multiplier);
                            entity.setBrightness(brightness);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                update = true;
            } else {
                brightnessSeekbar.setVisibility(GONE);
            }

            if (entity.hasColorTemp()) {
                colorTempSeekbar.setVisibility(View.VISIBLE);
                colorTempSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            double multiplier = (double) progress / 20d;
                            int colorTemp = (int)(347d * multiplier);
                            entity.setColorTemp(colorTemp);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                update = true;
            } else {
                colorTempSeekbar.setVisibility(View.GONE);
            }

            if (entity.hasRgb()) {
                selectColorButton.setVisibility(View.VISIBLE);
                selectColorButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        colorPicketEntity = entity;
                        colorPickerBuilder = colorPicker.newBuilder();
                        colorPickerBuilder.show(thisActivity);
                    }
                });
                if (entity.hasColorLoop()) {
                    colorLoopButton.setVisibility(View.VISIBLE);
                    colorLoopButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            entity.setColorLoop();
                        }
                    });
                } else {
                    colorLoopButton.setVisibility(View.GONE);
                }

                if (entity.hasRandom()) {
                    randomColorButton.setVisibility(View.VISIBLE);
                    randomColorButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            entity.setRandom();
                        }
                    });
                } else {
                    randomColorButton.setVisibility(View.GONE);
                }
            } else {
                selectColorButton.setVisibility(View.GONE);
                colorLoopButton.setVisibility(View.GONE);
                randomColorButton.setVisibility(View.GONE);
            }
        }

        if (update) {
            updateLightControls(entity, lightControlView);
        }
    }

    private void updateLightControls(LightControlInterface lightControlInterface, View lightControlView) {
        if (hasLightControlView(lightControlInterface, lightControlView)) {
            final SeekBar brightnessSeekbar = (SeekBar) lightControlView.findViewById(R.id.brightnessSeekbar);
            final SeekBar colorTempSeekbar = (SeekBar) lightControlView.findViewById(R.id.colorTempSeekbar);

            double colorTempMultiplier = (double) Math.round((lightControlInterface.getColorTemp() / 347d)*100) / 100;
            int colorTempProgress = (int)(20 * colorTempMultiplier);
            colorTempSeekbar.setProgress(colorTempProgress);

            double brightnessMultiplier = (double) Math.round((lightControlInterface.getBrightness() / 254d)*100) / 100;
            int brightnessProgress = (int)(20 * brightnessMultiplier);
            brightnessSeekbar.setProgress(brightnessProgress);
        }
    }

    private void unsetLightControlViews() {
        for (Map.Entry<LightControlInterface, View> entry : lightControlViews.entrySet()) {
            unsetLightControlView(entry.getKey(), entry.getValue());
        }
    }

    private void resetLightControlView(LightControlInterface lightControlInterface, View lightControlView) {
        if (hasLightControlView(lightControlInterface, lightControlView)) {
            unsetLightControlView(lightControlInterface, lightControlView);
            setLightControlView(lightControlInterface, lightControlView);
        }
    }

    private void unsetLightControlView(LightControlInterface lightControlInterface, View lightControlView) {
        if (hasLightControlView(lightControlInterface, lightControlView)) {
            lightControlViews.remove(lightControlInterface);
            lightControlView.setVisibility(GONE);
        }
    }

    private boolean hasLightControlView(LightControlInterface lightControlInterface, View lightControlView) {
        for (Map.Entry<LightControlInterface, View> entry : lightControlViews.entrySet()) {
            if (entry.getKey() == lightControlInterface && entry.getValue() == lightControlView) {
                return true;
            }
        }
        return false;
    }

    public void hideChildEntityViews() {
        for (View view : childEntityViews) {
            hideChildEntityView(view);
        }
    }

    public void hideChildEntityView(View childEntityContainer) {
        LinearLayout childEntityView = (LinearLayout)childEntityContainer.findViewById(R.id.childEntityView);
        childEntityView.removeAllViews();
        childEntityContainer.setVisibility(GONE);
    }

    private void showChildEntityView(ArrayList<LightControlInterface> lightControlInterfaces, final View childEntityContainer, View lightControlView, int color) {

        childEntityViews.add(childEntityContainer);
        childEntityContainer.setVisibility(View.VISIBLE);

        LinearLayout childEntityView = (LinearLayout)childEntityContainer.findViewById(R.id.childEntityView);
        childEntityView.removeAllViews();

        ImageButton closeButton = (ImageButton)childEntityContainer.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideChildEntityView(childEntityContainer);
            }
        });
        childEntityView.removeAllViews();

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT,1);
        for (LightControlInterface lightControlInterface : lightControlInterfaces) {

            View child = getLayoutInflater().inflate(R.layout.lightcontrol_list_item_small, null);
            TextView childText = (TextView)child.findViewById(R.id.buttonText);
            childText.setText(lightControlInterface.getName());

            try {
                String icon = lightControlInterface.getIcon();
                if (icon.length() > 0 && icon.substring(0, 4).equals("mdi:")) {

                    ImageView childImage = (ImageView)child.findViewById(R.id.buttonImage);
                    Drawable iconDrawable = thisActivity.getDrawable(getHassIconResource(lightControlInterface.getIcon()));
                    iconDrawable.setTint(getResources().getColor(R.color.white));
                    childImage.setBackground(iconDrawable);
                }
            } catch (Exception e) {
                Log.e("ChildEntityView", e.getMessage());
            }

            childEntityView.addView(child);
            setLightControlButton(lightControlInterface, child, childEntityContainer, lightControlView);
        }
    }

    private void updateButton(LightControlInterface entity, View button) {
        if (entity.isOn()){
            button.setAlpha(1.0f);
        } else {
            button.setAlpha(0.6f);
        }
    }

    private int getHassIconResource(String iconName) {

        if (iconName.length() > 0 && iconName.substring(0, 4).equals("mdi:")) {
            final int resourceId =
                    thisActivity.getResources()
                            .getIdentifier(
                                    iconName
                                            .replace("-", "_")
                                            .replace(":", "_")
                                    , "drawable",
                                    thisActivity.getPackageName());
            if (resourceId > 0) {
                return resourceId;
            } else {
                return R.drawable.mdi_blur;
            }
        } else {
            return R.drawable.mdi_blur;
        }
    }

    private void setHassEntities() {
        HassGroupEntity group = hassEntities.searchGroupEntity(hassGroup);

        if (group != null) {
            for (HassEntity entity : group.getEntities()) {

                View shortcutButton = getLayoutInflater().inflate(R.layout.shortcut_button, null);

                Drawable drawable = thisActivity.getDrawable(getHassIconResource(entity.getIcon()));
                drawable.setTint(getResources().getColor(R.color.white));

                ImageView childImage = (ImageView)shortcutButton.findViewById(R.id.buttonImage);
                childImage.setBackground(drawable);

                shortcutButtons.addView(shortcutButton);

                if (entity instanceof HassGroupEntity) {
                    setHassButtonGroup((HassGroupEntity) entity, shortcutButton);
                }

                if (entity instanceof HassClimateEntity) {
                    final View climateContainer = getLayoutInflater().inflate(R.layout.climate_big, null);
                    scrollViewItems.addView(climateContainer);
                    setClimateView(climateContainer, (ClimateControlInterface) entity);

                    shortcutButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            scrollView.smoothScrollTo(climateContainer.getLeft(), 0);
                        }
                    });
                }
            }
        } else {
            showSettings();
            setupError.setText("Group not found");
        }

    }

    private void setClimateView(final View climateContianer, final ClimateControlInterface climateEntity) {
        climateEntity.setCallback(new ClimateControlInterfaceCallback() {
            @Override
            public void updateClimateControlCallback(ClimateControlInterface entity) {
                updateClimateView(climateContianer, entity);
            }
        });
        ImageButton tempUp = (ImageButton)climateContianer.findViewById(R.id.tempUp);
        tempUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                climateEntity.tempUp();
            }
        });

        ImageButton tempDown = (ImageButton)climateContianer.findViewById(R.id.tempDown);
        tempDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                climateEntity.tempDown();
            }
        });

        ImageButton setEco = (ImageButton)climateContianer.findViewById(R.id.setEco);

        tempDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                climateEntity.tempDown();
            }
        });
    }

    private void updateClimateView(View climateContianer, ClimateControlInterface entity) {
        TextView targetTemp = (TextView)climateContianer.findViewById(R.id.targetTemp);
        TextView currentTemp = (TextView)climateContianer.findViewById(R.id.currentTemp);
        ImageView eco = (ImageView)climateContianer.findViewById(R.id.eco);
        LinearLayout background = (LinearLayout)climateContianer.findViewById(R.id.background);

        targetTemp.setText(Float.toString(entity.getTargetTemp()) + " " + entity.getUnit());
        currentTemp.setText(Float.toString(entity.getTemp()) + " " + entity.getUnit());

        if (entity.isEco()) {
            eco.setVisibility(View.VISIBLE);
        } else {
            eco.setVisibility(View.INVISIBLE);
        }

        if (entity.isHeating()) {
            background.setBackgroundResource(R.drawable.climate_bg_heat);
        } else {
            background.setBackgroundResource(R.drawable.climate_bg_off);
        }
    }

    private void setHassButtonGroup(HassGroupEntity groupEntity, View shortcutButton) {

        final View groupContainer = getLayoutInflater().inflate(R.layout.group_list_vertical_scrollable, null);
        scrollViewItems.addView(groupContainer);
        LinearLayout groupsView = (LinearLayout)groupContainer.findViewById(R.id.groupsView);

        View childContainer = getLayoutInflater().inflate(R.layout.childentity_list_vertical_scrollable, null);
        scrollViewItems.addView(childContainer);
        childContainer.setVisibility(GONE);

        View lightControl = getLayoutInflater().inflate(R.layout.lightcontrol, null);
        scrollViewItems.addView(lightControl);
        lightControl.setVisibility(GONE);

        shortcutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(groupContainer.getLeft(), 0);
            }
        });

        for (HassEntity entity : groupEntity.getEntities()) {

            View child = getLayoutInflater().inflate(R.layout.lightcontrol_list_item_medium, null);
            LinearLayout buttonLayout = (LinearLayout)child.findViewById(R.id.buttonLayout);
            Drawable background = buttonLayout.getBackground();
            background.setTint(entity.getColor());


            TextView childText = (TextView)child.findViewById(R.id.buttonText);
            childText.setText(entity.getName());

            try {
                String icon = entity.getIcon();

                Drawable drawable = thisActivity.getDrawable(getHassIconResource(icon));
                drawable.setTint(getResources().getColor(R.color.white));

                ImageView childImage = (ImageView)child.findViewById(R.id.buttonImage);
                childImage.setBackground(drawable);


            } catch (Exception e) {
                Log.e("LightControl", e.getMessage());
            }

            groupsView.addView(child);

            if (entity instanceof LightControlInterface) {
                setLightControlButton((LightControlInterface)entity, child, childContainer, lightControl);
            }

            if (entity instanceof SceneInterface) {
                setSceneButton((SceneInterface) entity, child);
            }

            if (entity instanceof SensorInterface) {
                setSensor((SensorInterface) entity, child);
            }
        }
    }

    private void setLightControlButton(final LightControlInterface lightControlInterface, final View button, final View childEntityView, final View lightControlView) {
        updateButton(lightControlInterface, button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!lightControlInterface.isOn()) {
                    if (lightControlInterface.isGroup() && lightControlInterface.getLightControlEntities().size() > 1) {
                        showChildEntityView(lightControlInterface.getLightControlEntities(), childEntityView, lightControlView, lightControlInterface.getColor());
                    }
                    setLightControlView(lightControlInterface, lightControlView);
                } else {
                    if (lightControlViews.containsKey(lightControlInterface)) {
                        unsetLightControlView(lightControlInterface, lightControlView);
                        hideChildEntityView(childEntityView);
                    }
                }
                lightControlInterface.switchEntity();
            }
        });

        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (lightControlInterface.isGroup() && lightControlInterface.getLightControlEntities().size() > 1) {
                    showChildEntityView(lightControlInterface.getLightControlEntities(), childEntityView, lightControlView, lightControlInterface.getColor());
                }
                setLightControlView(lightControlInterface, lightControlView);
                return true;
            }
        });

        lightControlInterface.setCallback(new LightControlInterfaceCallback() {
            @Override
            public void unsetLightControlCallback(){
                unsetLightControlView(lightControlInterface, lightControlView);
            }
            @Override
            public void updateLightControlCallback(){
                updateLightControls(lightControlInterface, lightControlView);
            }
            @Override
            public void updateButtonCallback(){
                updateButton(lightControlInterface, button);
            }
            @Override
            public void resetLightControlCallback(){
                resetLightControlView(lightControlInterface, lightControlView);
            }
        });
    }

    private void setSceneButton(final SceneInterface sceneInterface, View button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sceneInterface.turnOn();
            }
        });
    }

    private void setSensor(SensorInterface sensorInterface, final View button) {
        sensorInterface.setCallback(new SensorInterfaceCallback() {
            @Override
            public void sensorCallback(SensorInterface entity) {
                setSensorState(entity, button);
            }
        });
    }

    private void setSensorState(SensorInterface sensorInterface, View button) {
        TextView childText = (TextView)button.findViewById(R.id.buttonText);
        childText.setText(sensorInterface.getName() + ": " + sensorInterface.getState());
    }
}
