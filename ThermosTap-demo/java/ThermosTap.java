import com.iotize.jvm.*;
import com.iotize.jvm.hal.*;

/**
 *  JVM client code
 *  Example for thermostat
 *  The purpose of this example is to show the example of a radiator management with a Tap and a generated app.
 *  This simulation works with an LED to demonstrate the ignition and stoppage of a heater.
 *  The part of the JVM is to retrieve values in order to process.
 */
public class ThermosTap {

    /**
     * Class constructor with instantiation of the MQTT topic
     * Then send mqtt message for init class done.
     */
    ThermosTap() {
        byte[] topicPrefixPayload = new byte[256];
        system.engineCommand(TapNLinkSys.GET, RESOURCE_ID, 0, 58, topicPrefixPayload ); 
        this.topic = new String(topicPrefixPayload) + "/result";
    }

    private TapNLinkSys system;
    private String topic;

    private final int RESOURCE_ID               = 1024;
    private final int BASIC_FREQ_CHECK          = 2 * 1000;
    private final int STATE_FREQ                = 5000;
    private final int ACTIVE_PERCENTAGE_ID      = 0X05; 
    private final int CURRENT_MODE_ID           = 0x02;
    private final int FREE_FROST_TEMP_ID        = 0x06;
    private final int ECO_TEMP_ID               = 0x07;
    private final int COMFORT_TEMP_ID           = 0x01;
    private final int AUTO_RECOVERY_ID          = 0x17;
    private final int DAYS_CONFIG_ID            = 0x04;
    private final int PROGRAM_WE_ID             = 0x0b;
    private final int PROGRAM_WD_ID             = 0x0a;
    private final int TARGET_TEMP_ID            = 0x12;
    private final int CURRENT_TEMP_ID           = 0x13;
    private final int HYSTERESIS_ID             = 0x03;

    // -------------------------------------------------
    //  TapNLinkVar, only InTap value (volatile or not)    
    // -------------------------------------------------

    // The percentage of currentPowerState time of the radiator
    public TapNLinkVarFloat                activePercentage_varInTap   = new TapNLinkVarFloat((short) ACTIVE_PERCENTAGE_ID, "activePercentage", BASIC_FREQ_CHECK);
    // Current mode : 0 = Off / 1 = Auto / 2 = WE / 3 = WD / 4 = ecoTemp_varInTap / 5 = comfortTemp_varInTap / 6 = FREE-FROST
    public TapNLinkVarInt                  currentMode_varInTap        = new TapNLinkVarInt((short) CURRENT_MODE_ID, VariableType.UINT8, "currentMode", BASIC_FREQ_CHECK);
    // Free-frost mode temperature
    public TapNLinkVarInt                  freeFrostTemp_varInTap      = new TapNLinkVarInt((short) FREE_FROST_TEMP_ID, VariableType.UINT8, "freeFrost", BASIC_FREQ_CHECK);
    // ecoTemp_varInTap mode temperature
    public TapNLinkVarInt                  ecoTemp_varInTap            = new TapNLinkVarInt((short) ECO_TEMP_ID, VariableType.UINT8, "eco", BASIC_FREQ_CHECK);
    // comfortTemp_varInTap mode temperature
    public TapNLinkVarInt                  comfortTemp_varInTap        = new TapNLinkVarInt((short) COMFORT_TEMP_ID, VariableType.UINT8, "comfort", BASIC_FREQ_CHECK);
    // 
    public TapNLinkVarInt                  autoRecovery_varInTap        = new TapNLinkVarInt((short) AUTO_RECOVERY_ID, VariableType.UINT8, "autoRecovery", BASIC_FREQ_CHECK);
    // init by dayconfig, status of days. 1 = Week-end / 2 = Work-days
    public TapNLinkVarByteArray            daysConfiguration_varInTap   = new TapNLinkVarByteArray((short) DAYS_CONFIG_ID, "dayConfig", BASIC_FREQ_CHECK, 7);
    // Represents the 24 hours of a day of week-end with its defined mode. 1 = FREE-FROST / 2 = ecoTemp_varInTap / 3 = comfortTemp_varInTap
    public TapNLinkVarByteArray            programWE_varInTap           = new TapNLinkVarByteArray((short) PROGRAM_WE_ID, "program_WE", BASIC_FREQ_CHECK, 24);
    // Represents the 24 hours of a day of week-days with its defined mode. 1 = FREE-FROST / 2 = ecoTemp_varInTap / 3 = comfortTemp_varInTap
    public TapNLinkVarByteArray            programWD_varInTap           = new TapNLinkVarByteArray((short) PROGRAM_WD_ID, "program_WD", BASIC_FREQ_CHECK, 24);
    // The current temperature
    public TapNLinkVarFloat                currentTemp_varInTap         = new TapNLinkVarFloat((short) CURRENT_TEMP_ID, "currentTemp", STATE_FREQ);
    // hysteresis_varInTap settings is the amount of narrowness or wideness of temperature readings
    public TapNLinkVarFloat                hysteresis_varInTap          = new TapNLinkVarFloat((short) HYSTERESIS_ID, "hysteresis", BASIC_FREQ_CHECK);
    // Temperature targeted by the mode
    public TapNLinkVarFloat                targetTemp_varInTap          = new TapNLinkVarFloat((short) TARGET_TEMP_ID, "targetTemp", BASIC_FREQ_CHECK);

