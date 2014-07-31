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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.BindingProvider;
 
@Controller
@RequestMapping("/")
public class MainController {
    private final static Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @RequestMapping(value="/callback", method = RequestMethod.GET)
    public String callback(@RequestParam String code, ModelMap model) {
        LOGGER.info("Callback entered");
        try {
            OAuthUtil oaUtil = new OAuthUtil(code);
            String accessToken = oaUtil.getOAuthToken();
            
            SforceServiceService service = new SforceServiceService();
            SforceServicePortType port = service.getSforceService();
            SessionHeader sessionHeader = new SessionHeader();
            sessionHeader.setSessionId(accessToken);
            
            oaUtil.reconfigureBindingProvider((BindingProvider) port);
            
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
