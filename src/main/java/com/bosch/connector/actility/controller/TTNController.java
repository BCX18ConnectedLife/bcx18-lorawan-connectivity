package com.bosch.connector.actility.controller;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
@RequestMapping(value = "/ttn", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class TTNController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TTNController.class);

    @Value("${lora-reverse-proxy.tenant}")
    private String tenant;

    @Value("${lora-reverse-proxy.apikey}")
    private String apikey;

    HttpHost proxy = new HttpHost("10.0.2.2", 3128, "http");

    int timeout = 10;
    RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout * 1000)
            .setConnectionRequestTimeout(timeout * 1000)
            .setSocketTimeout(timeout * 1000)
            //.setProxy(proxy)
            .build();

    CloseableHttpClient client =
            HttpClientBuilder.create().setDefaultRequestConfig(config).build();

// {
//	"app_id": "klenk_app",
//  "dev_id": "klenk_stm32",
//  "hardware_serial": "0087C850A54323C5",
//  "port": 1,
//  "counter": 1742,
//  "payload_raw": "AWcAygJoXwMCE/8EAAM=",
//  "metadata": {
//        "time": "2018-02-02T07:37:11.184826364Z",
//                "frequency": 868.3,
//                "modulation": "LORA",
//                "data_rate": "SF7BW125",
//                "coding_rate": "4/5",
//                "gateways": [{
//                    "gtw_id": "eui-b827ebfffe1e310d",
//                    "timestamp": 1409205075,
//                    "time": "2018-02-02T07:37:11.161772Z",
//                    "channel": 1,
//                    "rssi": -55,
//                    "snr": 10.2,
//                    "rf_chain": 1,
//                    "latitude": 48.95697,
//                    "longitude": 9.43752,
//                    "altitude": 300
//        }]
//    },
//    "downlink_url": "https://integrations.thethingsnetwork.org/ttn-eu/api/v2/down/klenk_app/klenk_process?key=ttn-account-v2.xmFS--XBHYPeFh_c1xXW8bzj8gC6mJmsW99AGqRgoAk"
//}
//

    @PostMapping
    public ResponseEntity<Void> post(@RequestBody() String body, @RequestParam("apikey") String incomingApiKey) {

        if (! apikey.equals(incomingApiKey)) {
            LOGGER.warn("Invalid apikey {}", incomingApiKey);
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        Object document = Configuration.defaultConfiguration().jsonProvider().parse(body);

        String deviceEUI = JsonPath.read(document, "$.hardware_serial");
        String payloadRaw = JsonPath.read(document, "$.payload_raw");

        byte[] payload = Base64.decodeBase64(payloadRaw);

        LOGGER.info("Tenant={} DeviceEUI={} PayloadRaw={} PayloadHex={}", tenant, deviceEUI, payloadRaw, Hex.encodeHexString(payload));

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

        try {
            StringBuffer strbuf = new StringBuffer();
            strbuf.append("{");
            strbuf.append("  \"topic\": \"" + tenant + "/" + deviceEUI + "/things/twin/commands/modify\", ");
            strbuf.append("  \"headers\": { \"response-required\": false }, ");
            strbuf.append("  \"path\": \"/features/temperature/properties/status\", ");
            strbuf.append("  \"value\": {");
            strbuf.append("    \"max_range_value\": 50,");
            strbuf.append("    \"min_range_value\": -20,");
            strbuf.append("    \"sensor_units\": \"C\", ");
            strbuf.append("    \"sensor_value\": " + payload[0] + ", ");
            strbuf.append("    \"min_measured_value\": -15, ");
            strbuf.append("    \"max_measured_value\": 45 ");
            strbuf.append("  }");
            strbuf.append("}");

            LOGGER.info(strbuf.toString());

            request.setEntity(new StringEntity(strbuf.toString()));

            long startTs = System.currentTimeMillis();
            HttpResponse response = client.execute(request);
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

    public static void main(String args[]) {
        byte[] payload = { 29 };
        System.out.println(Base64.encodeBase64String(payload));
    }

}
