# bcx18-lorawan-connectivity
Integration component that connect LoRaWAN Network Operators to the Bosch IoT Suite.

## REST endpoints
The component acts as a kind of "smart" reverse proxy that adapts incoming
HTTP requests and forwards them to the Bosch IoT Hub.

    https://lorawan-reverse-proxy.apps.de1.bosch-iot-cloud.com/actility?apikey=abc123

REST Endpoint for Actility ThingParkPartner. Receives JSON payload and converts
it to "Things" format. **Note:** Set request parameter `apikey` to the secret API key
communicated during the Hackathon.

    https://lorawan-reverse-proxy.apps.de1.bosch-iot-cloud.com/ttn?apikey=abc123
    
REST Endpoint for TTN console. Receives JSON payload and converts
it to "Things" format. **Note:** Set request parameter `apikey` to the secret API key
communicated during the Hackathon.

## Limitations
Currently only takes 1 byte as payload and interprets it as a temperature value.
  Example (Sensor value is 25°C):

    {
	    "topic": "BCX18/07191920146/things/twin/commands/modify",
	    "headers": {
		    "response-required": false
	    },
	    "path": "/features/temperature/properties/status",
	    "value": {
		    "max_range_value": 50,
		    "min_range_value": -20,
		    "sensor_units": "C",
		    "sensor_value": 25,
		    "min_measured_value": -15,
		    "max_measured_value": 45
	    }
    }  
  

## Build and push to BIC
Build with

    $ mvn clean install
    
Login to BIC
    
    $ cf login -a api.sys.de1.bosch-iot-cloud.com
    
Push the app
    
    $ cf push

This will create a new app named `lorawan-reverse-proxy` which accepts incoming HTTP requests.    
## Configure

In the BIC console (Bosch IoT Cloud Apps Manager) go to the "Settings" tab of
the app `lorawan-reverse-proxy`. 
Select "REVEAL USER PROVIDED ENV VARS"

* Update environment variable `lora-reverse-proxy_apikey` to a secret key.
* Restart the app `lorawan-reverse-proxy` to make the app aware of the new value.
    
