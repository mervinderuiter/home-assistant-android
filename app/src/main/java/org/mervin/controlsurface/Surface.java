package org.mervin.controlsurface;


import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;
import me.tankery.lib.circularseekbar.CircularSeekBar;

import java.util.ArrayList;


public class Surface extends AppCompatActivity implements ColorPickerDialogListener  {

    interface PlatformInitializedCallback {
        void platformInitialized();
    }

    interface LightControlInterfaceCallback {
        void unsetLightControlCallback();
        void updateLightControlCallback(LightControlInterface entity);
        void updateButtonCallback(LightControlInterface entity);
    }

    interface ClimateControlInterfaceCallback {
        void updateClimateControlCallback(ClimateControlInterface entity);
    }

    private HorizontalScrollView scrollView;
    private LinearLayout shortcutButtons;
    private LinearLayout scrollViewItems;

    private LinearLayout settingsView;
    private Button settingsSubmitButton;
    private EditText settingsHassIp;
    private EditText settingsHassPort;
    private EditText settingsHassGroup;

    private Activity thisActivity;

    private ColorPickerDialog colorPicker;
    private ColorPickerDialog.Builder colorPickerBuilder;

    private HassEntities hassEntities;

    private ArrayList<View> childEntityViews = new ArrayList<>();
    private ArrayList<View> lightControlViews = new ArrayList<>();
    private LightControlInterface colorPicketEntity;

    private SharedPreferences sharedPref;
    private String hassIp;
    private int hassPort;
    private String hassGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thisActivity = this;

        sharedPref = thisActivity.getPreferences(thisActivity.MODE_PRIVATE);

        setContentView(R.layout.main);
        getInterfaceConponents();

        getSettings();

        colorPicker = new ColorPickerDialog();

