This project creates a simple connected relay using a TapNLink module and an app for access code entry. The project uses TapNLink's embedded Java Virtual Machine (JVM) as the application processor. The JVM runs a simple Java program and controls the access to a connected device such as a door lock.

>> Note: Before starting, learn how TapNLink and the associated tools work in this [Getting Started](http://docs.iotize.com/GettingStarted/).

This project is [on GitHub](https://github.com/iotize-sas/VoiceRelayControler-demo) !

The project has 4 folders:

- `java/VoiceRelayControler.java`: The Java code that is executed by the `JVM`.
- `iotize_studio/VoiceRelayControler.iotz`: The IoTize Studio configuration file that is used to configure the TapNLink for this demo.
- `skill`: The Amazon's skill informations. 
- `doc`: The illustrations and the readme file for the java code.

## The Main Components

The main component used in this project is the TapNLink communication module. You can purchase [it here.](https://www.digikey.fr/product-detail/fr/iotize/TNL-FIT203/2087-TNL-FIT203-ND/12397002).

## How it Works

These pictures show the hardware. An extension cord has been cut, and each half has been connected to the relay board.
<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/VoiceRelayControler-demo/doc/images/picture_1.jpg" width="250">
</p>
<p align="center">
    <em>The assembled cord and relay</em>
</p>
<p>
The relay acts as a spy between the mains plug and the load.
</p>

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/VoiceRelayControler-demo/doc/images/picture_2.jpg" width="250">
</p>
<p align="center">
    <em>Inside the relay casing</em>
</p>
<p>
Inside the plastic casing, the TapNLink module is linked with a ribbon cable to the relay.
</p>

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/VoiceRelayControler-demo/doc/images/picture_3.jpg" width="250">
</p>
<p align="center">
    <em>The relay</em>
</p>
<p>
The relay element switches the current to the device on and off. 
</p>

The red cable will be replaced by a button, to ask if access is possible.

## Overview

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/VoiceRelayControler-demo/doc/images/overview.jpg">
</p>
<p align="center">
    <em>General view of the project</em>
</p>

## Schematic

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/VoiceRelayControler-demo/doc/images/schematic.png">
</p>
<p align="center">
    <em>Electrical diagram</em>
</p>

## Java code

The `Java` code embedded in the `JVM` allows us to do an `onCheck()` call each time interval (This is defined in the `TapNLinkVar` construct). With this `onCheck()` method, by verifying that the provided `id` corresponds to the desired `id`, we will be able to perform data recovery tasks. The `TapNLinkVar` value also updates or sends an MQTT message.

- For more `java` code information, [click here](https://github.com/iotize-sas/VoiceRelayControler-demo/doc/Java_code/Java_code.md)!

## IoTize Studio

1. Open the TapNLink configuration project `iotize_studio/VoiceRelayControler.iotz` with IoTize Studio. For more information about Studio, [click here](http://docs.iotize.com/UserManuals/IotizeStudio/).
2. In the `IoTize_Studio/Tap` section, set `User internal JVM` to `Yes` and set the `.java` file path `java/VoiceRelayControler.java`.
3. In IoTize Studio execute the `java` build to generate the `.bcb` file. 
4. Configure your `TapNLink` using `IoTize Studio` :
    -  [Setup the connection to your TapNLink](http://docs.iotize.com/UserManuals/DiverseTools/)
    -  Click on the `Configure` button, and wait for the end of the configuration process.
5. Reboot the TapNLink to apply the new configuration. 

## Conversation demo 

<p align="center">
<img src="https://github.com/iotize-sas/Open-Projects/blob/main/VoiceRelayControler-demo/doc/images/schema_chat.jpg">
</p>
<p align="center">
    <em>Possible procedures</em>
</p>
