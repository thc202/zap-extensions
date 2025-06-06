/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2013 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.pscanrules;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.network.HtmlParameter;
import org.parosproxy.paros.network.HtmlParameter.Type;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.addon.commonlib.CommonAlertTag;
import org.zaproxy.addon.commonlib.PolicyTag;
import org.zaproxy.zap.extension.pscan.PluginPassiveScanner;

/**
 * Port for the Watcher passive scanner (http://websecuritytool.codeplex.com/) rule {@code
 * CasabaSecurity.Web.Watcher.Checks.CheckPasvUserControlledHTMLAttributes}
 */
public class UserControlledHTMLAttributesScanRule extends PluginPassiveScanner
        implements CommonPassiveScanRuleInfo {

    /** Prefix for internationalized messages used by this rule */
    private static final String MESSAGE_PREFIX = "pscanrules.usercontrolledhtmlattributes.";

    private static final Map<String, String> ALERT_TAGS;

    static {
        Map<String, String> alertTags =
                new HashMap<>(
                        CommonAlertTag.toMap(
                                CommonAlertTag.OWASP_2021_A03_INJECTION,
                                CommonAlertTag.OWASP_2017_A01_INJECTION));
        alertTags.put(PolicyTag.PENTEST.getTag(), "");
        ALERT_TAGS = Collections.unmodifiableMap(alertTags);
    }

    @Override
    public String getName() {
        return Constant.messages.getString(MESSAGE_PREFIX + "name");
    }

    @Override
    public void scanHttpResponseReceive(HttpMessage msg, int id, Source source) {
        if (!getHelper().isPage200(msg) || !isResponseHTML(msg, source)) {
            return;
        }

        List<Element> htmlElements = source.getAllElements();
        if (htmlElements.isEmpty()) {
            return;
        }

        Set<HtmlParameter> params = new TreeSet<>(msg.getFormParams());
        params.addAll(msg.getUrlParams());
        if (params.isEmpty()) {
            return;
        }

        checkHtmlElements(msg, id, params, htmlElements);
    }

    /*
     Mainly looks to see if user-input controls certain attributes.  If the input is a URL, this attempts
     to see if the scheme or domain can be controlled.  If it's not, it attempts to see if the attribute
     data starts with the user-data.
    */
    private void checkHtmlElements(
            HttpMessage msg, int id, Set<HtmlParameter> params, List<Element> listOfHtmlElements) {
        for (Element element : listOfHtmlElements) {
            checkHtmlElement(msg, id, params, element);
        }
    }

    private void checkHtmlElement(
            HttpMessage msg, int id, Set<HtmlParameter> params, Element htmlElement) {
        Attributes attributes = htmlElement.getAttributes();
        if (attributes == null) {
            return;
        }

        for (Attribute attribute : attributes) {
            checkHtmlAttribute(msg, id, params, htmlElement, attribute);
        }
    }

    private void checkHtmlAttribute(
            HttpMessage msg,
            int id,
            Set<HtmlParameter> params,
            Element htmlElement,
            Attribute attribute) {
        String attrValue = attribute.getValue();
        if (attrValue == null) {
            return;
        }

        attrValue = attrValue.toLowerCase();

        // special handling of meta tag
        if (htmlElement.getName().equalsIgnoreCase(HTMLElementName.META)
                && attribute.getName().equalsIgnoreCase("content")) {
            if (attrValue.matches("^\\s*?[0-9]+?\\s*?;\\s*?url\\s*?=.*")) {
                attrValue = attrValue.substring(attrValue.indexOf("url=") + 4).trim();
            }
        }

        if (attrValue.length() == 0) {
            return;
        }

        String protocol = null;
        String domain = null;
        String token = null;

        // if contains protocol/domain name separator
        if (attrValue.indexOf("://") > 0) {
            URL url;
            try {
                url = new URL(attrValue);
            } catch (MalformedURLException e) {
                return;
            }
            // get protocol
            protocol = url.getProtocol();

            // get domain name
            domain = url.getAuthority();

            // token
            token = url.getQuery();
            // get up to first slash
            if (token != null && token.indexOf("/") > 0) {
                token = token.substring(0, token.indexOf("/"));
            }
        }

        // It's a local path, or it's not a resource.
        // Proceed later expecting the attribute value
        // might start with the user-input.

        for (HtmlParameter param : params) {
            String paramValue = param.getValue();
            if (paramValue == null) {
                return;
            }

            paramValue = paramValue.toLowerCase();

            // Special handling of meta tag.
            // If I were just looking to see if the meta tag 'contains' the user input,
            // we'd wind up with lots of false positives.
            // To avoid this, I  parse the meta tag values based on a set of delimiters,
            // such as ; =  and ,.  This is similar to what the Cookie poisoning
            // check does.
            if (htmlElement.getName().equalsIgnoreCase(HTMLElementName.META)
                    && attribute.getName().equalsIgnoreCase("content")) {
                // False Positive Reduction
                // We have a check for meta tag charset already, so get out of here.
                if (attrValue.contains("charset")) {
                    continue;
                }

                for (String s : attrValue.split("[;=,]")) {
                    if (s.equals(paramValue)) {
                        buildAlert(
                                        msg.getRequestHeader().getURI().toString(),
                                        htmlElement.getName(),
                                        attribute.getName(),
                                        param,
                                        attrValue)
                                .raise();
                        return; // Only need one alert
                    }
                }
            }

            // False Positive Reduction
            // I want the value length to be greater than 1 to avoid all the false positives
            // we're seeing when the input is limited to a single character.
            if (paramValue.length() > 1) {
                // See if the user-input can control the start of the attribute data.
                if (attrValue.startsWith(paramValue)
                        || paramValue.equalsIgnoreCase(protocol)
                        || paramValue.equalsIgnoreCase(domain)
                        || paramValue.equalsIgnoreCase(token)
                        || (attrValue.indexOf("://") > 0 && paramValue.indexOf(attrValue) == 0)) {
                    buildAlert(
                                    msg.getRequestHeader().getURI().toString(),
                                    htmlElement.getName(),
                                    attribute.getName(),
                                    param,
                                    attrValue)
                            .raise();
                }
            }

            // Make up for the false positive reduction by by being
            // sure to catch cases where a single character may control the attribute.
            // UPDATE: This case is too common and annoyingly rife with false positives.

            // if (val.Length == 1 && att.Equals(val) )
            // {
            //    AssembleAlert(element.tag, attribute, parm, val, value);
            // }

        }
    }

    // TODO: Fix up to support other variations of text/html.
    // FIX: This will match Atom and RSS feeds now, which set text/html but
    // use &lt;?xml&gt; in content

    // TODO: these methods have been extracted from CharsetMismatchScanner
    // I think we should create helper methods for them
    private static boolean isResponseHTML(HttpMessage message, Source source) {
        String contentType = message.getResponseHeader().getHeader(HttpHeader.CONTENT_TYPE);
        if (contentType == null) {
            return false;
        }

        return contentType.indexOf("text/html") != -1
                || contentType.indexOf("application/xhtml+xml") != -1
                || contentType.indexOf("application/xhtml") != -1;
    }

    private AlertBuilder buildAlert(
            String url,
            String htmlElement,
            String htmlAttribute,
            HtmlParameter param,
            String userControlledValue) {
        return newAlert()
                .setRisk(Alert.RISK_INFO)
                .setConfidence(Alert.CONFIDENCE_LOW)
                .setDescription(Constant.messages.getString(MESSAGE_PREFIX + "desc"))
                .setParam(param.getName())
                .setOtherInfo(
                        Constant.messages.getString(
                                MESSAGE_PREFIX + "extrainfo",
                                url,
                                htmlElement,
                                htmlAttribute,
                                param.getName(),
                                param.getValue(),
                                userControlledValue))
                .setSolution(Constant.messages.getString(MESSAGE_PREFIX + "soln"))
                .setReference(Constant.messages.getString(MESSAGE_PREFIX + "refs"))
                .setCweId(20) // CWE-20: Improper Input Validation
                .setWascId(20); // WASC-20: Improper Input Handling
    }

    @Override
    public int getPluginId() {
        return 10031;
    }

    @Override
    public Map<String, String> getAlertTags() {
        return ALERT_TAGS;
    }

    @Override
    public List<Alert> getExampleAlerts() {
        return List.of(
                buildAlert(
                                "http://example.com/i.php?name=fred",
                                "img",
                                "alt",
                                new HtmlParameter(Type.url, "name", "fred"),
                                MESSAGE_PREFIX)
                        .build());
    }
}
