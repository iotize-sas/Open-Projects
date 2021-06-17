import com.iotize.jvm.*;
import com.iotize.jvm.hal.*;


/**
 * This java class is intended to manage the values recovered by the device to serve as WattTap.
 * We count pulses via a Pin that define the active power consumption of the connected device.
 * With the recovered data the consumption is calculated.
 */
public class WattTap {

    /**
     * Class constructor with instantiation of the MQTT topic
     * Then send mqtt message for init class done.
     */
    WattTap() {
        byte[] topicPrefixPayload = new byte[256];
        // Resource id 58 get the resource "MQTT Relay topic".
        system.engineCommand(TapNLinkSys.GET, RESOURCE_ID, 0, 58, topicPrefixPayload ); 
        this.topic = new String(topicPrefixPayload) + "/result";
    }

    // ---------------
    //  FINAL INT VAR
    // ---------------

    private final int   LONG_FREQ_CHECK         = 4 * 60 * 1000;
    private final int   SHADOW_FREQ_CHECK         = 10*  60 * 1000;
    private final int   BASIC_FREQ_CHECK        = 1000;
    private final int   RESOURCE_ID             = 1024;
    private final int   DAY_CONSUMPTION_ID      = 0x01;
    private final int   OVERALL_ID              = 0x03;
    private final int   CAL_WATT_ID             = 0x04;
    private final int   MIN_WATT_ID             = 0x05;
    private final int   LAST_DAYS_ID            = 0x06;
    private final int   MAX_WATT_ID             = 0x07;
    private final int   LAST_MONTHS_ID          = 0x08;
    private final int   AWS_SHADOW_UPDATE_ID    = 0x09;
    private final int   INSTANT_POWER_ID        = 0x0a;
    private final int   LAST_HOURS_ID           = 0x0b;
    private final int   VOLTAGE_ID              = 0x0c;
    private final int   CAL_VOLTAGE_ID          = 0x0d;
    private final int   INTENSITY_ID            = 0x0e;
    private final int   BACKUP_ID               = 0x0f;
    private final int   CAL_CURRENT_ELECTRIC_ID = 0x10;
    private final int   GENERAL_RESET_ID        = 0x11;
    private final int   COUNTER_ID              = 0x12;
    private final int   WATT_TO_KWATT           = 1000;
    private final int   KWATT_TO_KWATTH         = 3600;

    private TapNLinkSys system;
    private String topic;

    // -------------------------------------------------
    //  TapNLinkVar, only InTap value (volatile or not)    
    // -------------------------------------------------

