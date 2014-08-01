package com.nttdata.depend.controller;

import com.nttdata.depend.util.OAuthUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.sforce.soap.tooling.ApexClass;
import com.sforce.soap.tooling.SessionHeader;
import com.sforce.soap.tooling.SforceServicePortType;
import com.sforce.soap.tooling.SforceServiceService;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Autowired;
 
@Controller
@RequestMapping("/")
public class MainController {    
    private final static Logger LOGGER = Logger.getLogger(MainController.class.getName());

    private final static String TOOLING_WSDL = "/WEB-INF/wsdl/toolingapi.wsdl";
    @Autowired
    ServletContext context;
    
    @RequestMapping(value="/test", method = RequestMethod.GET)
    public String test(ModelMap model) {
        try {
            LOGGER.info("Test entered");

            String wsdlFileName = "/WEB-INF/wsdl/toolingapi.wsdl";
            URL url = context.getResource(wsdlFileName);

            LOGGER.info("WSDL URL:"+url.toString());

            model.addAttribute("message", "Hello world"+url.toString());
        } catch (Exception e) {
            model.addAttribute("message", "Hello world"+e);
        }
        return "class_list";
    }
    
    @RequestMapping(value="/callback", method = RequestMethod.GET)
    public String callback(@RequestParam String code, ModelMap model) {
        LOGGER.info("Callback entered");
        try {            
            URL url = context.getResource(TOOLING_WSDL);
            LOGGER.log(Level.INFO, "WSDL URL:{0}", url.toString());
            
            SforceServiceService service = new SforceServiceService(
                    url, new QName("urn:tooling.soap.sforce.com", "SforceServiceService"));
            SforceServicePortType port = service.getSforceService();
            
            OAuthUtil oaUtil = new OAuthUtil(code);
            oaUtil.reconfigureBindingProvider((BindingProvider) port);
            String accessToken = oaUtil.getOAuthToken();
            SessionHeader sessionHeader = new SessionHeader();
            sessionHeader.setSessionId(accessToken); 
            
            ApexClass[] apexClasses =
                port.query("select Id, Name, Body from ApexClass where NamespacePrefix = null", sessionHeader)
                    .getRecords().toArray(new ApexClass[0]);
            
            StringBuilder apexClassList = new StringBuilder();
            
            apexClassList.append("<ul>");
            for (ApexClass apexClasse : apexClasses) {
                apexClassList.append("<li>").append(apexClasse.getName()).append("</li>");
            }
            apexClassList.append("</ul>");

            model.addAttribute("message", apexClassList.toString());
        } catch (Exception e) {
            model.addAttribute("message", "Error" + e);
            LOGGER.log(Level.SEVERE, "Error", e);
        }

        return "class_list";
    }
}
