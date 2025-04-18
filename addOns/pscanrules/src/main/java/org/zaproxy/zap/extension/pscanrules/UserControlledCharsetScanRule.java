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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
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
 * CasabaSecurity.Web.Watcher.Checks.CheckPasvUserControlledCharset}
 */
public class UserControlledCharsetScanRule extends PluginPassiveScanner
        implements CommonPassiveScanRuleInfo {

    /** Prefix for internationalized messages used by this rule */
    private static final String MESSAGE_PREFIX = "pscanrules.usercontrolledcharset.";

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
        if (!getHelper().isPage200(msg)) {
            return;
        }

        String responseBody = msg.getRequestBody().toString();
        if (responseBody == null) {
            return;
        }

        Set<HtmlParameter> params = new TreeSet<>(msg.getFormParams());
        params.addAll(msg.getUrlParams());
        if (params.isEmpty()) {
            return;
        }

        if (!isResponseHTML(msg, source) && !isResponseXML(source)) {
            return;
        }

        if (isResponseHTML(msg, source)) {
            checkMetaContentCharset(msg, id, source, params);
        } else if (isResponseXML(source)) {
            checkXmlEncodingCharset(msg, id, source, params);
        }

        checkContentTypeCharset(msg, id, params);
    }

    private void checkMetaContentCharset(
            HttpMessage msg, int id, Source source, Set<HtmlParameter> params) {
        List<Element> metaElements = source.getAllElements(HTMLElementName.META);
        if (metaElements == null || metaElements.isEmpty()) {
            return;
        }

        for (Element metaElement : metaElements) {
            String httpEquiv = metaElement.getAttributeValue("http-equiv");
            String bodyContentType = metaElement.getAttributeValue("content");

            // If META element defines HTTP-EQUIV and CONTENT attributes,
            // compare charset values
            if (httpEquiv == null
                    || bodyContentType == null
                    || !httpEquiv.equalsIgnoreCase("content-type")) {
                continue;
            }

            String bodyContentCharset = getBodyContentCharset(bodyContentType);
            if (bodyContentCharset == null) {
                continue;
            }
            for (HtmlParameter param : params) {
                if (bodyContentCharset.equalsIgnoreCase(param.getValue())) {
                    buildAlert("META", "Content-Type", param, bodyContentCharset).raise();
                }
            }
        }
    }

    // TODO: taken from CharsetMismatchScanner. Extract into helper method
    private static String getBodyContentCharset(String bodyContentType) {
        // preconditions
        assert bodyContentType != null;

        String charset = null;

        bodyContentType = bodyContentType.trim();

        int charsetIndex;
        if ((charsetIndex = bodyContentType.indexOf("charset=")) != -1) {
            // 8 is a length of "charset="
            charset = bodyContentType.substring(charsetIndex + 8);
        }

        return charset;
    }

    private void checkXmlEncodingCharset(
            HttpMessage msg, int id, Source source, Set<HtmlParameter> params) {
        List<StartTag> xmlDeclarationTags = source.getAllStartTags(StartTagType.XML_DECLARATION);
        if (xmlDeclarationTags.isEmpty()) {
            return;
        }

        StartTag xmlDeclarationTag = xmlDeclarationTags.get(0);
        String encoding = xmlDeclarationTag.getAttributeValue("encoding");

        if (encoding == null || encoding.equals("")) {
            return;
        }

        for (HtmlParameter param : params) {
            if (encoding.equalsIgnoreCase(param.getValue())) {
                buildAlert("\\?xml", "encoding", param, encoding).raise();
            }
        }
    }

    private void checkContentTypeCharset(HttpMessage msg, int id, Set<HtmlParameter> params) {
        String charset = msg.getResponseHeader().getCharset();
        if (charset == null || charset.equals("")) {
            return;
        }

        for (HtmlParameter param : params) {
            if (charset.equalsIgnoreCase(param.getValue())) {
                buildAlert("Content-Type HTTP header", "charset", param, charset).raise();
            }
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

    private static boolean isResponseXML(Source source) {
        return source.isXML();
    }

    private AlertBuilder buildAlert(String tag, String attr, HtmlParameter param, String charset) {
        return newAlert()
                .setRisk(Alert.RISK_INFO)
                .setConfidence(Alert.CONFIDENCE_LOW)
                .setDescription(Constant.messages.getString(MESSAGE_PREFIX + "desc"))
                .setParam(param.getName())
                .setOtherInfo(getExtraInfoMessage(tag, attr, param, charset))
                .setSolution(Constant.messages.getString(MESSAGE_PREFIX + "soln"))
                .setCweId(20) // CWE-20: Improper Input Validation
                .setWascId(20); // WASC-20: Improper Input Handling
    }

    @Override
    public int getPluginId() {
        return 10030;
    }

    @Override
    public Map<String, String> getAlertTags() {
        return ALERT_TAGS;
    }

    /*
     * Rule-associated messages
     */
    private static String getExtraInfoMessage(
            String tag, String attr, HtmlParameter param, String charset) {
        return Constant.messages.getString(
                MESSAGE_PREFIX + "extrainfo",
                tag,
                attr,
                param.getName(),
                param.getValue(),
                charset);
    }

    @Override
    public List<Alert> getExampleAlerts() {
        return List.of(
                buildAlert(
                                "Content-Type HTTP header",
                                "charset",
                                new HtmlParameter(Type.url, "cs", "utf-8"),
                                "utf-8")
                        .build());
    }
}
