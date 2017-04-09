package org.mervin.controlsurface;


import android.app.Activity;
import android.app.Application;
import android.app.assist.AssistContent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.tankery.lib.circularseekbar.CircularSeekBar;

import static android.view.View.GONE;
import static org.mervin.controlsurface.HassConstants.ATTR_ENTITY_ID;


public class Surface extends AppCompatActivity implements Application.OnProvideAssistDataListener, ColorPickerDialogListener  {

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

    private LinearLayout groupsView;
    private LinearLayout lightControlsView;
    private LinearLayout groupEntityView;
    private LinearLayout tvView;
    private LinearLayout brightTempContainer;
    private TextView entityName;
    private RequestQueue queue;
    private CircularSeekBar brightness;
    private CircularSeekBar colorTemp;
    private ImageButton selectColorButton;
    private ImageButton randomColorButton;
    private ImageButton colorLoopButton;
    private HashMap<Integer, HassEntity> entities = new HashMap<Integer, HassEntity>();
    private HorizontalScrollView scrollView;

    private ImageButton nestButton;
    private ImageButton groupsButton;
    private ImageButton tvButton;

    private Activity thisActivity;

    private ColorPickerDialog colorPicker;
    private ColorPickerDialog.Builder colorPickerBuilder;
    private LightControlInterface controlOwner;

    private HassEntities hassEntities;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thisActivity = this;

        setContentView(R.layout.main);
        getInterfaceConponents();

        populateNest();

        colorPicker = new ColorPickerDialog();

        queue = Volley.newRequestQueue(this);

