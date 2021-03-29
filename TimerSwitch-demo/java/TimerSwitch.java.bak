import com.iotize.jvm.*;
import com.iotize.jvm.hal.*;


public class TimerSwitch {


    TimerSwitch() {
        byte[] topicPrefixPayload = new byte[256];
        // Resource id 58 get the resource "MQTT Relay topic".
        system.engineCommand(TapNLinkSys.GET, RESOURCE_ID, 0, 58, topicPrefixPayload ); 
        this.topic = new String(topicPrefixPayload) + "/result";
    }

    private final int   RESOURCE_ID             = 1024;
    private final int   BASIC_FREQ_CHECK        = 1000;
    private final int   QUICK_FREQ_CHECK        = 100; 
    private final int   RELAY_STATUS_ID         = 0x02;
    private final int   MANUAL_ACCES_ID         = 0x04;
    private final int   ACCES_TIME_ID           = 0x08;
    private final int   DAILY_SETUP_ID          = 0x05;
    private final int   ACTIVE_MODE_ID          = 0x07;
    private final int   AUTO_RUN_ID             = 0x03;
    private final int   TURN_ON                 = 0x01;
    private final int   TURN_OFF                = 0x00;
    private final int   MODE_AUTO               = 0x02;
    private final int   MODE_FORCE_ON           = 0x01;
    private final int   MODE_FORCE_OFF          = 0x00;

    // -------------------------------------------------
    //  TapNLinkVar, only InTap value (volatile or not)    
    // -------------------------------------------------
    public  TapNLinkVarInt                  automaticRun_varInTap           = new TapNLinkVarInt((short) AUTO_RUN_ID, VariableType.INT8, "automaticRun", BASIC_FREQ_CHECK);
    public  TapNLinkVarInt                  activeMode_varInTap             = new TapNLinkVarInt((short) ACTIVE_MODE_ID, VariableType.INT8, "activeMode", BASIC_FREQ_CHECK);
    public  TapNLinkVarByteArray            dailySetup_varInTap             = new TapNLinkVarByteArray((short) DAILY_SETUP_ID, "dailySetup", BASIC_FREQ_CHECK, 24);
    public  TapNLinkVarInt                  relayStatus_varInTap            = new TapNLinkVarInt((short) RELAY_STATUS_ID, VariableType.INT8, "relayStatus", BASIC_FREQ_CHECK);
    public  TapNLinkVarInt                  accessTime_varInTap             = new TapNLinkVarInt((short) ACCES_TIME_ID, VariableType.INT32, "accessTime", BASIC_FREQ_CHECK);
    public  TapNLinkVarInt                  manualAccess_varInTap           = new TapNLinkVarInt((short) MANUAL_ACCES_ID, VariableType.INT32, "manualAccess", QUICK_FREQ_CHECK);

    public  Pin                             pinPressButton                  = new Pin(Pin.TGT_DATA, 0x04, Pin.HIZ);
    public  Pin                             pinActivation                   = new Pin(Pin.TGT_CLK, Pin.PP_OUT, Pin.HIZ);

    // date : Define de absolute time from tap
    private byte[]                          date                            = new byte[36];
    // currentActivationHour : define the current hour recovery by absolute time.
    private int                             currentActivationHour           = 0;
    // oldActivationHour : Contain the 
    private int                             oldActivationHour               = 0;
    // startAccessTime : Define the system.getTime() when the jvm starting manual turn on.
    private int                             startAccessTime                 = 0;
    // defaultValuePinButton : Define the default value from start to the Pin on press button.
    private int                             defaultValuePinButton           = 0;
    // initPinValue : Define if the default pin value has init or not.
    private boolean                         initPinValue                    = false;
    // accesInProgress : Define if the manual access has in progress.
    private boolean                         accessInProgress                = false;

    private int                             buttonPress                     = 0;

    TapNLinkSys system;
    String topic;

    /**
    * onCheck method is called for each TapNLinkVar declared in the Java class. This frequency is defined in the TapNLinkVar constructors
    */
    public void onCheck(final int id) {

        if(id == MANUAL_ACCES_ID) {

            /**
            * If the default value from pin button has not define, then we execute this one.
            */
            if(!initPinValue) {
                defaultValuePinButton = pinPressButton.getValue();
                initPinValue = true;
            }

            /**
             * If default pin button value has different to pin.getValue() then the button has pressed
             * The pin activation has define to 0x01 to turn ON the device.
             * We define the startAccessTime to know how many time is remaining.
             */
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

        /**
        * If the manual access has in progress
        * Then the value display to time remaining has refresh
        * And if the remaining time has end, the device has turn OFF
        * And we define the pin status with the programm daily setup or mode define.
        */
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

       
        if(id == RELAY_STATUS_ID) {
            // Recovery the current time absolute from TapNLink.
            getCurrentTime();

            /**
             * If no manual access has in progress then we define the pin status with the programm daily set up or mode define
             */
            if(!accessInProgress) {
                definePinStatusByProgram();
            }    

            relayStatus_varInTap.setValue(pinActivation.getValue());   
        }        
    }

    /**
     * If the old activation hour has different has current and the automatic run has enable 
     * Then we force the AUTO mode to define the pin status with daily setup.
     * Else we define the status by the current active mode.
     */
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


    /**
    * Recover absolute time with a GET command to the lwM2M system.
    * Each value is coded on 4 consecutive bytes
    */ 
    public void getCurrentTime() {
        try {
            // Resource id 79 for the lwM2M provides "Local Time"
            system.engineCommand(TapNLinkSys.GET, RESOURCE_ID, 0x00, 79, date);
            String hour = "" + date[8] + date[9] + date[10] + date[11];
            currentActivationHour = (int) Integer.valueOf(hour);
            if(oldActivationHour == 0) {
                oldActivationHour = currentActivationHour;
            }
        } catch (TapNLinkException e) {
            system.sendMQTTMessage(topic,"Error on method currentTime(), error code : " + e.getCode(), 0);
        }
    }

     
    /**
     * This method is called in case of major error in the code, then the onCheck method will no longer be called
     */
    public void onException(int errorcode, int par1, int par2) {
        system.sendMQTTMessage(topic, String.format("Error code : ", errorcode, par1, par2), 0);
    }
}