    // Switch led to on/off for demo
    public Pin                             pinTurnPowerOnOff            = new Pin(Pin.PA2, Pin.PP_OUT, Pin.HIZ);
    // Get current temperature with Volt on TempSensor
    public Pin                             pinTempSensor                = new Pin(Pin.PB3, Pin.ANA, Pin.HIZ);

    // settings currentTime var
    private byte[]              date                = new byte[36];
    // Defines the ignition status of the led
    private boolean             currentPowerState   = false;
    // Current day
    private int                 currentDay          = 0;
    // Current hour
    private int                 currentTimeHour     = 0;
    // Old hour for auto recoTemp_varInTapvery
    private int                 oldTimeHour         = 0;
    // Target temperature
    private int                 targetTemperature   = 0;
    // Total time to compare for percentage
    private int                 overallTime         = 0;
    // currentPowerState time to compare to total time for percentage
    private int                 powerActiveTime     = 0;


    /**
     * this method is called for each variable with a TapNLinkVar constructor.
     * The method recoTemp_varInTapvers as id that of the VarInTap which calls its check.
     * The frequency of this method call is defined in the TapNLinkVar constructor
     */
    public void check(final int id) {

        /**
        * This method retrieves the current temperature. A calculation is made to recoTemp_varInTapver the exact temperature 
        */
        if(id == CURRENT_TEMP_ID) {
            currentTemp_varInTap.setValue((float)(50 - (pinTempSensor.getValue() * 0.01015)));
        }
      
        if(id == TARGET_TEMP_ID) {
            // Runs a method to retrieve the current time and day
            currentTime();
            // Checks whether auto mode should be followed or not
            // If yes, the current mode is redefined by auto
            // Else retrieve the current mode and perform an action based on it
            try {
                if(autoRecovery_varInTap.getValue() == 0x01 && oldTimeHour != currentTimeHour) {
                    // Switch to mode auto
                    currentMode_varInTap.setValue(0x01);
                    // Retrieves the status of the day
                    int dayConfig = getDayConfig(currentDay);
                    if(dayConfig == 1) {
                        targetTemperature = getTempByMode(programWE_varInTap.getValue()[currentTimeHour]);
                    } else {
                        targetTemperature = getTempByMode(programWD_varInTap.getValue()[currentTimeHour]);
                    }
                } else {
                    // A loop to retry the action according to the current mode
                    switch(currentMode_varInTap.getValue()) {
                        // MODE OFF
                        case 0x00 :
                            targetTemperature = 0;
                            break;
                        // MODE AUTO
                        case 0x01 :
                            int dayConfig = getDayConfig(currentDay);
                            // If auto mode is enabled then we check the status of the day to see if we are running a program we or wd
                            if(dayConfig == 1) {
                                targetTemperature = getTempByMode(programWE_varInTap.getValue()[currentTimeHour]);
                            }else {
                                targetTemperature = getTempByMode(programWD_varInTap.getValue()[currentTimeHour]);
                            }
                            break;
                        // MODE MODE_FORCE_WE
                        case 0x02 :
                            targetTemperature = getTempByMode(programWE_varInTap.getValue()[currentTimeHour]);
                            break;
                        // MODE_FORCE_WD
                        case 0x03 : 
                            targetTemperature = getTempByMode(programWD_varInTap.getValue()[currentTimeHour]);
                            break;
                        // MODE_ecoTemp_varInTap
                        case 0x04 :
                            targetTemperature = ecoTemp_varInTap.getValue();
                            break;
                        // MODE_comfortTemp_varInTap
                        case 0x05 :
                            targetTemperature = comfortTemp_varInTap.getValue();
                            break;
                        // MODE_FREE_FROST
                        case 0x06 :
                            targetTemperature = freeFrostTemp_varInTap.getValue();
                            break;
                    }
                }
                // Change oldTimeHour to inform autoRecovery_varInTap if he should return to auto mode.
                oldTimeHour = currentTimeHour;
            } catch (TapNLinkException e) {
                system.sendMQTTMessage(topic,"Error when settings temperature to compare, error code : " + e.getCode(), 0);
            }
            
        
            // Updates application display to alert target temperature
            targetTemp_varInTap.setValue((float) targetTemperature);
            
            // If the temperature is smaller than the targeted one reduced by hysteresis_varInTap. Then turn on the led
            // On activation the consumption in currentPowerState minute is counted
            // Every day consumption goes back to zero
            if(currentTemp_varInTap.getValue() <= ( targetTemperature - hysteresis_varInTap.getValue() )) {   
                currentPowerState = true;
            } else if(currentTemp_varInTap.getValue() >= ( targetTemperature + hysteresis_varInTap.getValue()) ){
                currentPowerState = false;
            } 

            // Calculates the percentage of currentPowerState time of the radiator since its launch
            overallTime += (STATE_FREQ / 1000);
            if(currentPowerState) {
                powerActiveTime += (STATE_FREQ / 1000);
            }
            activePercentage_varInTap.setValue((float) powerActiveTime / overallTime * 100);

            // If last temp is lower than mode temperature activa is true
            // If currentPowerState is true, pin is turn ON
            // If currentPowerState is false, pin is turn OFF
            try {
                if(currentPowerState) {
                    pinTurnPowerOnOff.setValue(0x00);
                } else {
                    pinTurnPowerOnOff.setValue(0x01);
                } 
            } catch (TapNLinkException e) {
                system.sendMQTTMessage(topic,"Error on method setting set Pin 0x01 or 0x00, error code : " + e.getCode(), 0);
            }
        }
    }

