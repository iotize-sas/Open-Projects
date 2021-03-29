import com.iotize.jvm.*;
import com.iotize.jvm.hal.*;


public class DigiTapLock {


    DigiTapLock() {
        byte[] topicPrefixPayload = new byte[256];
        // Resource id 58 get the resource "MQTT Relay topic".
        system.engineCommand(TapNLinkSys.GET, INTERFACE_RESOURCE_ID, 0, 58, topicPrefixPayload ); 
        this.topic = new String(topicPrefixPayload) + "/result";
    }

    private final int                       INTERFACE_RESOURCE_ID               = 1024;
    private final int                       BASIC_FREQ_CHECK                    = 1000;
    private final int                       DIGITAL_CODE_CHECK                  = 500;
    private final int                       QUICK_FREQ_CHECK                    = 100; 
    private final int                       ACCESS_OPEN                         = 0x00;
    private final int                       ACCESS_CLOSE                        = 0x01;
    private final int                       RECEPTION_ACCESS_MESSAGE            = 0x00;
    private final int                       REFUSE_ACCESS_MESSAGE               = 0x01;
    private final int                       ACCEPT_ACCESS_MESSAGE               = 0x02;
    private final int                       UNAUTHORIZED_MESSAGE                = 0x03;
    private final int                       DIGITAL_CODE_ID                     = 0x01;
    private final int                       ACCESS_CODE_ID                      = 0x04;
    private final int                       ACCESS_REMAINING_ID                 = 0x05;
    private final int                       DAILY_SETUP_ID                      = 0x06;
    private final int                       ACCESS_MESSAGE_ID                   = 0x08;
    private final int                       RESET_VALUE                         = 0x00;
    private final int                       REFUSE_MESSAGE_REMAINING            = 0x08;
    private final int                       BUTTON_PRESS_SCALE                  = 0x03;
    private final int                       DEFAULT_DIGITAL_CODE                = 0x00;

    // -------------------------------------------------
    //  TapNLinkVar, only InTap value (volatile or not)    
    // -------------------------------------------------

    // accessRemaining_varInTap : Define the access time remaining when access has authorized
    public  TapNLinkVarInt                  accessRemaining_varInTap            = new TapNLinkVarInt((short) ACCESS_REMAINING_ID, VariableType.INT32, "accessRemaining", DIGITAL_CODE_CHECK);
    // digitalCode_varInTap : This var contain the current digital code input
    public  TapNLinkVarInt                  digitalCode_varInTap                = new TapNLinkVarInt((short) DIGITAL_CODE_ID, VariableType.INT32, "digitalCode", DIGITAL_CODE_CHECK);
    // accessMessage_varInTap : Return the access message (Waiting, Refuse or Accept)
    public  TapNLinkVarInt                  accessMessage_varInTap              = new TapNLinkVarInt((short) 0x08, VariableType.INT8, "errorMessage", BASIC_FREQ_CHECK);
    // accessCode_varInTap : This tab contains the access code array for each resident
    public  TapNLinkVarIntArray             accessCode_varInTap                 = new TapNLinkVarIntArray((short) ACCESS_CODE_ID, VariableType.INT32, "accessCode", BASIC_FREQ_CHECK, 21);
    // dailySetup_varInTap : Define for each hour if the button can give access to device or not.
    public  TapNLinkVarByteArray            dailySetup_varInTap                 = new TapNLinkVarByteArray((short) DAILY_SETUP_ID, "dailySetup", QUICK_FREQ_CHECK, 24);


    public  Pin                             pinPressButton                      = new Pin(Pin.TGT_DATA, 0x04, Pin.HIZ);
    public  Pin                             pinActivation                       = new Pin(Pin.TGT_CLK, Pin.PP_OUT, Pin.HIZ);

    private byte[]                          date                                = new byte[36];

    // accessInProgress : Define if the current status of the access has open or not
    private boolean                         accessInProgress                    = false;
    // initDone : Define if the first init has done
    private boolean                         initDone                            = false;

    // currentAccessRemaining : This one count the time remaining after access authorized
    private int                             currentAccessRemaining              = 0;
    // TheButtonIsPressed : if this value equals 3, then the button has pressed
    private int                             TheButtonIsPressed                  = 0;
    // defaultValuePinButton. When we start the tap, we get the default value on button to check if this one change.
    private int                             defaultValuePinButton               = 0;
    // currentRefuseRemaining : This one count the time remaining after access refuse 
    private int                             currentRefuseRemaining              = 0;
    // currentTimeHour : Define the current hour to define if we can access with button pressed
    private int                             currentTimeHour                     = 0;

