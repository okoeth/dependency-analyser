/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nttdata.depend.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.BindingProvider;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author oliverkoeth
 */
public class OAuthUtil {
    private final static Logger LOGGER = Logger.getLogger(OAuthUtil.class.getName());
    
    private static final String OAUTH_URL="https://login.salesforce.com/services/oauth2/token";
    private static final String URL_PATTERN="/services/";
    private static final String ACCESS_TOKEN="access_token";
    private static final String INSTANCE_URL="instance_url";
    
    private JSONObject session;
    private final HttpClient httpClient;
    
    public OAuthUtil (String code) {
        httpClient = HttpClientBuilder.create().build();
        createSession(code);
    }
    
    private void createSession(String code) {
        try {           
            String clientId = System.getenv("DEPENDENCY_ANALYSER_CLIENT_ID");
            if (clientId==null) {
                LOGGER.log(Level.SEVERE, "DEPENDENCY_ANALYSER_CLIENT_ID not found in env");
            } else {
                LOGGER.log(Level.INFO, "DEPENDENCY_ANALYSER_CLIENT_ID{0}", clientId);
            }
            String clientSecret = System.getenv("DEPENDENCY_ANALYSER_CLIENT_SECRET");
            if (clientSecret==null) {
                LOGGER.log(Level.SEVERE, "DEPENDENCY_ANALYSER_CLIENT_SECRET not found in env");
            } else {
                LOGGER.log(Level.INFO, "DEPENDENCY_ANALYSER_CLIENT_SECRET: {0}", clientSecret);
            }
            String requestURI = System.getenv("DEPENDENCY_ANALYSER_REQUEST_URI");
            if (requestURI==null) {
                LOGGER.log(Level.SEVERE, "DEPENDENCY_ANALYSER_REQUEST_URI not found in env");
            } else {
                LOGGER.log(Level.INFO, "\"DEPENDENCY_ANALYSER_REQUEST_URI: {0}", requestURI);
            }
            
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("code", code));
            nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
            nameValuePairs.add(new BasicNameValuePair("client_id", clientId));
            nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
            nameValuePairs.add(new BasicNameValuePair("redirect_uri", requestURI));

            HttpPost httppost = new HttpPost(OAUTH_URL);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            // Execute
            HttpResponse response = httpClient.execute(httppost);
            HttpEntity entity = response.getEntity();

            String json = EntityUtils.toString(entity);
            
            session = new JSONObject(new JSONTokener(json));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error in IO", e);
        } catch (ParseException e) {
            LOGGER.log(Level.SEVERE, "Error in JSON Parser", e);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Error in JSON", e);
        }
    }
    
    public String getOAuthToken()  {
        if (session==null || session.getString(ACCESS_TOKEN)==null) {
            return "EMPTY";
        } else {
            return session.getString(ACCESS_TOKEN);
        }
    }

    public String getInstanceURL()  {
        if (session==null || session.getString(INSTANCE_URL)==null) {
            return "EMPTY";
        } else {
            return session.getString(INSTANCE_URL);
        }
    }
    
    public void reconfigureBindingProvider(BindingProvider bindingProvider) {
        String defaultEndpointURL = 
                (String) bindingProvider.getRequestContext().get(
                        BindingProvider.ENDPOINT_ADDRESS_PROPERTY); 
        String actualEndpointURL = 
                getInstanceURL()+defaultEndpointURL.substring(defaultEndpointURL.indexOf(URL_PATTERN));
        bindingProvider.getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                actualEndpointURL);         
    }
 }