    public  TapNLinkVarFloatArray           counter_varInTap                    = new TapNLinkVarFloatArray((short) COUNTER_ID, "COUNTER", BASIC_FREQ_CHECK, 3);
    // dayConsumption_varInTap : Contains current day consumption.
    public  TapNLinkVarFloat                dayConsumption_varInTap             = new TapNLinkVarFloat((short) DAY_CONSUMPTION_ID, "day_consumption", LONG_FREQ_CHECK);
    // backupConsumption_varInTap : Save the day consumption, to restore when tap is rebooting.
    public  TapNLinkVarFloatArray           backupConsumption_varInTap          = new TapNLinkVarFloatArray((short) BACKUP_ID, "backupConsumption", BASIC_FREQ_CHECK, 3);
    // overall_varInTap : Contains active power consumption since tap start.
    public  TapNLinkVarFloat                overall_varInTap                    = new TapNLinkVarFloat((short) OVERALL_ID, "overall", BASIC_FREQ_CHECK);
    // lastDays_varInTap : Contains a table to display the last 30 days of use.
    public  TapNLinkVarFloatArray           lastDays_varInTap                   = new TapNLinkVarFloatArray((short) LAST_DAYS_ID, "lastDays", BASIC_FREQ_CHECK, 7);
    // lastMonths_varInTap : Contains a table to display the last 12 months of use.
    public  TapNLinkVarFloatArray           lastMonths_varInTap                 = new TapNLinkVarFloatArray((short) LAST_MONTHS_ID, "lastMonths", BASIC_FREQ_CHECK, 12);
    // lastHours_varInTap : Contains a table to display the 24 hours of use.
    public  TapNLinkVarFloatArray           lastHours_varInTap                  = new TapNLinkVarFloatArray((short) LAST_HOURS_ID, "lastHours", BASIC_FREQ_CHECK, 24);
    // instantPower_varInTap : Defines the active power of the device
    public  TapNLinkVarFloat                instantPower_varInTap               = new TapNLinkVarFloat((short) INSTANT_POWER_ID, "instantPower", BASIC_FREQ_CHECK);
    // maxPower_W_varInTap : Contains the highest value in Watt recorded.
    public  TapNLinkVarFloat                maxPower_W_varInTap                 = new TapNLinkVarFloat((short) MAX_WATT_ID, "maxPower", BASIC_FREQ_CHECK);
    // minPower_W_varInTap : Contains the smallest value in Watt recorded
    public  TapNLinkVarFloat                minPower_W_varInTap                 = new TapNLinkVarFloat((short) MIN_WATT_ID, "minPower", BASIC_FREQ_CHECK);
    // calWatt_varInTap : Defines the calibration watt for optimise the active power consumption.
    public  TapNLinkVarFloat                calWatt_varInTap                    = new TapNLinkVarFloat((short) CAL_WATT_ID, "calWatt", BASIC_FREQ_CHECK);
    // calVoltage_varInTap : Defines the calibration watt for optimise the active power consumption.
    public  TapNLinkVarFloat                calVoltage_varInTap                 = new TapNLinkVarFloat((short) CAL_VOLTAGE_ID, "calVoltage", BASIC_FREQ_CHECK);
    // calCurrent_varInTap : Defines the calibration watt for optimise the active power consumption.
    public  TapNLinkVarFloat                calCurrent_varInTap                 = new TapNLinkVarFloat((short) CAL_CURRENT_ELECTRIC_ID, "calCurrent", BASIC_FREQ_CHECK);
    // awsShadowUpdate_varInTap : Refreshing aws_shadow for thing WattTap
    public  TapNLinkVarInt                  awsShadowUpdate_varInTap            = new TapNLinkVarInt((short) AWS_SHADOW_UPDATE_ID, VariableType.INT8, "updateShadow", SHADOW_FREQ_CHECK);
    // generalReset_varInTap : When this value is define to 1, the all VarInTap value has define to 0.
    public  TapNLinkVarInt                  generalReset_varInTap               = new TapNLinkVarInt((short) GENERAL_RESET_ID, VariableType.INT8, "generalReset", BASIC_FREQ_CHECK);
    // current_varInTap : Contains the electric current find by the CLK Pin
    public  TapNLinkVarFloat                current_varInTap                    = new TapNLinkVarFloat((short) INTENSITY_ID, "electricCurrent", BASIC_FREQ_CHECK);
    // voltage_varInTap : Contains the voltage find by the CLK Pin
    public  TapNLinkVarFloat                voltage_varInTap                    = new TapNLinkVarFloat((short) VOLTAGE_ID, "voltage", BASIC_FREQ_CHECK);
    // pinPower : Recovers the pulses of the Pin on the TARG_RST.
    public  Pin                             pinPower                            = new Pin(Pin.TGT_NRST, 4, Pin.HIZ);
    // pinCurrentOrVoltage : Recovers the pulses of the Pin on the SWDCLK_TARG.
    public  Pin                             pinCurrentOrVoltage                 = new Pin(Pin.TGT_CLK, 4, Pin.HIZ);
    // pinSelect : Define value of the Pin on the SWDIO_TARG.
    public  Pin                             pinSelect                           = new Pin(Pin.TGT_DATA, Pin.PP_OUT, Pin.HIZ);
    