        initHassEntities();

    }

    private void storeSettings() {

        hassIp = settingsHassIp.getText().toString();
        hassPort = Integer.parseInt(settingsHassPort.getText().toString());
        hassGroup = settingsHassGroup.getText().toString();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.settings_hass_ip), hassIp);
        editor.putInt(getString(R.string.settings_hass_port), hassPort);
        editor.putString(getString(R.string.settings_hass_group), hassGroup);
        editor.commit();

        scrollViewItems.removeAllViews();
        if (hassEntities != null) {
            hassEntities.stop();
            hassEntities = null;
        }
        initHassEntities();
    }

    private void initHassEntities() {
        hassEntities = new HassEntities(this, hassIp, hassPort);
        hassEntities.callback = new PlatformInitializedCallback() {
            @Override
            public void platformInitialized(){
                hideSettings();
                setHassEntities();
            }
        };
    }

    private void getSettings() {

        hassIp = sharedPref.getString(getString(R.string.settings_hass_ip), getString(R.string.settings_hass_default_ip));
        hassPort = sharedPref.getInt(getString(R.string.settings_hass_port), Integer.parseInt(getString(R.string.settings_hass_default_port)));
        hassGroup = sharedPref.getString(getString(R.string.settings_hass_group), getString(R.string.settings_hass_default_group));

        settingsHassIp.setText(hassIp);
        settingsHassPort.setText(Integer.toString(hassPort));
        settingsHassGroup.setText(hassGroup);
    }

    private void hideSettings() {
        settingsView.setVisibility(View.GONE);
    }

    private void showSettings() {
        settingsView.setVisibility(View.VISIBLE);
    }

    private void getInterfaceConponents() {

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
        //TODO: Handle websocket
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

    private void setLightControlView(final LightControlInterface entity, View lightControlView) {
        if (entity.isOn()) {
            unsetLightControlView(lightControlView);
            lightControlViews.add(lightControlView);
            lightControlView.setVisibility(View.VISIBLE);

            //LightControl
            LinearLayout brightTempContainer = (LinearLayout) lightControlView.findViewById(R.id.brightTempContainer);
            TextView entityName = (TextView) lightControlView.findViewById(R.id.entityName);
            CircularSeekBar brightness = (CircularSeekBar) lightControlView.findViewById(R.id.brightness);
            CircularSeekBar colorTemp = (CircularSeekBar) lightControlView.findViewById(R.id.colorTemp);
            ImageButton selectColorButton = (ImageButton) lightControlView.findViewById(R.id.selectColorButton);
            ImageButton randomColorButton = (ImageButton) lightControlView.findViewById(R.id.randomColorButton);
            ImageButton colorLoopButton = (ImageButton) lightControlView.findViewById(R.id.colorLoopButton);
            brightness.setProgress(0);
            colorTemp.setProgress(0);
            colorTemp.setCircleStrokeWidth(50);
            colorTemp.setStartAngle(120);
            colorTemp.setEndAngle(60);
            brightness.setCircleStrokeWidth(50);
            brightness.setStartAngle(120);
            brightness.setEndAngle(60);

            entityName.setText(entity.getName());
            boolean visible = false;
            boolean update = false;

            if (entity != null) {
                if (entity.hasBrightness()) {
                    brightness.setVisibility(View.VISIBLE);
                    brightness.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(CircularSeekBar seekBar, float progress, boolean fromUser) {
                            if (fromUser) {
                                double progressMultiplyer = progress / 100;
                                double alpha = (175 * progressMultiplyer);
                                seekBar.setCircleProgressColor(Color.argb((int)(alpha + 80), 255, 255, 255));
                                seekBar.setCircleColor(Color.argb((int)alpha, 255, 255, 255));
                                entity.setBrightness((int)(255 * progressMultiplyer));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(CircularSeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(CircularSeekBar seekBar) {

                        }
                    });
                    visible = true;
                    update = true;
                } else {
                    brightness.setVisibility(View.GONE);
                }

                if (entity.hasColorTemp()) {
                    colorTemp.setVisibility(View.VISIBLE);
                    colorTemp.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(CircularSeekBar seekBar, float progress, boolean fromUser) {
                            if (fromUser) {
                                double progressMultiplyer = progress / 100;
                                double kelvin = 6500d - (4300d * progressMultiplyer);
                                int[] rgb = ColorTempToRgb.getRGBFromK((int)kelvin);
                                seekBar.setCircleProgressColor(Color.rgb((int)(rgb[0] * 0.7), rgb[1], rgb[2]));
                                seekBar.setCircleColor(Color.argb(150, (int)(rgb[0] * 0.7), rgb[1], rgb[2]));
                                entity.setColorTemp((int)(347 * progressMultiplyer));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(CircularSeekBar seekBar) {}

                        @Override
                        public void onStopTrackingTouch(CircularSeekBar seekBar) {}
                    });
                    visible = true;
                    update = true;
                } else {
                    colorTemp.setVisibility(View.GONE);
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
                    visible = true;
                    if (entity.hasColorLoop()) {
                        colorLoopButton.setVisibility(View.VISIBLE);
                        colorLoopButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                entity.setColorLoop();
                            }
                        });
                        visible = true;
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
                        visible = true;
                    } else {
                        randomColorButton.setVisibility(View.GONE);
                    }
                } else {
                    selectColorButton.setVisibility(View.GONE);
                    colorLoopButton.setVisibility(View.GONE);
                    randomColorButton.setVisibility(View.GONE);
                }
            }

            if (visible) {

                Display display = getWindowManager().getDefaultDisplay();
                DisplayMetrics outMetrics = new DisplayMetrics ();
                display.getMetrics(outMetrics);

                int dimension = (int)(outMetrics.heightPixels / 2.4);
                brightness.setLayoutParams(new LinearLayout.LayoutParams(dimension, dimension, 0));
                colorTemp.setLayoutParams(new LinearLayout.LayoutParams(dimension, dimension, 0));
            }

            if (update) {
                updateLightControls(entity, lightControlView);
            }
        }
    }

    private void updateLightControls(LightControlInterface entity, View lightControlView) {
        if (lightControlViews.contains(lightControlView)) {
            final CircularSeekBar brightness = (CircularSeekBar) lightControlView.findViewById(R.id.brightness);
            final CircularSeekBar colorTemp = (CircularSeekBar) lightControlView.findViewById(R.id.colorTemp);

            double colorTempMultiplyer = entity.getColorTemp() / 347d;
            double kelvin = 6500d - (4300d * colorTempMultiplyer);
            int[] rgb = ColorTempToRgb.getRGBFromK((int)kelvin);
            colorTemp.setCircleProgressColor(Color.rgb((int)(rgb[0] * 0.7), rgb[1], rgb[2]));
            colorTemp.setCircleColor(Color.argb(150, (int)(rgb[0] * 0.7), rgb[1], rgb[2]));
            colorTemp.setProgress((int)(colorTempMultiplyer * 100));

            int brightnessValue = entity.getBrightness();
            double brightnessMultiplyer = brightnessValue / 255d;
            double alpha = (175 * brightnessMultiplyer);
            brightness.setCircleProgressColor(Color.argb((int)(alpha + 80), 255, 255, 255));
            brightness.setCircleColor(Color.argb((int)alpha, 255, 255, 255));
            brightness.setProgress((int)(brightnessMultiplyer * 100));
        }
    }

    private void unsetLightControlViews() {
        for (View view : lightControlViews) {
            unsetLightControlView(view);
        }
    }

    private void unsetLightControlView(View lightControl) {
        lightControlViews.remove(lightControl);
        lightControl.setVisibility(View.GONE);
    }

    public void hideChildEntityViews() {
        for (View view : childEntityViews) {
            hideChildEntityView(view);
        }
    }

    public void hideChildEntityView(View childEntityContainer) {
        LinearLayout childEntityView = (LinearLayout)childEntityContainer.findViewById(R.id.childEntityView);
        childEntityView.removeAllViews();
        childEntityContainer.setVisibility(View.GONE);
    }

    private void showChildEntityView(ArrayList<LightControlInterface> lightControlInterfaces, View childEntityContainer, View lightControlView) {

        childEntityViews.add(childEntityContainer);
        childEntityContainer.setVisibility(View.VISIBLE);
        LinearLayout childEntityView = (LinearLayout)childEntityContainer.findViewById(R.id.childEntityView);
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
                    Drawable drawable = thisActivity.getDrawable(getHassIconResource(lightControlInterface.getIcon()));
                    drawable.setTint(getResources().getColor(R.color.white));
                    childImage.setBackground(drawable);
                }
            } catch (Exception e) {
                Log.e("LightControlButtonSettings", e.getMessage());
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
                if (entity instanceof HassGroupEntity) {

                    View shortcutButton = getLayoutInflater().inflate(R.layout.shortcut_button, null);

                    Drawable drawable = thisActivity.getDrawable(getHassIconResource(entity.getIcon()));
                    drawable.setTint(getResources().getColor(R.color.white));

                    ImageView childImage = (ImageView)shortcutButton.findViewById(R.id.buttonImage);
                    childImage.setBackground(drawable);

                    shortcutButtons.addView(shortcutButton);

                    setHassButtonGroup((HassGroupEntity) entity, shortcutButton);

                }
            }
        } else {
            showSettings();
        }

    }

    private void setHassButtonGroup(HassGroupEntity groupEntity, View shortcutButton) {

        final View groupContainer = getLayoutInflater().inflate(R.layout.group_list_vertical_scrollable, null);
        scrollViewItems.addView(groupContainer);
        LinearLayout groupsView = (LinearLayout)groupContainer.findViewById(R.id.groupsView);

        View childContainer = getLayoutInflater().inflate(R.layout.childentity_list_vertical_scrollable, null);
        scrollViewItems.addView(childContainer);
        childContainer.setVisibility(View.GONE);

        View lightControl = getLayoutInflater().inflate(R.layout.lightcontrol, null);
        scrollViewItems.addView(lightControl);
        lightControl.setVisibility(View.GONE);

        shortcutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(groupContainer.getLeft(), 0);
            }
        });

        for (HassEntity entity : groupEntity.getEntities()) {

            View child = getLayoutInflater().inflate(R.layout.lightcontrol_list_item_medium, null);
            LinearLayout buttonLayout = (LinearLayout)child.findViewById(R.id.buttonLayout);
            buttonLayout.setBackground(new ColorDrawable(entity.getColor()));

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
        }
    }

    private void setLightControlButton(final LightControlInterface lightControlInterface, final View button, final View childEntityView, final View lightControlView) {
        updateButton(lightControlInterface, button);
        lightControlInterface.setCallback(new LightControlInterfaceCallback() {
            @Override
            public void unsetLightControlCallback(){
                unsetLightControlView(lightControlView);
            }
            @Override
            public void updateLightControlCallback(LightControlInterface entity){
                updateLightControls(entity, lightControlView);
            }
            @Override
            public void updateButtonCallback(LightControlInterface entity){
                updateButton(entity, button);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lightControlInterface.switchEntity();
                if (lightControlInterface.isOn()) {
                    if (lightControlInterface.isGroup() && lightControlInterface.getLightControlEntities().size() > 1) {
                        showChildEntityView(lightControlInterface.getLightControlEntities(), childEntityView, lightControlView);
                    }
                    setLightControlView(lightControlInterface, lightControlView);
                } else {
                    if (lightControlViews.contains(lightControlView)) {
                        unsetLightControlView(lightControlView);
                        hideChildEntityView(childEntityView);
                    }
                }
            }
        });

        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (lightControlInterface.isGroup() && lightControlInterface.getLightControlEntities().size() > 1) {
                    showChildEntityView(lightControlInterface.getLightControlEntities(), childEntityView, lightControlView);
                }
                setLightControlView(lightControlInterface, lightControlView);
                return true;
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
}
