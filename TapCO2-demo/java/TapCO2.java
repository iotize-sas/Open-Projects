import com.iotize.jvm.*;
import com.iotize.jvm.hal.*;


public class TapCO2 {

    private final int   LONG_FREQ_CHECK         = 4 * 60 * 1000;    
    private final int   BASIC_FREQ_CHECK        = 2000;
    private final int   RESOURCE_ID             = 1024;
    private final int   DEFAULT_I2C_ADDRESS     = 0x29;
    private final int   INSTANT_VALUE_ID        = 0x02;
    private final int   OVERALL_AVERAGE_ID      = 0x05;
    private final int   CURRENT_HOUR_AVERAGE_ID = 0x07;
    private final int   CURRENT_DAY_AVERAGE_ID  = 0x01;
    private final int   BACKUP_ID               = 0x03;
    private final int   LAST_HOURS_ID           = 0x08;
    private final int   LAST_DAYS_ID            = 0x09;
    private final int   CALIBRATION_MODE_ID     = 0x0b;
    private final int   VALUE_CALIBRATION_ID    = 0x0c;
    private final int   WARNING_LIMIT_ID        = 0x04;
    private final int   TEMPERATURE_ID          = 0x06;

    /**
    * This creates a new I2C interface, with a 50kbit/s baud rate. 
    */
    private I2c                             instanceI2C  = new I2c(50000);

    private TapNLinkSys                     system;
    private String                          topic;

    // initDone : Save if init method was launched
    private boolean                         initDone                                = false;
    // currentDay, currentMonth, currentHour : Retrieves the day and month from the date table
    private int                             currentDay, currentHour;
    // oldDay, oldHour : Backup the day and hour
    private int                             oldDay, oldHour;
    // backupTime : Recovery the time to check the last backup time.
    private int                             backupTime                              = 0;
    // date : Define de absolute time from tap
    private byte[]                          date                                    = new byte[36];
    // lastDays : Contains the average of the last 30 days.
    private float[]                         lastDays                                = new float[7];
    // lastHours : Contains the average of the last 24 hours.
    private float[]                         lastHours                               = new float[24];
    // loopCount : [0] = overall / [1] = hour / [2] day
    private float[]                         loopCount                               = new float[3];

    // -------------------------------------------------
    //  TapNLinkVar, only InTap value (volatile or not)    
    // -------------------------------------------------

    
    // warningLimit_varInTap : contains the limit of the current average to send AWS message if this one has passed.
    public  TapNLinkVarInt                  valueCalibration_varInTap               = new TapNLinkVarInt((short) VALUE_CALIBRATION_ID, VariableType.INT8, "valueCalibration", BASIC_FREQ_CHECK);
    // warningLimit_varInTap : contains the limit of the current average to send AWS message if this one has passed.
    public  TapNLinkVarInt                  calibrationMode_varInTap                = new TapNLinkVarInt((short) CALIBRATION_MODE_ID, VariableType.INT8, "calibrationMode", BASIC_FREQ_CHECK);
    // warningLimit_varInTap : contains the limit of the current average to send AWS message if this one has passed.
    public  TapNLinkVarInt                  warningLimit_varInTap                   = new TapNLinkVarInt((short) WARNING_LIMIT_ID, VariableType.INT8, "warningLimit", BASIC_FREQ_CHECK);
    // currentHourAverage_varInTap : contains the average of the current hour.
    public  TapNLinkVarFloat                currentHourAverage_varInTap             = new TapNLinkVarFloat((short) CURRENT_HOUR_AVERAGE_ID, "overallAverage", BASIC_FREQ_CHECK);
    // currentDayAverage_varInTap : contains the average of the current day.
    public  TapNLinkVarFloat                currentDayAverage_varInTap              = new TapNLinkVarFloat((short) CURRENT_DAY_AVERAGE_ID, "overallAverage", LONG_FREQ_CHECK);
    // overallAverage_varInTap : contains the average from the scratch.
    public  TapNLinkVarFloat                overallAverage_varInTap                 = new TapNLinkVarFloat((short) OVERALL_AVERAGE_ID, "overallAverage", BASIC_FREQ_CHECK);
    // instantValue_varInTap : show the current CO2 in air.
    public  TapNLinkVarFloat                instantValue_varInTap                   = new TapNLinkVarFloat((short) INSTANT_VALUE_ID, "instantValue", BASIC_FREQ_CHECK);
    // temperature_varInTap : show the current temperature.
    public  TapNLinkVarFloat                temperature_varInTap                   = new TapNLinkVarFloat((short) TEMPERATURE_ID, "temperature", BASIC_FREQ_CHECK);
    // back_varInTap : contains the backup to restore overall's and day's average.
    public  TapNLinkVarFloatArray           backup_varInTap                         = new TapNLinkVarFloatArray((short) BACKUP_ID, "backup", BASIC_FREQ_CHECK, 5);
    // lastHours_varInTap : contains the average array from 24 last hours.
    public  TapNLinkVarFloatArray           lastHours_varInTap                      = new TapNLinkVarFloatArray((short) LAST_HOURS_ID, "lastHours", BASIC_FREQ_CHECK, 24);
    // lastDays_varInTap : contains the average array from 30 last days.
    public  TapNLinkVarFloatArray           lastDays_varInTap                       = new TapNLinkVarFloatArray((short) LAST_DAYS_ID, "lastDays", BASIC_FREQ_CHECK, 7);

