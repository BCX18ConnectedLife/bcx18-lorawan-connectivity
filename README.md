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

## About payload
Payload sent by LoRaWAN devices should be kept to a minimum in order to not violate
duty cycle restrictions. The Developer Console allows to map the payload back to the
status properties of the device. This avoids that devices need to send their
values in the "chatty" Ditto format.

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
* Make sure environment variable `lora-reverse-proxy_tenant` has value `DX_TENANT`. 
  This is temporarily required to make sure the proof-of-concept payload mapping works.
* Restart the app `lorawan-reverse-proxy` to make the app aware of the new value.
    
