# home-assistant-android
A controlsurface for Home Assistant

Easy to configure controlsurface for Home Assistant, targeted to wall mounted android tablets. 

# Features: 

* Easy configuration trough Home-Assistant
* Uses websocket API
* Light control with RGB, Brightness, Color Temperature and effects
* Color temperature to RGB translation
* Thermostat control
* Sensors
* Uses same MDI icons as Home-Assistant

# What doesn't work (yet)
* Set temperature on thermostat
* Scripts
* Media players
* More methods to style your view will be added 

# How to configure
* Start the app
* Enter the Home-Assistant IP and Port
* Enter a configuration group (for example: group.surface)
* Create group.surface in Home-Assistant

# Create your view (or multiple)
* Create a group view
To add a group view, create a new Home-Assistant group and add it to the configuration group (group.surface). Add lights, scenes or sensors to the group. The group will be displayed in a column on the screen. Restart Home-Assistant to apply settings. 
* Create a thermostat view
To create a thermostat view, add a thermostat (for example: climate.thermostat) to the configuration group (group.surface).

# Style your view
Views can be styled using the Home-Assistant costomization section. The icon is automatically detected so this does not have to be configured. Change a button name by setting 'friendly_name' for your entity. Color the button by adding 'surface_button_color: '#38B9FF'' to the entity. 

# Screenshots

![1](https://cloud.githubusercontent.com/assets/16005217/25311945/374bc684-280d-11e7-8711-cc81e0d9d42c.png)
![2](https://cloud.githubusercontent.com/assets/16005217/25311946/394f27d2-280d-11e7-8799-4fd70294864d.png)
![3](https://cloud.githubusercontent.com/assets/16005217/25311947/3ac1dac4-280d-11e7-87e4-1fc5b1084f33.png)