    // date : Define de absolute time from tap
    private byte[]          date                                = new byte[36];
    // lastDaysConsumption : This table is used to store the consumption of the last 30 days
    private float[]         lastDaysConsumption                 = new float[7];
    // lastMonthsConsumption : This table is used to store the consumption of the last 12 months
    private float[]         lastMonthsConsumption               = new float[12];
    // lastHoursConsumption : This table is used to store the consumption of the last 24 hours
    private float[]         lastHoursConsumption                = new float[24];
    // consumptionOfTheDay : Consumption for the current day
    private float           consumptionOfTheDay                 = 0;
    // consumptionOfTheMonth : Consumption for the current month
    private float           consumptionOfTheMonth               = 0;
    // consumptionOfTheHour : Consumption for the current hour
    private float           consumptionOfTheHour                = 0;
    // oldPinPowerValue : Saves the old value given by the Pin
    private float           oldPinPowerValue                    = 0;
    // powerConsumpted_W : Saves active power consumption for one second
    private float           powerConsumpted_W                   = 0;
    // powerCur_W : The dynamic graph of the active power consumption.
    private float           powerCur_W                          = 0;
    // initDone : Save if init method was launched
    private boolean         initDone                            = false;
    // lastTime_ms : The old time since last boot of the Tap
    private int             lastTime_ms                         = 0;
    // deltaTime_ms : Define the current time find by the absolut time
    private float           deltaTime_ms                        = 0;
    // selectCurrent0Voltage1 : Define the current select on Pin Target_DATA, default value 1
    private byte            selectCurrent0Voltage1              = 0x01;
    // oldCurrentOrVoltageValue : Define the old pin value find on Pin Target_CLK, can be current or voltage
    private int             oldCurrentOrVoltageValue            = 0;
    // backupDayConsumption : Save the current day, then when the tap is rebooting we can check the backupDayConsumption to recovery day consummption or not
    private int             backupDayConsumption                = 0;
    // currentDay, currentMonth, currentHour : Retrieves the day and month from the date table
    private int             currentDay, currentMonth, currentHour;
    // oldConsumptionDay, oldconsumptionMonth, oldConsumptionHour : Backup the day and month of consumption to prevent the change of day or month
    private int             oldConsumptionDay, oldconsumptionMonth, oldConsumptionHour;

    private float           overallValueFromStartCounter = 0;
    private int             counterTimeAtStart           = 0;
    private int		counterPause = 0;
private float startPauseOverall = 0;
private float counterOverallPause = 0;
private boolean startPause = false;
private int startPauseTime = 0;
    
