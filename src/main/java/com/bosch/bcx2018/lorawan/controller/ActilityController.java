package com.bosch.bcx2018.lorawan.controller;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.MessageFormat;

@RestController
@RequestMapping(value = "/actility", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ActilityController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActilityController.class);

    @Value("${lora-reverse-proxy.tenant}")
    private String tenant;

    @Value("${lora-reverse-proxy.apikey}")
    private String apikey;

//    {
//        "DevEUI_uplink": {
//        "Time": "2018-01-31T17:18:25.365+01:00",
//                "DevEUI": "ACDE48234567ABCF",
//                "FPort": "1",
//                "FCntUp": "1782",
//                "ADRbit": "1",
//                "MType": "2",
//                "FCntDn": "1751",
//                "payload_hex": "00",
//                "mic_hex": "857c3ab8",
//                "Lrcid": "00000127",
//                "LrrRSSI": "-49.000000",
//                "LrrSNR": "8.000000",
//                "SpFact": "11",
//                "SubBand": "G1",
//                "Channel": "LC2",
//                "DevLrrCnt": "1",
//                "Lrrid": "08060669",
//                "Late": "0",
//                "LrrLAT": "48.823025",
//                "LrrLON": "9.296049",
//                "Lrrs": {
//            "Lrr": [{
//                "Lrrid": "08060669",
//                        "Chain": "0",
//                        "LrrRSSI": "-49.000000",
//                        "LrrSNR": "8.000000",
//                        "LrrESP": "-49.638920"
//            }]
//        },
//        "CustomerID": "100114152",
//                "CustomerData": {
//            "alr": {
//                "pro": "ADRF/ARF8123AA",
//                        "ver": "1"
//            }
//        },
//        "ModelCfg": "0",
//        "InstantPER": "0.000000",
//        "MeanPER": "0.000003",
//        "DevAddr": "055835AC",
//        "AckRequested": "0",
//        "rawMacCommands": ""
//        }
//    }


    @PostMapping
    public ResponseEntity<Void> post(@RequestBody() String body, @RequestParam("apikey") String incomingApiKey) {

        if (! apikey.equals(incomingApiKey)) {
            LOGGER.warn("Invalid apikey {}", incomingApiKey);
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        Object document = Configuration.defaultConfiguration().jsonProvider().parse(body);

        String deviceEUI = JsonPath.read(document, "$.DevEUI_uplink.DevEUI");
        String payloadHex = JsonPath.read(document, "$.DevEUI_uplink.payload_hex");

        LOGGER.info("Tenant={} DeviceEUI={} PayloadHex={}", tenant, deviceEUI, payloadHex);

        String url = MessageFormat.format("https://rest.bosch-iot-hub.com/telemetry/{0}/{1}",
                tenant, deviceEUI);
        LOGGER.info("IoT Hub request URL={}", url);

        //HttpPut request = new HttpPut(MessageFormat.format("https://requestb.in/vsv66kvs?tenant={0}&device={1}",
        HttpPut request = new HttpPut(url);

        String basicAuthentication = MessageFormat.format("{0}@{1}:{2}",
                deviceEUI, tenant, apikey
        );

        basicAuthentication = new String(Base64.encodeBase64(basicAuthentication.getBytes()));
        LOGGER.info("authentication={}", basicAuthentication);

        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuthentication);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            StringBuffer strbuf = new StringBuffer();
            strbuf.append("{");
            strbuf.append("  \"topic\": \"" + tenant + "/" + deviceEUI + "/things/twin/commands/modify\", ");
            strbuf.append("  \"headers\": { \"response-required\": false }, ");
            strbuf.append("  \"path\": \"/features/temperature/properties/status\", ");
            strbuf.append("  \"value\": {");
            strbuf.append("    \"max_range_value\": 50,");
            strbuf.append("    \"min_range_value\": -20,");
            strbuf.append("    \"sensor_units\": \"C\", ");
            strbuf.append("    \"sensor_value\": " + Integer.valueOf(payloadHex.substring(0, 2), 16) + ", ");
            strbuf.append("    \"min_measured_value\": -15, ");
            strbuf.append("    \"max_measured_value\": 45 ");
            strbuf.append("  }");
            strbuf.append("}");

            LOGGER.info(strbuf.toString());

            request.setEntity(new StringEntity(strbuf.toString()));

            long startTs = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(request);
            long endTs = System.currentTimeMillis();

            int statusCode = response.getStatusLine().getStatusCode();

            LOGGER.info("IoT Hub REST Connector returned HTTP status {}. ResponseTime={} ms", statusCode, (endTs - startTs));
        } catch (IOException e) {
            LOGGER.warn("Error calling IoT Hub REST Connector: " + e.getMessage(), e);
        }

        return ResponseEntity
                .ok()
                .build();
    }

}
