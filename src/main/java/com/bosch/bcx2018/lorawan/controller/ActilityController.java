/*
 * Bosch SI Example Code License Version 1.0, January 2016
 *
 * Copyright 2016 Bosch Software Innovations GmbH ("Bosch SI"). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * BOSCH SI PROVIDES THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE
 * QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL
 * NECESSARY SERVICING, REPAIR OR CORRECTION. THIS SHALL NOT APPLY TO MATERIAL DEFECTS AND DEFECTS OF TITLE WHICH BOSCH
 * SI HAS FRAUDULENTLY CONCEALED. APART FROM THE CASES STIPULATED ABOVE, BOSCH SI SHALL BE LIABLE WITHOUT LIMITATION FOR
 * INTENT OR GROSS NEGLIGENCE, FOR INJURIES TO LIFE, BODY OR HEALTH AND ACCORDING TO THE PROVISIONS OF THE GERMAN
 * PRODUCT LIABILITY ACT (PRODUKTHAFTUNGSGESETZ). THE SCOPE OF A GUARANTEE GRANTED BY BOSCH SI SHALL REMAIN UNAFFECTED
 * BY LIMITATIONS OF LIABILITY. IN ALL OTHER CASES, LIABILITY OF BOSCH SI IS EXCLUDED. THESE LIMITATIONS OF LIABILITY
 * ALSO APPLY IN REGARD TO THE FAULT OF VICARIOUS AGENTS OF BOSCH SI AND THE PERSONAL LIABILITY OF BOSCH SI'S EMPLOYEES,
 * REPRESENTATIVES AND ORGANS.
 */
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
        String port = JsonPath.read(document, "$.DevEUI_uplink.FPort");

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
            strbuf.append("  \"payloadHex\": \"" + payloadHex + "\", ");
            strbuf.append("  \"port\": " + Integer.valueOf(port) );
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
