## Java code

The Java code embedded in the `JVM` will allow us to make a `onCheck()` method call every time (This time is defined in a `TapNLinkVar` constructor). In this onCheck() method, by verifying that the passed `id` corresponds to the desired one, we will be able to perform data recovery tasks and also `TapNLinkVar` value updates or send MQTT message.

### Pin management

This part of the code will allow you to recover pulses captured by `TapNLink`’s paws. By fetching the difference of a `pin.getValue()` between two onCheck() method with the same `id`, will return the value of the pulse. Example: for the `TARGET_RESET` `Pin`, every second we will retrieve pulses that we will remove at the old value to find the number of pulses in one second. Calibration is applied to this value to find the number of joules.

To find more information for `Pin`, [it's here](http://docs.iotize.com/reference/jvm/index.html)

1. Create var for each `Pin` paw used.

    ```java
    public Pin pin_power = new Pin(Pin.TGT_NRST, 4, Pin.HIZ);
    public Pin pin_current_or_voltage = new Pin(Pin.TGT_CLK, 4, Pin.HIZ);
    public Pin pin_select = new Pin(Pin.TGT_DATA, Pin.PP_OUT, Pin.HIZ);
    ```
    * Pin `TARG_RST` recovery the power active pulse.
    * Pin `SWDCLK_TARG` recovery the electric current or voltage.
    * Pin `SWDIO_TARG` define if `Pin.CLK` return electric current or voltage (1 = voltage / 0 = current).
    
2.  Define the active power consumption.

    ```java
    powerConsumpted_W = ((pinPower.getValue() - oldPinPowerValue) * calWatt_varInTap.getValue());
    ```
    * `calWatt_varInTap.getValue()` define the calibration to convert pulse to Watt.
    * `oldPinPowerValue` return the pulse value on last `onCheck()` method call.
    
3. Recovery the electric current and voltage from device.
    
    * By default the `Pin IO` is define to voltage (default value 1).
    
    ```java
   int pin_current_or_voltage_value = pinCurrentOrVoltage.getValue();
   if ( selectCurrent0Voltage1 == 0x01) {    //voltage is selected as output
       float pinVoltage = (float)((pin_current_or_voltage_value - oldCurrentOrVoltageValue) * calVoltage_varInTap.getValue());
       voltage_varInTap.setValue((float)((pinVoltage / deltaTime_ms) * 1000.0));
       pinSelect.setValue(0x00);
       selectCurrent0Voltage1 = 0x00;   
   else  {
       float pinCurrent = (float)((pin_current_or_voltage_value - oldCurrentOrVoltageValue) * calCurrent_varInTap.getValue());
       current_varInTap.setValue((float)((pinCurrent / deltaTime_ms) * 1000.0));
       pinSelect.setValue(0x01);
       selectCurrent0Voltage1 = 0x01;
   }
   oldCurrentOrVoltageValue = pin_current_or_voltage_value;
   ```
   * Each second the pulses of the voltage or electric current are recover.
   * Each second we redefine `selectCurrent0Voltage1` for switch voltage pulse to current pulse (current 0 to voltage 1).   

### Absolute time from tap


The absolute time is recover on the `system` interface of the `JVM`. this gives us access to the `GET` method that we will execute on `/1024//79` to retrieve the `Local Time`, we pass in the method the `date` object which will contain the result. Once the date is define, it will be cut into parts of 4 bytes to retrieve the desired elements of the date. These elements will make it possible to be notified if a day, month or hour has changed and then update the `TapNLinkVar`.
 
1. Recovers the current time on `TapNLink`'s system.
    
    ```java
   oldConsumptionDay = currentDay;
   oldconsumptionMonth = currentMonth;
   oldConsumptionHour = currentHour;
   lastDaysConsumption = lastDays_varInTap.getValue();
   lastMonthsConsumption = lastMonths_varInTap.getValue();
   lastHoursConsumption = lastHours_varInTap.getValue();
    ```
    * To find more information of `system` : [it's here](http://docs.iotize.com/reference/jvm/index.html)
    * `date = [0.0.0.39.0.0.0.33.0.0.0.8.0.0.0.3.0.0.0.1.0.0.0.121.0.0.0.3.0.0.0.33.0.0.0.0]`

2. Each `onCheck()` call, compare to older hour, day and month.

    ```java
    if(oldConsumptionHour != currentHour) {}
    if(oldConsumptionDay != currentDay) {}
    if(oldConsumptionMonth != currentMonth) {}
    ```
   * Older value has refreshed every check.

### Active power consumption 

In this part we will calculate and update a large part of the `TapNLinkVar` with new values found. If a time, day or other change has taken place then the code contained in the conditions has executed.

1. Each seconds, adding new active power pulse.

   * Gathers all the pulses found since the start of tap.
   * `WATT_TO_KWATT` = 1000, to transform current W to kW.
   * `KWATT_TO_KWATTH` = 3600, to transform the current kW to kWh.
    ```java
    overall_varInTap.setValue(overall_varInTap.getValue() + (powerConsumpted_W / (WATT_TO_KWATT * KWATT_TO_KWATTH)));
    ```
   
   * Refresh consumption of the hour. 
   ```java
   consumptionOfTheHour += (powerConsumpted_W / (WATT_TO_KWATT * KWATT_TO_KWATTH));
   ```   
      
   * Refresh consumption of the day.       
   ```java
   consumptionOfTheDay += (powerConsumpted_W / (WATT_TO_KWATT * KWATT_TO_KWATTH));
   ```   
    
2. Refresh `TapNLinkVar` if something change.

    * If hour change
    ```java
     if(currentHour != oldConsumptionHour) {
       if(currentHour == 0) {
           lastHoursConsumption[lastHoursConsumption.length - 1] = consumptionOfTheHour;
       } else {
           lastHoursConsumption[currentHour - 1] = consumptionOfTheHour;
       }
       consumptionOfTheHour = 0;
       lastHours_varInTap.setValue(lastHoursConsumption);
   }
    ```
   
   * If day change
   ```java
    if(oldConsumptionDay != currentDay) {   
       for(int i = lastDaysConsumption.length - 1; i > 0; i--) {
           lastDaysConsumption[i] = lastDaysConsumption[i - 1];
       }   
       lastDaysConsumption[0] = consumptionOfTheDay;
       consumptionOfTheMonth += consumptionOfTheDay;
       consumptionOfTheDay = 0;   
       lastDays_varInTap.setValue(lastDaysConsumption);
   }
   ```
   
   * If month change
   ```java
    if(oldconsumptionMonth != currentMonth) { 
       if(currentMonth == 1) {
           lastMonthsConsumption[lastMonthsConsumption.length - 1] = consumptionOfTheMonth;
       } else {
           lastMonthsConsumption[currentMonth - 2] = consumptionOfTheMonth;
       }
       consumptionOfTheMonth = 0;   
       lastMonths_varInTap.setValue(lastMonthsConsumption);
   }
   ```

### Update dynamical a graph on app

The `JVM` can be cut for a few seconds if it tries to execute something. This is why we create an average of the consumption to smooth the result and not have a big spike of consumption at the return of the operation of the `JVM`.

```java
powerConsumpted_W = ((pinPower.getValue() - oldPinPowerValue) * calWatt_varInTap.getValue());
deltaTime_ms = system.getTime() - lastTime_ms;
powerCur_W = (float) ((powerConsumpted_W / deltaTime_ms) * 1000.0);
instantPower_varInTap.setValue(powerCur_W);
```
   
  * `system.getTime()` return time since last boot of the `TapNLink`, in milliseconds.

### Backup current day consumption

To don't forgot the current day consumption, we define backup `VarInTap` array with backup date and last day consumption value to recovery.

We do not directly use the `VarInTap` dayConsumption as a backup as it is updated every second.  It’s not recommended for no volatile `VarInTap`.

- Define on backup the current day consumption, and the current day consumption value.
```java
if(id == DAY_CONSUMPTION_ID && initDone) {
     float[] backup = new float[2];
     backup[0] = backupDayConsumption;
     backup[1] = dayConsumption_varInTap.getValue();
     backupConsumption_varInTap.setValue(backup);
}
```

- On `init()` method, recovery the consumption of the day if `backupDayConsumption` didn't change.
```java
if((int) backupConsumption_varInTap.getValue()[0] == backupDayConsumption) {
    consumptionOfTheDay = backupConsumption_varInTap.getValue()[1];
    dayConsumption_varInTap.setValue(backupConsumption_varInTap.getValue()[1]);
}
```


- Refresh `backupDayConsumption` every `onCheck()` call on `currentTime()` method.
```java
int currentCalendarToBackup = Integer.valueOf("" + date[12] + date[13] + date[14] + date[15]);
int currentYearsToBackup = Integer.valueOf("" + date[20] + date[21] + date[22] + date[23]) ;
backupDayConsumption = (currentMonth * 31) + currentCalendarToBackup + (currentYearsToBackup * 365);
```
