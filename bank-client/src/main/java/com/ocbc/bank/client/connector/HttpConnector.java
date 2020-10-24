package com.ocbc.bank.client.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocbc.bank.dto.BankOperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class HttpConnector {

    // TODO - make post request with arguments rather that url based approach
    public BankOperationResponse getResponse(String resourcePath) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:8080/"+resourcePath);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            ObjectMapper objectMapper = new ObjectMapper();
            StringBuilder responseString = new StringBuilder();
            String nextLine = null;
            while ((nextLine = br.readLine()) != null) {
                responseString.append(nextLine);
            }

            return objectMapper.readValue(responseString.toString(), BankOperationResponse.class);
        } catch (MalformedURLException e) {
            log.error("Error while making a request to server. resource {}", resourcePath, e);
        } catch (IOException e) {
            log.error("Error while making a request to server. resource {}", resourcePath, e);
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }
}
