/* *
 * This sample demonstrates handling intents from an Alexa skill using the Alexa Skills Kit SDK (v2).
 * Please visit https://alexa.design/cookbook for additional examples on implementing slots, dialog management,
 * session persistence, api calls, and more.
 * */
const Alexa = require('ask-sdk-core');
const mqtt = require('mqtt');

const LaunchRequestHandler = {
    canHandle(handlerInput) {
    console.log('Check hnadler',handlerInput );
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'LaunchRequest';
    },
    handle(handlerInput) {
        const speakOutput = 'Welcome to the IoTize tests';

        return handlerInput.responseBuilder
            .speak(speakOutput)
            .reprompt(speakOutput)
            .getResponse();
    }
};



const HelpIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.HelpIntent';
    },
    handle(handlerInput) {
        const speakOutput = 'You can say hello to me! How can I help?';

        return handlerInput.responseBuilder
            .speak(speakOutput)
            .reprompt(speakOutput)
            .getResponse();
    }
};

const CancelAndStopIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && (Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.CancelIntent'
                || Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.StopIntent');
    },
    handle(handlerInput) {
        const speakOutput = 'Goodbye!';

        return handlerInput.responseBuilder
            .speak(speakOutput)
            .getResponse();
    }
};

/* *
 * FallbackIntent triggers when a customer says something that doesn’t map to any intents in your skill
 * It must also be defined in the language model (if the locale supports it)
 * This handler can be safely added but will be ingnored in locales that do not support it yet 
 * */
const FallbackIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.FallbackIntent';
    },
    handle(handlerInput) {
        const speakOutput = 'Sorry, I don\'t know about that. Please try again.';

        return handlerInput.responseBuilder
            .speak(speakOutput)
            .reprompt(speakOutput)
            .getResponse();
    }
};

/* *
 * SessionEndedRequest notifies that a session was ended. This handler will be triggered when a currently open 
 * session is closed for one of the following reasons: 1) The user says "exit" or "quit". 2) The user does not 
 * respond or says something that does not match an intent defined in your voice model. 3) An error occurs 
 * */
const SessionEndedRequestHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'SessionEndedRequest';
    },
    handle(handlerInput) {
        console.log(`~~~~ Session ended: ${JSON.stringify(handlerInput.requestEnvelope)}`);
        // Any cleanup logic goes here.
        return handlerInput.responseBuilder.getResponse(); // notice we send an empty response
    }
};

/* *
 * The intent reflector is used for interaction model testing and debugging.
 * It will simply repeat the intent the user said. You can create custom handlers for your intents 
 * by defining them above, then also adding them to the request handler chain below 
 * */
const IntentReflectorHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest';
    },
    handle(handlerInput) {
        const intentName = Alexa.getIntentName(handlerInput.requestEnvelope);
        const speakOutput = `You just triggered ${intentName}`;

        return handlerInput.responseBuilder
            .speak(speakOutput)
            //.reprompt('add a reprompt if you want to keep the session open for the user to respond')
            .getResponse();
    }
};
/**
 * Generic error handling to capture any syntax or routing errors. If you receive an error
 * stating the request handler chain is not found, you have not implemented a handler for
 * the intent being invoked or included it in the skill builder below 
 * */
const ErrorHandler = {
    canHandle() {
        return true;
    },
    handle(handlerInput, error) {
        const speakOutput = 'Sorry, I had trouble doing what you asked. Please try again.';
        console.log(`~~~~ Error handled: ${JSON.stringify(error)}`);

        return handlerInput.responseBuilder
            .speak(speakOutput)
            .reprompt(speakOutput)
            .getResponse();
    }
};


// ------------------------------------------------------------------------------------------------------------------------------------
// ----------- INTENT EN --------------------------------------------------------------------------------------------------------------
// ------------------------------------------------------------------------------------------------------------------------------------


const StartRelayIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'StartRelayIntent';
    },
    handle(handlerInput) {
         return new Promise((resolve, reject) => {
            mqttConnect(function(client) {
             client.publish('alexa', 'turn on');
             
            client.subscribe('response', function () {
             mqttMessageTimerInfo(client, handlerInput)
                .then((result) => {
                    client.end()
                    resolve(result);
             })
                .catch(err => {
                    client.end()
                    reject(handlerInput.responseBuilder
                        .speak("Erreur, " + err)
                        .getResponse());
             });
            });
            });
        })
    }
};

const CommandCounterIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'CommandCounterIntent';
    },
    handle(handlerInput) {
        return new Promise((resolve, reject) => {
         mqttConnect(function(client) {
             client.publish('alexa', 'command counter');
             
            client.subscribe('response', function () {
             mqttMessageTimerInfo(client, handlerInput)
                .then((result) => {
                    client.end()
                    resolve(result);
             })
                .catch(err => {
                    client.end()
                    reject(handlerInput.responseBuilder
                        .speak("Erreur, " + err)
                        .getResponse());
             });
            });
        })
        })
    }
};

const StartRelayWithTimerIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'StartRelayWithTimerIntent';
    },
    handle(handlerInput) {
        let timer;
        let speakOutput;
        let timerDefine = 0;
        if(handlerInput.requestEnvelope.request.intent.slots.seconds.value) {
            timer = handlerInput.requestEnvelope.request.intent.slots.seconds.value;
            speakOutput = 'The relay will be activated during ' + timer + 'seconds';
            timerDefine = timer;
        } else if(handlerInput.requestEnvelope.request.intent.slots.minutes.value) {
            timer = handlerInput.requestEnvelope.request.intent.slots.minutes.value;
            speakOutput = 'The relay will be activated during ' + timer + 'minutes';
            timerDefine = timer * 60;
        } else if(handlerInput.requestEnvelope.request.intent.slots.hours.value) {
            timer = handlerInput.requestEnvelope.request.intent.slots.hours.value;
            speakOutput = 'The relay will be activated during ' + timer + 'hours';
            timerDefine = timer * 60 * 60;
        }
         mqttConnect(function(client) {
            client.publish('alexa','T'+ timerDefine.toString());
            client.end()
        })
        return handlerInput.responseBuilder
            .speak(speakOutput)
            //.reprompt('add a reprompt if you want to keep the session open for the user to respond')
            .getResponse();
    }
};


const AskTimerIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AskTimerIntent';
    },
    handle(handlerInput) {
        return new Promise((resolve, reject) => {
         mqttConnect(function(client) {
             client.publish('alexa', 'ask timer');
             
            client.subscribe('response', function () {
             mqttMessageTimerInfo(client, handlerInput)
                .then((result) => {
                    client.end()
                    resolve(result);
             })
                .catch(err => {
                    client.end()
                    reject(handlerInput.responseBuilder
                        .speak("Erreur, " + err)
                        .getResponse());
             });
            });
        })
        })
    }
};


const IncreaseTimerIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'IncreaseTimerIntent';
    },
    handle(handlerInput) {
        return new Promise((resolve, reject) => {
         mqttConnect(function(client) {
             
            let timer;
            let speakOutput;
            let timerDefine = 0;
            if(handlerInput.requestEnvelope.request.intent.slots.secondsTimer.value) {
                timer = handlerInput.requestEnvelope.request.intent.slots.secondsTimer.value;
                timerDefine = timer;
            } else if(handlerInput.requestEnvelope.request.intent.slots.minutesTimer.value) {
                timer = handlerInput.requestEnvelope.request.intent.slots.minutesTimer.value;
                timerDefine = timer * 60;
            } else if(handlerInput.requestEnvelope.request.intent.slots.hoursTimer.value) {
                timer = handlerInput.requestEnvelope.request.intent.slots.hoursTimer.value;
                timerDefine = timer * 60 * 60;
            }
                
            client.publish('alexa','R'+ timerDefine.toString());
             
            client.subscribe('response', function () {
             mqttMessageTimerInfo(client, handlerInput)
                .then((result) => {
                    client.end()
                    resolve(result);
             })
                .catch(err => {
                    client.end()
                    reject(handlerInput.responseBuilder
                        .speak("Erreur, " + err)
                        .getResponse());
             });
            });
        })
        })
    }
};

const DisableRelayIntentHandler = {
    canHandle(handlerInput) {
        return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
            && Alexa.getIntentName(handlerInput.requestEnvelope) === 'DisableRelayIntent';
    },
    handle(handlerInput) {
         return new Promise((resolve, reject) => {
            mqttConnect(function(client) {
             client.publish('alexa', 'turn off');
             
            client.subscribe('response', function () {
             mqttMessageTimerInfo(client, handlerInput)
                .then((result) => {
                    client.end()
                    resolve(result);
             })
                .catch(err => {
                    client.end()
                    reject(handlerInput.responseBuilder
                        .speak("Erreur, " + err)
                        .getResponse());
             });
            });
        })
        })
    }
};






/**
 * This handler acts as the entry point for your skill, routing all request and response
 * payloads to the handlers above. Make sure any new handlers or interceptors you've
 * defined are included below. The order matters - they're processed top to bottom 
 * */
exports.handler = Alexa.SkillBuilders.custom()
    .addRequestHandlers(
        LaunchRequestHandler,
        HelpIntentHandler,
        CommandCounterIntentHandler,
        CancelAndStopIntentHandler,
        StartRelayWithTimerIntentHandler,
        AskTimerIntentHandler,
        FallbackIntentHandler,
        SessionEndedRequestHandler,
        StartRelayIntentHandler,
        IncreaseTimerIntentHandler,
        DisableRelayIntentHandler,
        IntentReflectorHandler)
    .addErrorHandlers(
        ErrorHandler)
    .withCustomUserAgent('sample/hello-world/v1.2')
    .lambda();
    
    
    // ---------------------------------------------------------------------------------------------------------------------------------
    // ----------- METHOD --------------------------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------------------------------------------
    
    
    
    function mqttConnect(onConnected, onError) {
    const client  = mqtt.connect('mqtt://broker.hivemq.com');
    client.on('connect', function () {
        console.log('MQTT client connected');
        onConnected(client);
    });
    
    client.on('error', function (err) {
        console.log('MQTT client err', err);
        // onError(err);
        client.close();
        client.end(true);
    });
    
    return client;
}



function mqttConnectPromise() {
    return new Promise((resolve, reject) => {
        const client  = mqtt.connect('mqtt://broker.hivemq.com');
        client.on('connect', function () {
            console.log('MQTT client connected');
            resolve(client);
        });
        
        client.on('error', function (err) {
            console.log('MQTT client err', err);
            reject(err);
            client.close();
            client.end(true);
        });
        
    })
}

function mqttSubscribe(client) {
    return new Promise((resolve, reject) => {
        client.subscribe('response', function () {
            resolve();
        });
        
        client.on('error', function (err) {
            reject(err);
            client.close();
            client.end(true);
        });
    })
}

function mqttMessageTimerInfo(client, handlerInput) {
    return new Promise((resolve, reject) => {
        client.on('message', function (topic, message) {
            resolve( handlerInput.responseBuilder
            .speak(message.toString())
            //.reprompt('add a reprompt if you want to keep the session open for the user to respond')
            .getResponse());
        });
        client.on('error', function (err) {
            reject(err);
            client.close();
            client.end(true);
        });
        
    })
}
    
    
    
    
    
    