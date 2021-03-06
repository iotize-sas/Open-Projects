# ThermosTap



This project creates a connected temperature sensor that is used as a thermostat from a TapNLink module. The project uses TapNLink's embedded Java Virtual Machine (JVM) as the application processor. The JVM runs a simple Java program to drive the temperature sensor. 

## *** WARNING ***

<p>THE THERMOSTAP IN THIS DESIGN CONNECTS DIRECTLY TO 220 VOLT MAINS POWER.</p> 

<p>Physical contact with any component of the design can cause electrocution resulting in serious INJURY, or DEATH.</p> 

<p>DO NOT reproduce this design without appropriate knowledge of, and protective measures for working with mains power.</p>

<p>Disconnect power before handling the material or manipulating the connections. Ensure that material is properly grounded and isolated. Always work with another adult present in case of an emergency.</p>


>> Note: Before starting, learn how TapNLink and the associated tools work in this [Getting Started](http://docs.iotize.com/GettingStarted/).

This project is on [Github](https://github.com/iotize-sas/Open-Projects/tree/main/ThermosTap-demo) !


The project has 4 folders:

- `app_generated/ThermosTap.apk`: Source for the mobile application. It was auto generated by IoTize mobile app build server.
- `java/ThermosTap.java`: Contains a Java code which will be executed by the `JVM`.
- `iotize_studio/ThermosTap.iotz`: IoTize Studio configuration file to configure the TapNLink for the demo.
- `doc`: Contains a pictures and java code readme file.

## The Main Components

The main components used in this project are:
- TapNLink (the communication module), [it's here](https://www.digikey.fr/product-detail/fr/iotize/TNL-FIT203/2087-TNL-FIT203-ND/12397002).

## How it Works

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/picture_1.jpg" width="500">
</p>
<p align="center">
    <em>The hardware setup</em>
</p>


## Overview

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/overview.jpg">
</p>
<p align="center">
    <em>General view of the project</em>
</p>

## Schematic 

The schematic below contains 3 blocks that can be used to control the temperature:

1. a simple relay that is supposed to start/stop heating.
2. an OT+ (opentherm) interface,
3. a 'pilot wire' that is an interface very popular on the French market (and almost unknown everywhere else).

In the application below, only the 'simple relay' block will be used.

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/diagram.png">
</p>
<p align="center">
    <em>Electrical diagram</em>
</p>

## Java code

In the `Java` code executed by the `JVM`, the `onCheck()` is called periodically (the period is specified in the `TapNLinkVar` construct). Using this method, we determine the power by evaluating the differences in the pulses counts during the elapsed time. 

- for more `Java` code information, [click here](https://github.com/iotize-sas/Open-Projects/tree/main/ThermosTap-demo/doc/Java_code/Java_code.md) !

## IoTize Studio

1. Open the TapNLink configuration project `iotize_studio/ThermosTap.iotz` with IoTize Studio. For more information about Studio [click here](http://docs.iotize.com/UserManuals/IotizeStudio/).
2. In the `IoTize_Studio/Tap` section, set `User internal JVM` to `Yes`, and set the `.java` file path `java/ThermosTap.java`.
3. In IoTize Studio, execute the `java` build to generate the `.bcb` file. 
4. Configure your `TapNLink` using `IoTize Studio` :
    -  [Setup the connection to your TapNLink](http://docs.iotize.com/UserManuals/DiverseTools/)
    -  [Set the IoT Platform (MQTT) settings](http://docs.iotize.com/Technologies/AWSIot/).
    -  Click on the `Configure` button, and wait until the end of the configuration process.
5. Reboot the TapNLink to apply the new configuration. 

## Running the 'ThermosTap' App

- Because this app has not been published on app stores, you have to install it manually. See how this is done [here](https://github.com/iotize-sas/Open-Projects/tree/main/ThermosTap-demo/app_generated/ThermosTap.apk).
- After your circuit is properly connected, and your TapNLink configured, launch the mobile app. The fastest way is to approach your mobile phone (with NFC enabled) to the TapNLink. NFC will launch the app and connect the mobile to the TapNLink automatically. 
- With `TapNLink` you can now monitor the target and current temperatures.

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/app.jpg" width="200">
</p>
<p align="center">
    <em>View of the app</em>
</p>

## Active Mode

You can activate any mode. However, if `Auto recovery` mode is enabled, then the mode will automatically be reset to  `AUTO` at the start of each time period.

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/app_1.jpg" width="200">
</p>
<p align="center">
    <em>Selecting the mode</em>
</p>

## Set the Temperature for Each Mode

The temperatures for each mode is changed from the app using a slider.

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/app_2.jpg" width="200">
</p>
<p align="center">
    <em>The temperature sliders</em>
</p>

## Adjust the Hysteresis

You can modify the hysteresis to avoid frequent On/Off switching.

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/app_3.jpg" width="200">
</p>
<p align="center">
    <em>Hysteresis slider</em>
</p>

## Define Days by Type

For each day, set the type as Work Day or Weekend. This will determine the program that is applied for each day.

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/app_4.jpg" width="200">
</p>
<p align="center">
    <em>Setting the type of day</em>
</p>

## Create a Program for Each Type

For each type of day, set the schedule with heating modes (Comfort, Eco, etc.) for each time period.

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/ThermosTap-demo/doc/images/app_5.jpg" width="200">
</p>
<p align="center">
    <em>Programming the Weekend</em>
</p>