        hassEntities = new HassEntities(queue);
        hassEntities.callback = new PlatformInitializedCallback() {
            @Override
            public void platformInitialized(){
                setLightInterfaceButtons(hassEntities.getLightControl());
                setSceneButtons(hassEntities.getScenes());
            }
        };
    }

    private void getInterfaceConponents() {

        //Controlsurface setup
        scrollView = (HorizontalScrollView) findViewById(R.id.scrollView);
        scrollView.smoothScrollTo(0,0);

        groupsView = (LinearLayout) findViewById(R.id.groupsView);
        lightControlsView = (LinearLayout) findViewById(R.id.lightControlsView);
        brightTempContainer = (LinearLayout) findViewById(R.id.brightTempContainer);

        entityName = (TextView) findViewById(R.id.entityName);

        groupEntityView = (LinearLayout) findViewById(R.id.groupEntityView);


        tvView = (LinearLayout) findViewById(R.id.tvView);

        nestButton = (ImageButton) findViewById(R.id.nestButton);
        groupsButton = (ImageButton) findViewById(R.id.groupsButton);
        tvButton = (ImageButton) findViewById(R.id.tvButton);


        //LightControl
        brightness = (CircularSeekBar) findViewById(R.id.brightness);
        colorTemp = (CircularSeekBar) findViewById(R.id.colorTemp);
        brightness.setProgress(0);
        colorTemp.setProgress(0);
        colorTemp.setCircleStrokeWidth(50);
        colorTemp.setStartAngle(120);
        colorTemp.setEndAngle(60);
        brightness.setCircleStrokeWidth(50);
        brightness.setStartAngle(120);
        brightness.setEndAngle(60);

        selectColorButton = (ImageButton) findViewById(R.id.selectColorButton);
        randomColorButton = (ImageButton) findViewById(R.id.randomColorButton);
        colorLoopButton = (ImageButton) findViewById(R.id.colorLoopButton);

        lightControlsView.setVisibility(GONE);
        groupEntityView.setVisibility(GONE);

        setOnClickListeners();
    }

    private void setOnClickListeners() {
        nestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //scrollView.smoothScrollTo(nestView.getLeft(), 0);
            }
        });

        groupsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(groupsView.getLeft(), 0);
            }
        });

        tvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(tvView.getLeft(), 0);
            }
        });

        selectColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorPickerBuilder = colorPicker.newBuilder();
                colorPickerBuilder.show(thisActivity);
            }
        });

        randomColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRandomColor();
            }
        });

        colorLoopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColorLoop();
            }
        });


        colorTemp.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar seekBar, float progress, boolean fromUser) {
                if (fromUser) {
                    double progressMultiplyer = progress / 100;
                    double kelvin = 6500d - (4300d * progressMultiplyer);
                    int[] rgb = ColorTempToRgb.getRGBFromK((int)kelvin);
                    seekBar.setCircleProgressColor(Color.rgb((int)(rgb[0] * 0.7), rgb[1], rgb[2]));
                    seekBar.setCircleColor(Color.argb(150, (int)(rgb[0] * 0.7), rgb[1], rgb[2]));
                    setColorTemp((int)(347 * progressMultiplyer));
                }
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {}
        });

        brightness.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar seekBar, float progress, boolean fromUser) {
                if (fromUser) {
                    double progressMultiplyer = progress / 100;
                    double alpha = (175 * progressMultiplyer);
                    seekBar.setCircleProgressColor(Color.argb((int)(alpha + 80), 255, 255, 255));
                    seekBar.setCircleColor(Color.argb((int)alpha, 255, 255, 255));
                    setBrightness((int)(255 * progressMultiplyer));
                }
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }
        });
    }

    public void setColorTemp(int progress) {
        if (controlOwner != null) {
            controlOwner.setColorTemp(progress);
        }
    }

    public void setBrightness(int progress) {
        if (controlOwner != null) {
            controlOwner.setBrightness(progress);
        }
    }

    public void setColorLoop() {
        if (controlOwner != null) {
            controlOwner.setColorLoop();
        }
    }

    public void setRandomColor() {
        if (controlOwner != null) {
            controlOwner.setRandom();
        }
    }

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        int[] rgb = new int[3];
        rgb[0] = Color.red(color);
        rgb[1] = Color.green(color);
        rgb[2] = Color.blue(color);
        controlOwner.setRgb(rgb);
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }

    @Override
    protected void onPause() {
        //TODO: Handle websocket
        unsetLightControls();
        hideChildEntities();
        super.onPause();
    }

    @Override
    protected void onResume() {
        //TODO: Handle websocket
        super.onResume();
    }

    @Override
    public void onProvideAssistContent(AssistContent assistContent) {
        super.onProvideAssistContent(assistContent);

        try {
            String structuredJson = new JSONObject()
                    .put("@type", "MusicRecording")
                    .put("@id", "https://example.com/music/recording")
                    .put("name", "Album Title")
                    .toString();

            assistContent.setStructuredData(structuredJson);
        } catch (JSONException e) {

        }
    }

    @Override
    public void onProvideAssistData(Activity activity, Bundle data) {
        super.onProvideAssistData(data);
    }

    private void populateNest() {

    }

    private void setLightControls(LightControlInterface entity) {
        if (entity.isOn()) {
            unsetLightControls();
            controlOwner = entity;
            entityName.setText(entity.getName());
            boolean visible = false;
            boolean update = false;

            if (controlOwner != null) {
                if (controlOwner.hasBrightness()) {
                    brightness.setVisibility(View.VISIBLE);
                    visible = true;
                    update = true;
                } else {
                    brightness.setVisibility(GONE);
                }

                if (controlOwner.hasColorTemp()) {
                    colorTemp.setVisibility(View.VISIBLE);
                    visible = true;
                    update = true;
                } else {
                    colorTemp.setVisibility(GONE);
                }

                if (controlOwner.hasRgb()) {
                    selectColorButton.setVisibility(View.VISIBLE);
                    visible = true;
                } else {
                    selectColorButton.setVisibility(GONE);
                }

                if (controlOwner.hasColorLoop()) {
                    colorLoopButton.setVisibility(View.VISIBLE);
                    visible = true;
                } else {
                    colorLoopButton.setVisibility(GONE);
                }

                if (controlOwner.hasRandom()) {
                    randomColorButton.setVisibility(View.VISIBLE);
                    visible = true;
                } else {
                    randomColorButton.setVisibility(GONE);
                }
            }

            if (visible) {
                lightControlsView.setVisibility(View.VISIBLE);

                Runnable setCircularSeekbarDimensions = new Runnable() {
                    public void run() {
                        int dimension = (int)(brightTempContainer.getHeight() / 2.1);
                        brightness.setLayoutParams(new LinearLayout.LayoutParams(dimension, dimension, 0));
                        colorTemp.setLayoutParams(new LinearLayout.LayoutParams(dimension, dimension, 0));
                    }
                };
                Handler handler = new Handler();
                handler.postDelayed(setCircularSeekbarDimensions, 100);
            }

            if (update) {
                updateLightControls(entity);
            }
        }
    }

    private void updateLightControls(LightControlInterface entity) {
        if (controlOwner != null && controlOwner == entity) {
            double colorTempMultiplyer = controlOwner.getColorTemp() / 347d;
            double kelvin = 6500d - (4300d * colorTempMultiplyer);
            int[] rgb = ColorTempToRgb.getRGBFromK((int)kelvin);
            colorTemp.setCircleProgressColor(Color.rgb((int)(rgb[0] * 0.7), rgb[1], rgb[2]));
            colorTemp.setCircleColor(Color.argb(150, (int)(rgb[0] * 0.7), rgb[1], rgb[2]));
            colorTemp.setProgress((int)(colorTempMultiplyer * 100));

            int brightnessValue = controlOwner.getBrightness();
            double brightnessMultiplyer = brightnessValue / 255d;
            double alpha = (175 * brightnessMultiplyer);
            brightness.setCircleProgressColor(Color.argb((int)(alpha + 80), 255, 255, 255));
            brightness.setCircleColor(Color.argb((int)alpha, 255, 255, 255));
            brightness.setProgress((int)(brightnessMultiplyer * 100));
        }
    }

    private void unsetLightControls() {
        controlOwner = null;
        lightControlsView.setVisibility(GONE);
        colorTemp.setProgress(0);
        brightness.setProgress(0);
    }

    public void hideChildEntities() {
        groupEntityView.setVisibility(View.GONE);
        groupEntityView.removeAllViews();
    }

    private void showChildEntities(ArrayList<LightControlInterface> lightControlInterfaces, View parentButton) {

        groupEntityView.setVisibility(View.VISIBLE);
        groupEntityView.removeAllViews();

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT,1);
        for (LightControlInterface lightControlInterface : lightControlInterfaces) {
            Button button = new Button(thisActivity);
            button.setLayoutParams(lp);
            button.setTag(lightControlInterface.getId());
            button.setText(lightControlInterface.getName());
            groupEntityView.addView(button);
            setLightInterfaceButton(lightControlInterface, button);
        }
    }

    private void updateButton(LightControlInterface entity, View button) {
        if (entity.isOn()){
            button.setAlpha(1.0f);
        } else {
            button.setAlpha(0.6f);
        }
    }

    private void setSceneButtons(ArrayList<SceneInterface> sceneInterfaces) {
        for (final SceneInterface sceneInterface : sceneInterfaces) {
            Integer view = getView(sceneInterface.getId());
            ImageButton button = (ImageButton) findViewById(view);
            if (view > 0) {
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sceneInterface.turnOn();
                    }
                });
            }
        }
    }

    private void setLightInterfaceButtons(ArrayList<LightControlInterface> lightControlInterfaces) {
        for (LightControlInterface lightControlInterface : lightControlInterfaces) {
            Integer view = getView(lightControlInterface.getId());
            ImageButton button = (ImageButton) findViewById(view);
            if (view > 0) {
                setLightInterfaceButton(lightControlInterface, button);
            }
        }
    }

    private void setLightInterfaceButton(final LightControlInterface lightControlInterface, final View button) {
        updateButton(lightControlInterface, button);
        lightControlInterface.setCallback(new LightControlInterfaceCallback() {
            @Override
            public void unsetLightControlCallback(){
                unsetLightControls();
            }
            @Override
            public void updateLightControlCallback(LightControlInterface entity){
                updateLightControls(entity);
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
                    if (lightControlInterface.isGroup()) {
                        showChildEntities(lightControlInterface.getChildEntities(), button);
                    }
                    setLightControls(lightControlInterface);
                } else {
                    if (controlOwner == lightControlInterface) {
                        unsetLightControls();
                    }
                }
            }
        });

        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (lightControlInterface.isGroup()) {
                    showChildEntities(lightControlInterface.getChildEntities(), button);
                }
                setLightControls(lightControlInterface);
                return true;
            }
        });
    }

    private int getView(String name) {
        try {
            return getResources().getIdentifier(name, "id", getPackageName());
        } catch (Throwable t) {
            return 0;
        }
    }











}
