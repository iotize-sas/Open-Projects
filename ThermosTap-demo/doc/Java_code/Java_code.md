## Java code

The `Java` code embedded in the `JVM` will allow us to make a `onCheck()` method call every time (This time is defined in a `TapNLinkVar` constructor). In this onCheck() method, by verifying that the passed `id` corresponds to the desired one, we will be able to perform data recovery tasks and also `TapNLinkVar` value updates or send MQTT message.

### Pin management

To find more information for `Pin`, [it's here](http://docs.iotize.com/reference/jvm/index.html)

1. Create var for each `Pin` paw used.

    ```java
       public Pin pinTurnPowerOnOff = new Pin(Pin.PA2, Pin.PP_OUT, Pin.HIZ);
       public Pin pinTempSensor = new Pin(Pin.PB3, Pin.ANA, Pin.HIZ);
    ```
    * The `Pin PA2` define the LED2 status .
    * The `Pin PB3` return the POT value.
    
### Auto recovery

First time we check if the auto recovery mode has active or not.
And if the hour change than last onCheck call
If this one has true, then we redefine the current mode to AUTO
And we define the target temperature with program value

```java
    if(autoRecovery_varInTap.getValue() == 0x01 && oldTimeHour != currentTimeHour) {
        currentMode_varInTap.setValue(0x01);
        int dayConfig = getDayConfig(currentDay);
        if(dayConfig == 1) {
            targetTemperature = getTempByMode(programWE_varInTap.getValue()[currentTimeHour]);
        } else {
            targetTemperature = getTempByMode(programWD_varInTap.getValue()[currentTimeHour]);
        }
    }
```

### Define target temperature

```java
switch(currentMode_varInTap.getValue()) {
    case 0x00 :
        targetTemperature = 0;
        break;
    case 0x01 :
        int dayConfig = getDayConfig(currentDay);
        if(dayConfig == 1) {
            targetTemperature = getTempByMode(programWE_varInTap.getValue()[currentTimeHour]);
        }else {
            targetTemperature = getTempByMode(programWD_varInTap.getValue()[currentTimeHour]);
        }
        break;
    case 0x02 :
        targetTemperature = getTempByMode(programWE_varInTap.getValue()[currentTimeHour]);
        break;
    case 0x03 : 
        targetTemperature = getTempByMode(programWD_varInTap.getValue()[currentTimeHour]);
        break;
    case 0x04 :
        targetTemperature = ecoTemp_varInTap.getValue();
        break;
    case 0x05 :
        targetTemperature = comfortTemp_varInTap.getValue();
        break;
    case 0x06 :
        targetTemperature = freeFrostTemp_varInTap.getValue();
        break;
}
```

### Compare temperature

The target temperature has refresh on var tap
Then we compare this value with current temperature find on sensor.

```java
targetTemp_varInTap.setValue((float) targetTemperature);
if(currentTemp_varInTap.getValue() <= ( targetTemperature - hysteresis_varInTap.getValue() )) {   
    currentPowerState = true;
} else if(currentTemp_varInTap.getValue() >= ( targetTemperature + hysteresis_varInTap.getValue()) ){
    currentPowerState = false;
} 
```

### Refresh the active percentage

```java
overallTime += (STATE_FREQ / 1000);
if(currentPowerState) {
    powerActiveTime += (STATE_FREQ / 1000);
}
activePercentage_varInTap.setValue((float) powerActiveTime / overallTime * 100);
```

### Define the led status

With the compare temperature result
We define the led ON or OFF with Pin setValue method

```java
if(currentPowerState) {
    pinTurnPowerOnOff.setValue(0x00);
} else {
    pinTurnPowerOnOff.setValue(0x01);
} 
```