    /**
     * Class constructor with instantiation of the MQTT topic
     * Then send mqtt message for init class done.
     */
    TapCO2() {
        byte[] topicPrefixPayload = new byte[256];
        // Resource id 58 get the resource "MQTT Relay topic".
        system.engineCommand(TapNLinkSys.GET, RESOURCE_ID, 0, 58, topicPrefixPayload ); 
        this.topic = new String(topicPrefixPayload) + "/result";
        disableCRC();
        defineBinaryGasToCo2InAir();
        disableAutomaticSelfCalibration();
        //enableAutomaticSelfCalibration();
        forcedRecalibration();
    }


    /**
     * onCheck method is called for each TapNLinkVar declared in the Java class. This frequency is defined in the TapNLinkVar constructors
     */
    public void onCheck(final int id) {

        if(id == CALIBRATION_MODE_ID) {
            switch (calibrationMode_varInTap.getValue()) {
                case 0x02:
                    // DEFINE SELF CALIBRATION
                    enableAutomaticSelfCalibration();
                    calibrationMode_varInTap.setValue(0x00);
                    break;
                case 0x03:
                    // DEFINE FORCED CALIBRATION
                    disableAutomaticSelfCalibration();
                    forcedRecalibration();
                    calibrationMode_varInTap.setValue(0x01);
                    break;
                default:
                    // NOTHING CHANGED
                    break;
            }
        }

        /**
         * Updates the backup to restore average and count of loop.
         * every LONG_FREQ_CHECK.
         */
        if(id == CURRENT_DAY_AVERAGE_ID && initDone) {
            float[] backup = new float[5];
            backup[0] = backupTime;
            backup[1] = overallAverage_varInTap.getValue();
            backup[2] = loopCount[0];
            backup[3] = currentDayAverage_varInTap.getValue();
            backup[4] = loopCount[2];
            backup_varInTap.setValue(backup);
        }

        if(id == INSTANT_VALUE_ID) {
            currentTime();

            if(!initDone) {
                init();
            }
        
            float value = measureGasConcentration();
            float valueAndOlderToCal = (instantValue_varInTap.getValue() * valueCalibration_varInTap.getValue()) + value;
            float valAfterCal = valueAndOlderToCal / (valueCalibration_varInTap.getValue() + 1);
            instantValue_varInTap.setValue(valAfterCal);

            /**
             * If the value has higher than warning limit
             * An message has been send to AWS.
             */
            if(valAfterCal > warningLimit_varInTap.getValue()) {
             system.sendMQTTMessage("$aws/things/TapCO2/shadow/name/inAir/update",
             "{\"state\": {\"reported\": {\"ID\": \"TAPCO2\", \"currentValue\": " + valAfterCal + "}}}", 0);
            }

            if(currentHour != oldHour) {
                lastHours[oldHour] = currentHourAverage_varInTap.getValue();
                lastHours_varInTap.setValue(lastHours);

                if(currentDay != oldDay) {
                    lastDays[oldDay - 1] = currentDayAverage_varInTap.getValue();
                    lastDays_varInTap.setValue(lastDays);
                    loopCount[2] = 0;
                }
                loopCount[1] = 0;
            }
            refreshAverage(valAfterCal);
            refreshOldDate();
        }
    }