    /**
     * onCheck method is called for each TapNLinkVar declared in the Java class. This frequency is defined in the TapNLinkVar constructors
     */
    public void onCheck(final int id) {

        if(id == COUNTER_ID) {
            float[] counterTab = counter_varInTap.getValue();
            if(counterTab[1] == 1) {
                if(startPause) {
                    counterPause = system.getTime() - startPauseTime;
	                counterOverallPause = overall_varInTap.getValue() - startPauseOverall;
                    overallValueFromStartCounter += counterOverallPause;
                    counterTimeAtStart += counterPause;
                }
                if(overallValueFromStartCounter == 0) {
                    overallValueFromStartCounter = overall_varInTap.getValue();
                }
                if(counterTimeAtStart == 0) {
                    counterTimeAtStart = system.getTime();
                }
                counterTab[0] = overall_varInTap.getValue() - overallValueFromStartCounter;
                counterTab[2] = system.getTime() - counterTimeAtStart;
                startPause = false;
            } else if (counterTab[1] == 2) {
                counterTab[0] = 0;
                counterTab[1] = 0;
                counterTab[2] = 0;
                counterTimeAtStart = 0;
	            counterPause = 0;
                overallValueFromStartCounter = 0;
	            counterOverallPause = 0;
                startPauseOverall = 0;
                startPauseTime= 0;
                startPause = false;
            } else if (counterTab[1] == 3) {
                if(!startPause) {
	                startPauseTime = system.getTime();
	                startPauseOverall = overall_varInTap.getValue();
                }
	            startPause = true;
	            counterPause = system.getTime() - startPauseTime;
	            counterOverallPause = overall_varInTap.getValue() - startPauseOverall;
            }
            counter_varInTap.setValue(counterTab);
        }

        /**
         * Updates the backup to restore day consumption.
         * every LONG_FREQ_CHECK, save the current day consumption.
         */
        if(id == DAY_CONSUMPTION_ID && initDone) {
            float[] backup = new float[3];
            backup[0] = backupDayConsumption;
            backup[1] = consumptionOfTheDay;
            backup[2] = overall_varInTap.getValue();
            backupConsumption_varInTap.setValue(backup);
        }

        /**
         * Updates the Shadow of Thing WattTap on AWS via MQTT message.
         * if value = 0, the message doesn't be send.
         */
        if(id == AWS_SHADOW_UPDATE_ID &&  awsShadowUpdate_varInTap.getValue() == 1) {
            system.sendMQTTMessage("$aws/things/WattMeter/shadow/name/inUse/update",
                "{\"state\": {\"reported\": {\"ID\": \"WattMeter\", \"CurrentDayConsumption\": " + consumptionOfTheDay + " ,\"maxWattInUse\": "+ maxPower_W_varInTap.getValue() +"}}}", 0);
        }

        // This part calculates according to the pulses recovered the consumption and the active power. This method has called every second.
        if(id == INSTANT_POWER_ID) {
            // Recovery the absolute time from lwM2M.
            currentTime();

            if(!initDone) {
                init();
            }

            // Calculate the active power  and energy consumption since the last call to onCheck().
            if(pinPower.getValue() >= oldPinPowerValue) {
                powerConsumpted_W = (pinPower.getValue() - oldPinPowerValue) * calWatt_varInTap.getValue();
            } else {
                powerConsumpted_W = pinPower.getValue() * calWatt_varInTap.getValue();
            }

            if(system.getTime() >= lastTime_ms) {
                deltaTime_ms = system.getTime() - lastTime_ms;
            } else {
                deltaTime_ms = system.getTime();
            }
            powerCur_W = (float) ((powerConsumpted_W / deltaTime_ms) * 1000.0);

            // Save current time for the next (future) call to onCheck().
            lastTime_ms = system.getTime();

            int pin_current_or_voltage_value = pinCurrentOrVoltage.getValue();

            //alternatively we measure either voltage or current
            if ( selectCurrent0Voltage1 == 0x01) { // voltage is selected as output
                // Update voltage value.
                float pinVoltage = (float)((pin_current_or_voltage_value - oldCurrentOrVoltageValue) * calVoltage_varInTap.getValue());
                voltage_varInTap.setValue((float)((pinVoltage / deltaTime_ms) * 1000.0));
                pinSelect.setValue(0x00); // Select 'current' (instead of 'voltage')  measurement 
                selectCurrent0Voltage1 = 0x00;  //switch to current for the next time
            } else  { // current is selected as output
                // Update current value.
                float pinCurrent = (float)((pin_current_or_voltage_value - oldCurrentOrVoltageValue) * calCurrent_varInTap.getValue());
                current_varInTap.setValue((float)((pinCurrent / deltaTime_ms) * 1000.0));
                pinSelect.setValue(0x01); // Select 'voltage' (instead of 'current')  measurement 
                selectCurrent0Voltage1 = 0x01;  //switch to voltage for the next time
            }

            oldCurrentOrVoltageValue = pin_current_or_voltage_value;

            // Update the MIN / MAX values if any change
            if (powerCur_W > maxPower_W_varInTap.getValue()) {
                maxPower_W_varInTap.setValue(powerCur_W);
            } else if((powerCur_W < minPower_W_varInTap.getValue()) && (powerCur_W > 1)) {
                minPower_W_varInTap.setValue((short) powerCur_W);
            } else if(minPower_W_varInTap.getValue() == 0) {
                minPower_W_varInTap.setValue(powerCur_W);
            }

            instantPower_varInTap.setValue(powerCur_W);
            // Refresh TapNLinkVar with recent data find.
            overall_varInTap.setValue(overall_varInTap.getValue() + (powerConsumpted_W / (WATT_TO_KWATT * KWATT_TO_KWATTH)));
            // Refresh the consumption of the current hour. 
            consumptionOfTheHour += (powerConsumpted_W / (WATT_TO_KWATT * KWATT_TO_KWATTH));
            // Define the current consumption on the day with current active power, (active power  / ( Watt to kW * kW to kWh ))
            consumptionOfTheDay += (powerConsumpted_W / (WATT_TO_KWATT * KWATT_TO_KWATTH));

            // Check if the current hour is already the same has last check call
            if(currentHour != oldConsumptionHour) {
                lastHoursConsumption[oldConsumptionHour] = consumptionOfTheHour;
                // Check is the current day is already the same has last check call.
                if(oldConsumptionDay != currentDay) {
	      lastDaysConsumption[oldConsumptionDay - 1] = consumptionOfTheDay;
                    /**
                    * If the month has changed then the previous month�s consumption is updated in the table
                    */
                   lastMonthsConsumption[oldconsumptionMonth - 1] += consumptionOfTheDay;

                    if(oldconsumptionMonth != currentMonth) { 
                        lastMonthsConsumption[currentMonth - 1] = 0;
                    }
                    lastMonths_varInTap.setValue(lastMonthsConsumption);
                    // The current day change, then we restart value from consumptionOfTheDay to 0.
                    consumptionOfTheDay = 0;
                    // Refresh VarInTap for the lastDays_varInTap array with recent data.
                    lastDays_varInTap.setValue(lastDaysConsumption);
                }
                consumptionOfTheHour = 0;
                lastHours_varInTap.setValue(lastHoursConsumption);
            }
            // Redefines these variables so as not to miss the day or month change
            refreshOldDate();

            // Refresh TapNLinkVar consumptionOfTheDay with recent data.
            dayConsumption_varInTap.setValue(consumptionOfTheDay);
            
            // Refresh value to filter the next check call. Set oldValue with current pinPower value.
            oldPinPowerValue = pinPower.getValue(); 
        }


        /**
         * Reset the current state value.
         * overall consumption max and min pulse has define to 0.
         * Then the generalReset_varInTap value has define to 0.
         */
        if(id == GENERAL_RESET_ID && generalReset_varInTap.getValue() == 0x01) {
            overall_varInTap.setValue(0);
            maxPower_W_varInTap.setValue(0);
            minPower_W_varInTap.setValue(0);
            generalReset_varInTap.setValue(0x00);
            backupConsumption_varInTap.setValue(new float[3]);
            consumptionOfTheDay = 0;
            consumptionOfTheHour = 0;
            consumptionOfTheMonth = 0;
            lastDays_varInTap.setValue(new float[30]);
            lastDaysConsumption = new float[30];
            lastMonths_varInTap.setValue(new float[12]);
            lastMonthsConsumption = new float[12];
            lastHours_varInTap.setValue(new float[24]);
            lastHoursConsumption = new float[24];
            dayConsumption_varInTap.setValue(0);
        }
    }