    /**
     * RecoTemp_varInTapvers the temperature according to the requested mode
     * Returns the desired mode temperature
     */
    public int getTempByMode(int mode) {
        int tempToSelectMode = 0;
        try {
            switch(mode) {
                case 0x01 :   
                    // MODE FREE-FROST
                    tempToSelectMode = freeFrostTemp_varInTap.getValue();
                    break;
                case 0x02 :
                    // MODE ecoTemp_varInTap
                    tempToSelectMode = ecoTemp_varInTap.getValue();
                    break;
                case 0x03 :
                    // MODE comfortTemp_varInTap
                    tempToSelectMode = comfortTemp_varInTap.getValue();
                    break;
            } 
        } catch (TapNLinkException e) {
            system.sendMQTTMessage(topic,"Error on method getTemByMode(), error code : " + e.getCode(), 0);
        }
        return tempToSelectMode;
    }

    /**
     * Retrieves the status of the day to find out if it is considered a weekend day or the work day
     * 1 = week ends / 2 = work days
     */
    public int getDayConfig(int day) {
        return daysConfiguration_varInTap.getValue()[day];
    }

    /**
     * This method retrieves the current time. The time object retrieves a list of 9 elements containing all the current time information.
     */
    public void currentTime() {
        try {
            // Resource id 79 for the lwM2M provides "Local Time"
            system.engineCommand(TapNLinkSys.GET, RESOURCE_ID, 0x00, 79, date);
            String hour = "" + date[8] + date[9] + date[10] + date[11];
            String day = "" + date[24] + date[25] + date[26] + date[27];
            currentDay = (int) Integer.valueOf(day);
            currentTimeHour = (int) Integer.valueOf(hour);
        } catch (TapNLinkException e) {
            system.sendMQTTMessage(topic,"Error on method currentTime(), error code : " + e.getCode(), 0);
        }
    }

    /**
     * This method is called in case of major error in the code, then the check method will no longer be called
     */
    public void onException(int errorcode, int par1, int par2) {
        system.sendMQTTMessage(topic, String.format("Error code : " + errorcode, errorcode, par1, par2), 0);
    }
}