    public void keepAlive() {
        byte[] refreshKeepAlive = new byte[4];
        // Resource id 58 get the resource "MQTT Relay topic".
        system.engineCommand(TapNLinkSys.GET, RESOURCE_ID, 0, 4, refreshKeepAlive); 
        system.sendMQTTMessage(topic,"keep Alive refreshed !", 0); 
    }

    /**
     * Initialize the current time and the current consumption.
     */
    public void init() {
        if((int) backup_varInTap.getValue()[0] == backupTime) {
            currentDayAverage_varInTap.setValue(backup_varInTap.getValue()[3]);
            loopCount[2] = backup_varInTap.getValue()[4];
        }
        overallAverage_varInTap.setValue(backup_varInTap.getValue()[1]);
        lastHours = lastHours_varInTap.getValue();
        lastDays = lastDays_varInTap.getValue();
        loopCount[0] = backup_varInTap.getValue()[2];
        oldDay = currentDay;
        oldHour = currentHour;
        initDone = true;
    }

    /**
     * This method refresh 3 average data
     * Each average has calculated with her loop count.
     */
    public void refreshAverage(float value) {
        overallAverage_varInTap.setValue(
                (value + (overallAverage_varInTap.getValue() * loopCount[0]))
                / (loopCount[0] + 1));
        loopCount[0]++;

        currentHourAverage_varInTap.setValue(
                (value + (currentHourAverage_varInTap.getValue() * loopCount[1]))
                 / (loopCount[1] + 1));
        loopCount[1]++;

        currentDayAverage_varInTap.setValue(((currentDayAverage_varInTap.getValue() * loopCount[2]) + value) 
                / (loopCount[2] + 1));
        loopCount[2]++;
    }

    public void enterSleepMode() {
        byte [] buf = new byte[2];
        buf[0] =(byte) 0x36;
        buf[1] =(byte) 0x77;
        checkI2CResult(instanceI2C.writeBytes(DEFAULT_I2C_ADDRESS, buf, 2), 2, "write sleep mode");
    }

    /**
    * Save the last day and hour called onCheck
    */
    public void refreshOldDate() {
        oldDay = currentDay;
        oldHour = currentHour;
    }

    /**
     * This method turn OFF the CRC to communicate with sensor.
     */
    public void disableCRC() {
        byte [] buf = new byte[2];
        buf[0] =(byte) 0x37;
        buf[1] =(byte) 0x68;
        checkI2CResult(instanceI2C.writeBytes(DEFAULT_I2C_ADDRESS, buf, 2), 2, "write disable crc");
    }

    public void enableAutomaticSelfCalibration() {
        byte [] buf = new byte[2];
        buf[0] = (byte) 0x3F;
        buf[1] = (byte) 0xEF;
        checkI2CResult(instanceI2C.writeBytes(DEFAULT_I2C_ADDRESS, buf, 2), 2, "write enable AutomaticSelfCalibration");
    }