    TapNLinkSys system;
    String topic;

    /**
    * onCheck method is called for each TapNLinkVar declared in the Java class. This frequency is defined in the TapNLinkVar constructors
    */
    public void onCheck(final int id) {
        if(!initDone) {
            defaultValuePinButton = pinPressButton.getValue();
            pinActivation.setValue(ACCESS_CLOSE);
            initDone = true;
        }

        switch (id) {
            /**
             * On id DIGITAL_CODE TapNLinkVar
             * First time we check if the access has in progress or not and if the digital code has different to 0
             * Then we compare the digital code with access code list.
             * A message access has display with this result
             * The digital code has refresh
             */
            case DIGITAL_CODE_ID:
                if(!accessInProgress && (digitalCode_varInTap.getValue() > DEFAULT_DIGITAL_CODE)) {
                    boolean accessResult = false;
                    for(int code: accessCode_varInTap.getValue()) {
                        if(code == Integer.valueOf(digitalCode_varInTap.getValue())) {
                            accessResult = true;
                            pinActivation.setValue(ACCESS_OPEN);
                        }
                    }
                    if(accessResult) {
                        accessInProgress = true; 
                        accessMessage_varInTap.setValue(ACCEPT_ACCESS_MESSAGE);
                    } else {
                        accessMessage_varInTap.setValue(REFUSE_ACCESS_MESSAGE);
                    }
                    digitalCode_varInTap.setValue(0x00);
                }
                break;
            /**
             * On id ACCESS_REMAINING TapNLinkVar
             * First time we check the current access status
             * Then we refresh the current remaining time with current status
             */
            case ACCESS_REMAINING_ID:
                if(accessInProgress) {
                    currentAccessRemaining++;
                    if(currentAccessRemaining >= (accessRemaining_varInTap.getValue() * 2)) {
                        accessInProgress = false;
                        accessMessage_varInTap.setValue(RECEPTION_ACCESS_MESSAGE);
                        pinActivation.setValue(ACCESS_CLOSE);
                        currentAccessRemaining = RESET_VALUE;
                    }
                } else {
                    if(accessMessage_varInTap.getValue() == REFUSE_ACCESS_MESSAGE
                     || accessMessage_varInTap.getValue() == UNAUTHORIZED_MESSAGE) {
                        currentRefuseRemaining++;
                        if(currentRefuseRemaining >= REFUSE_MESSAGE_REMAINING) {
                            currentRefuseRemaining = RESET_VALUE;
                            accessMessage_varInTap.setValue(RECEPTION_ACCESS_MESSAGE);
                        }
                    }
                }
                break;
            case DAILY_SETUP_ID:
                /**
                * If default pin button value has different to pin.getValue() then the button has pressed
                * The pin activation has define to 0x01 to turn ON the device.
                */
                if(pinPressButton.getValue() != defaultValuePinButton) {
                    TheButtonIsPressed++;
                } else {
                    TheButtonIsPressed = RESET_VALUE;
                }
                if(TheButtonIsPressed == BUTTON_PRESS_SCALE) {
                    TheButtonIsPressed = RESET_VALUE;
                    if(dailySetup_varInTap.getValue()[currentTimeHour] == 0x01) {
                        pinActivation.setValue(ACCESS_OPEN);
                        accessMessage_varInTap.setValue(ACCEPT_ACCESS_MESSAGE);
                        accessInProgress = true;
                    } else {
                        accessMessage_varInTap.setValue(UNAUTHORIZED_MESSAGE);
                    }    
                }
                break;
            case ACCESS_MESSAGE_ID:
                getCurrentTime();
                break;
        }
    }

  
    /**
    * Recover absolute time with a GET command to the lwM2M system.
    * Each value is coded on 4 consecutive bytes
    */ 
    public void getCurrentTime() {
        try {
            // Resource id 79 for the lwM2M provides "Local Time"
            system.engineCommand(TapNLinkSys.GET, INTERFACE_RESOURCE_ID, 0x00, 79, date);
            String hour = "" + date[8] + date[9] + date[10] + date[11];
            currentTimeHour = (int) Integer.valueOf(hour);
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