    /**
     * Initialize the current time and the current consumption.
     */
    public void init() {
        // old value has not init then when define with current day, month and hour. 
        oldConsumptionDay = currentDay;
        oldconsumptionMonth = currentMonth;
        oldConsumptionHour = currentHour;
        // If the day didn't change, recovery the backup of the day consumption.
        if((int) backupConsumption_varInTap.getValue()[0] == backupDayConsumption) {
            consumptionOfTheDay = backupConsumption_varInTap.getValue()[1];
            dayConsumption_varInTap.setValue(backupConsumption_varInTap.getValue()[1]);
        }
        overall_varInTap.setValue(backupConsumption_varInTap.getValue()[2]);
        // If the jvm has been reboot, we recovered no volatile value on tap.
        lastDaysConsumption = lastDays_varInTap.getValue();
        lastMonthsConsumption = lastMonths_varInTap.getValue();
        lastHoursConsumption = lastHours_varInTap.getValue();
        initDone = true;
    }

    /**
    * Save the last time we called onCheck
    */
    public void refreshOldDate() {
        oldConsumptionDay = currentDay;
        oldconsumptionMonth = currentMonth;
        oldConsumptionHour = currentHour;
    }

   /**
    * Recover absolute time with a GET command to the lwM2M system.
    * Each value is coded on 4 consecutive bytes
    */ 
    public void currentTime() {
        try {
            // Resource id 79 for the lwM2M provides "Local Time"
            system.engineCommand(TapNLinkSys.GET, RESOURCE_ID, 0x00, 79, date);
            String hour = "" + date[8] + date[9] + date[10] + date[11];
            String month = "" + date[16] + date[17] + date[18] + date[19];
            String day = "" + date[24] + date[25] + date[26] + date[27];
            currentHour = (int) Integer.valueOf(hour);
            currentDay = (int) Integer.valueOf(day);
            currentMonth = (int) Integer.valueOf(month) + 1;
            // Define the backupDayConsumption to compare with current day to restore day consumption.
            int currentCalendarToBackup = Integer.valueOf("" + date[12] + date[13] + date[14] + date[15]);
            int currentYearsToBackup = Integer.valueOf("" + date[20] + date[21] + date[22] + date[23]) ;
            backupDayConsumption = (currentMonth * 31) + currentCalendarToBackup + (currentYearsToBackup * 365);
        } catch (TapNLinkException e) {
            system.sendMQTTMessage(topic,"Error on method currentTime(), error code : " + e.getCode(), 0);
        }
    }
    
    /**
     * This method is called in case of major error in the code, then the onCheck method will no longer be called
     */
    public void onException(int errorcode, int par1, int par2) {
        system.sendMQTTMessage(topic, String.format("Error code : " + errorcode, errorcode, par1, par2), 0);
    }
}