    public void disableAutomaticSelfCalibration() {
        byte [] buf = new byte [2];
        buf[0] = 0x3F;
        buf[1] = 0x6E;
        checkI2CResult(instanceI2C.writeBytes(DEFAULT_I2C_ADDRESS, buf, 2), 2, "write disable AutomaticSelfCalibration");
    }
    /**
     * This method define the Gas CO2 in air on sensor.
     * Change the buf[2] and and buf[3] to find another Gas.
     * 0x0000 = Set binary gas to CO2 in N2. Range: 0 to 100 vol%.
     * 0x0001 = Set binary gas to CO2 in air. Range: 0 to 100 vol%.
     * 0x0002 = Set binary gas to CO2 in N2. Range: 0 to 25 vol%.
     * 0x0003 = Set binary gas to CO2 in air. Range: 0 to 25 vol%.
     */
    public void defineBinaryGasToCo2InAir() {
        byte [] buf = new byte[4];
        buf[0] =(byte) 0x36;
        buf[1] =(byte) 0x15;
        buf[2] =(byte) 0x00;
        buf[3] =(byte) 0x01;
        /*
        byte [] dataToCrc = new byte[2];
        dataToCrc[0] = buf[2];
        dataToCrc[1] = buf[3];
        buf[4] = calcCrc(dataToCrc);
        */
        checkI2CResult(instanceI2C.writeBytes(DEFAULT_I2C_ADDRESS, buf, 4), 4, "defineBinaryGasToCo2InAir");
    }

    /**
     * This method return the current measure of the Gas concentration in air.
     */
    public float measureGasConcentration() {
        float valCO2;
        byte [] buf = new byte[2];
        buf[0] =(byte) 0x36;
        buf[1] =(byte) 0x39;
        checkI2CResult(instanceI2C.writeBytes(DEFAULT_I2C_ADDRESS, buf, 2), 2, "write measureGasConcentration");
        system.pause(250);
        byte [] reader = new byte[6];
        checkI2CResult(instanceI2C.readBytes(DEFAULT_I2C_ADDRESS, reader, 6), 6, "read measureGasConcentration");
        valCO2 = (float)(reader[0] *256 + reader[1]);
        valCO2 = valCO2 - 16384;
        valCO2 = valCO2 / 32768;
        valCO2 = valCO2 * 100;
        temperature_varInTap.setValue(((reader[2] *256 + reader[3]) / 200) - 7);
        if(valCO2 < 0.03f) {
           return 0.03f;
        }
        return valCO2;
    }


    /*
    public byte calcCrc(byte[] data) {
        byte crc = 0xFF;
        for(int i = 0; i < 2; i++)
            {
            crc ^= data[i];
            for(byte bit = 8; bit > 0; --bit)
                  {
                   if(crc == 0x80)
                           {
                            crc = (crc << (byte) 1) ^ 0x31;
                           }
                     else
                           {
                           crc = (crc <<(byte) 1);
                           }
                     }
              }
        return crc;
    }
    */

    public void forcedRecalibration() {
        byte [] buf = new byte[4];
        buf[0] =(byte) 0x36;
        buf[1] =(byte) 0x61;
        buf[2] =(byte) 0x40;
        buf[3] =(byte) 0x0D;
        checkI2CResult(instanceI2C.writeBytes(DEFAULT_I2C_ADDRESS, buf, 4), 4, "write forced calibration");
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
            String day = "" + date[24] + date[25] + date[26] + date[27];
            String month = "" + date[16] + date[17] + date[18] + date[19];
            currentHour = (int) Integer.valueOf(hour);
            currentDay = (int) Integer.valueOf(day);
            int currentCalendarToBackup = Integer.valueOf("" + date[12] + date[13] + date[14] + date[15]);
            int currentYearsToBackup = Integer.valueOf("" + date[20] + date[21] + date[22] + date[23]) ;
            backupTime = (((int) Integer.valueOf(month) + 1) * 31) + currentCalendarToBackup + (currentYearsToBackup * 365);
        } catch (TapNLinkException e) {
            system.sendMQTTMessage(topic,"Error on method currentTime(), error code : " + e.getCode(), 0);
        }
    }

    /**
     * If the I2C message has not 0
     * Then an message with error has send on MQTT.
     */
    public void checkI2CResult(int response, int bufferSize, String methods) {
        if(response != bufferSize) {
            system.sendMQTTMessage(topic,methods + " failed, Error I2C response results should be "+ bufferSize +" but find : " + response, 0);
        }
    }
    
    /**
     * This method is called in case of major error in the code, then the onCheck method will no longer be called
     */
    public void onException(int errorcode, int par1, int par2) {
        system.sendMQTTMessage(topic, String.format("Error code : " + errorcode, errorcode, par1, par2), 0);
    }
}
