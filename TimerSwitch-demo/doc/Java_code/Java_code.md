## Java code

The `Java` code embedded in the `JVM` will allow us to make a `onCheck()` method call every time (This time is defined in a `TapNLinkVar` constructor). In this onCheck() method, by verifying that the passed `id` corresponds to the desired one, we will be able to perform data recovery tasks and also `TapNLinkVar` value updates or send MQTT message.

### Pin management

To find more information for `Pin`, [it's here](http://docs.iotize.com/reference/jvm/index.html)

1. Create var for each `Pin` paw used.

    ```java
       public Pin pinPressButton = new Pin(Pin.TGT_DATA, 0x04, Pin.HIZ);
       public Pin pinActivation = new Pin(Pin.TGT_CLK, Pin.PP_OUT, Pin.HIZ);
    ```
    * The `Pin TARGET_DATA` return the state of the button.
    * The `Pin TARGET_CLK` define the relay state.
    
### Manual access

To know if the access as in progress, we are looked the state of the `Pin` button. Then we get the default button value to define default value.

To check if the state change, the frequency of this call has init every `100ms`. If state has 3 times different from default value, then we know the button has press.

Also, we define manual access in progress and turn `ON` the device.


```java
    if(id == MANUAL_ACCES_ID) {
        if(!initPinValue) {
            defaultValuePinButton = pinPressButton.getValue();
            initPinValue = true;
        }
        if(pinPressButton.getValue() != defaultValuePinButton) {
            buttonPress++;
        } else {
            buttonPress = 0;
        }
        if(buttonPress == 0x03) {
            buttonPress = 0;
            accessInProgress = true;
            pinActivation.setValue(TURN_ON);
            startAccessTime = (system.getTime() / 1000);  
        }
    }
```
    
### Access remaining

If manual access has in progress, then we check if the remaining from access time has equals or higher to current time subtract to the start access time.

If it was, we stop the access progress, turn `OFF` the device and define the status with the program.

If it wasn't, we refresh the time remaining to access progress.

```java
    if(id == ACCES_TIME_ID) {   
            if(accessInProgress) {
                if(((system.getTime() / 1000) - startAccessTime) >= accessTime_varInTap.getValue()) {
                    manualAccess_varInTap.setValue(0x00);
                    pinActivation.setValue(TURN_OFF);
                    accessInProgress = false;
                    definePinStatusByProgram();
                } else {
                    manualAccess_varInTap.setValue((startAccessTime + accessTime_varInTap.getValue()) - (system.getTime() / 1000));
                }
            }
            relayStatus_varInTap.setValue(pinActivation.getValue());
        }
```

### State of the device

If the manual access has not in progress we define the device state with program.

```java
if(id == RELAY_STATUS_ID) {
    getCurrentTime();
    if(!accessInProgress) {
        definePinStatusByProgram();
    }        
}  
```

If the hour change and auto run mode has enabled, then we define the current mode to mode `AUTO` to define state with daily setup.

If the hour didn't change and auto run has disable, then we define the state with current active mode.

```java
public void definePinStatusByProgram() {
        if((automaticRun_varInTap.getValue() == 0x01) && (oldActivationHour != currentActivationHour)) {
            activeMode_varInTap.setValue(0x01);
            pinActivation.setValue(dailySetup_varInTap.getValue()[currentActivationHour]);
            relayStatus_varInTap.setValue(pinActivation.getValue());
        } else {  
            switch (activeMode_varInTap.getValue()) {
                case MODE_FORCE_OFF:
                    pinActivation.setValue(TURN_OFF);
                    break;
                case MODE_FORCE_ON:
                    pinActivation.setValue(TURN_ON);
                    break;
                case MODE_AUTO:
                    pinActivation.setValue(dailySetup_varInTap.getValue()[currentActivationHour]);
                    break;
                default:
                    system.sendMQTTMessage(topic,"Wrong mode define, mode :" + activeMode_varInTap.getValue(), 0);
                    break;
            }
        } 
    }
```