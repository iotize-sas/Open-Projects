import com.iotize.jvm.*;
import com.iotize.jvm.hal.*;


public class TestAlexaTap {

    TestAlexaTap() {
        system.subscribeMQTT("alexa");
    }

    private final int                       READ_ID                         = 0x02;
    private final int                       COMMAND_COUNTER                 = 0x03;
    private final int                       BASIC_FREQ_CHECK                = 1000;
    private final int                       RESOURCE_ID                     = 1024;

    private boolean                         timerStarted                    = false;
    private TapNLinkSys                     system;
    private int                             timer, timeOnStart, timeToEnd;

    public  TapNLinkVarInt                  read_varInTap                   = new TapNLinkVarInt((short) READ_ID, VariableType.INT8, "read", BASIC_FREQ_CHECK);
    public  TapNLinkVarInt                  commandCounter_varInTap         = new TapNLinkVarInt((short) 0x03, VariableType.INT32, "commandCounter", BASIC_FREQ_CHECK);
    public  Pin                             pinRelayCMD                     = new Pin(Pin.TGT_CLK, Pin.PP_OUT, Pin.HIZ);


    /**
     * onCheck method is called for each TapNLinkVar declared in the Java class. This frequency is defined in the TapNLinkVar constructors
     */
    public void onCheck(final int id) {
        if(id == READ_ID) {
            if(timerStarted){
                if( (timeOnStart + timer) <= (system.getTime() / 1000) ) {
                    timerEnd();
                }
            }
        }
    }
    
    public void timerEnd() {
        pinRelayCMD.setValue(0x00); 
        timer = 0;
        timeOnStart = 0;
        timerStarted = false;
        response("The relay has been stopped");
    }

    /**
     * This method is called on each MQTT/UART or other event catched by the JVM.
     */
    public void onEvent(Object input) {
        if(input instanceof MqttInput) {
            MqttInput mqttMessage = (MqttInput) input;
            commandCounter_varInTap.setValue(commandCounter_varInTap.getValue() + 1);
            try {
                switch (mqttMessage.getMessage()) {
                    case "turn on":
                        if(!timerStarted) {
                            pinRelayCMD.setValue(0x01);
                            response("The relay is now activated");
                        } else {
                            response("The relay is already activated");
                        }
                        break;
                    case "turn off": 
                        if(pinRelayCMD.getValue() == 0x00) {
                            response("The relay is already disabled");
                        } else {
                            timerEnd();
                        }
                        break;
                    case "ask timer":
                        if(!timerStarted) {
                            response("No timer is running at this time");
                        } else {
                            timeToEnd = (timer - ((system.getTime() / 1000) - timeOnStart));
                            if( timeToEnd <= 60 ) {
                                response("The remaining time is " + timeToEnd + " seconds");
                            } else if( timeToEnd > 60 && timeToEnd <= (60 * 60) ) {
                                response("The remaining time is " + ((float) timeToEnd / 60) + " minutes");
                            } else if( timeToEnd > (60 * 60) ) {
                                response("The remaining time is " + (((float) timeToEnd / 60) / 60) + " hours");
                            }
                        }
                        break;
                    case "command counter":
                        response("Alexa has executed " + commandCounter_varInTap.getValue() + " commands to tap");
                        break;
                    default:
                        if(mqttMessage.getMessage().charAt(0) == 'R') {
                            if(!timerStarted) {
                                response("No timer is running at this time");
                            } else {
                                timer += (int) Integer.valueOf(mqttMessage.getMessage().substring(1));
                                response("Timer time has been increased");
                            }
                        } else if(mqttMessage.getMessage().charAt(0) == 'T') {
                            timeOnStart = system.getTime() / 1000;
                            timerStarted = true;
                            timer = (int) Integer.valueOf(mqttMessage.getMessage().substring(1));
                            pinRelayCMD.setValue(0x01);
                        } else {
                            response("I didn’t understand what I had to do, message find : "  + mqttMessage.getMessage());
                        }
                        break;
                }
            } catch (TapNLinkException e) {
                response("An error occurred in the JVM  " + e.getMessage());
            }
        }
    }

    public void response(String mqttResponse) {
        system.sendMQTTMessage("response", mqttResponse, 0);
    }
    
    /**
     * This method is called in case of major error in the code, then the onCheck method will no longer be called
     */
    public void onException(int errorcode, int par1, int par2) {
        system.sendMQTTMessage("reponse", String.format("Error code : " + errorcode, errorcode, par1, par2), 0);
    }
}
