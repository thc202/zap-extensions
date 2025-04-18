/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2016 The ZAP Development Team
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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.core.scanner.Plugin.AlertThreshold;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.Session;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.addon.commonlib.CommonAlertTag;
import org.zaproxy.addon.commonlib.PolicyTag;
import org.zaproxy.zap.extension.ruleconfig.RuleConfigParam;
import org.zaproxy.zap.model.Context;
import org.zaproxy.zap.utils.ZapXmlConfiguration;

/** Unit test for {@link CrossDomainScriptInclusionScanRule}. */
class CrossDomainScriptInclusionScanRuleUnitTest
        extends PassiveScannerTest<CrossDomainScriptInclusionScanRule> {

    @Mock(strictness = Strictness.LENIENT)
    Model model;

    @Mock(strictness = Strictness.LENIENT)
    Session session;

    @BeforeEach
    void setup() {
        when(session.getContextsForUrl(anyString())).thenReturn(Collections.emptyList());
        when(model.getSession()).thenReturn(session);
        rule.setModel(model);
    }

    @Override
    protected CrossDomainScriptInclusionScanRule createScanner() {
        CrossDomainScriptInclusionScanRule rule = new CrossDomainScriptInclusionScanRule();
        rule.setConfig(new ZapXmlConfiguration());
        return rule;
    }

    @Test
    void shouldReturnExpectedMappings() {
        // Given / When
        Map<String, String> tags = rule.getAlertTags();
        // Then
        assertThat(tags.size(), is(equalTo(4)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2021_A08_INTEGRITY_FAIL.getTag()),
                is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.PENTEST.getTag()), is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.DEV_STD.getTag()), is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.QA_STD.getTag()), is(equalTo(true)));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2021_A08_INTEGRITY_FAIL.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2021_A08_INTEGRITY_FAIL.getValue())));
    }

    @Test
    void noScripts() throws HttpMalformedHeaderException {

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");

        msg.setResponseBody("<html></html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void noCrossDomainScripts() throws HttpMalformedHeaderException {

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");

        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.example.com/script2\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void crossDomainScript() throws HttpMalformedHeaderException {

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.otherDomain.com/script2\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getParam(), equalTo("https://www.otherDomain.com/script2"));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<script src=\"https://www.otherDomain.com/script2\"/>"));
    }

    @Test
    void crossDomainScriptWithIntegrity() throws HttpMalformedHeaderException {

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.otherDomain.com/script2\" integrity=\"12345678\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void crossDomainScriptWithNullIntegrity() throws HttpMalformedHeaderException {

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.otherDomain.com/script2\" integrity=\"\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getParam(), equalTo("https://www.otherDomain.com/script2"));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<script src=\"https://www.otherDomain.com/script2\" integrity=\"\"/>"));
    }

    @Test
    void crossDomainScriptNotInContext() throws HttpMalformedHeaderException {
        // Given
        Context context = new Context(session, -1);
        context.addIncludeInContextRegex("https://www.example.com/.*");
        when(session.getContextsForUrl(anyString())).thenReturn(asList(context));

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");

        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.otherDomain.com/script2\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getParam(), equalTo("https://www.otherDomain.com/script2"));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<script src=\"https://www.otherDomain.com/script2\"/>"));
    }

    @Test
    void shouldIgnoreNonHtmlResponses() throws HttpMalformedHeaderException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/thing.js HTTP/1.1");

        msg.setResponseBody(
                "/* ------------------------------------------------------------\n"
                        + "Sample usage:\n"
                        + "<script type=\"text/javascript\" src=\"http://code.jquery.com/jquery-1.8.2.min.js\"></script>\n"
                        + "<script type=\"text/javascript\" src=\"http://code.jquery.com/ui/1.9.0/jquery-ui.js\"></script>\n"
                        + "...\n"
                        + "----------------------------------------------------------- */\n"
                        + "function myFunction(p1, p2) {\n"
                        + "    return p1 * p2;\n"
                        + "}\n");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: application/javascript\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void crossDomainScriptInContextHigh() throws HttpMalformedHeaderException {
        // Given
        Context context = new Context(session, -1);
        context.addIncludeInContextRegex("https://www.example.com/.*");
        context.addIncludeInContextRegex("https://www.otherDomain.com/.*");
        when(session.getContextsForUrl(anyString())).thenReturn(asList(context));

        rule.setAlertThreshold(AlertThreshold.HIGH);

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");

        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.otherDomain.com/script2\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void crossDomainScriptInContextMed() throws HttpMalformedHeaderException {
        // Given
        Context context = new Context(session, -1);
        context.addIncludeInContextRegex("https://www.example.com/.*");
        context.addIncludeInContextRegex("https://www.otherDomain.com/.*");
        when(session.getContextsForUrl(anyString())).thenReturn(asList(context));

        rule.setAlertThreshold(AlertThreshold.MEDIUM);

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");

        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.otherDomain.com/script2\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void crossDomainScriptInContextLow() throws HttpMalformedHeaderException {
        // Given
        rule.setAlertThreshold(AlertThreshold.LOW);

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");

        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.otherDomain.com/script2\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getParam(), equalTo("https://www.otherDomain.com/script2"));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<script src=\"https://www.otherDomain.com/script2\"/>"));
    }

    @Test
    void dontRaiseIssueWhenLinkIsTrustedDomain() throws HttpMalformedHeaderException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        rule.getConfig()
                .setProperty(RuleConfigParam.RULE_DOMAINS_TRUSTED, "https://www.example2.com/.*");

        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example2.com/script1\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void raiseIssueWhenLinkIsNotTrustedDomain() throws HttpMalformedHeaderException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET https://www.example.com/test/ HTTP/1.1");
        rule.getConfig()
                .setProperty(RuleConfigParam.RULE_DOMAINS_TRUSTED, "https://www.example2.com/.*");
        msg.setResponseBody(
                "<html>"
                        + "<head>"
                        + "<script src=\"https://www.example.com/script1\"/>"
                        + "<script src=\"https://www.otherDomain.com/script2\"/>"
                        + "</head>"
                        + "</html>");
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=ISO-8859-1\r\n"
                        + "Content-Length: "
                        + msg.getResponseBody().length()
                        + "\r\n");
        scanHttpResponseReceive(msg);
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("<script src=\"https://www.otherDomain.com/script2\"/>"));
    }

    @Test
    void shouldReturnExpectedExampleAlert() {
        // Given / When
        List<Alert> alerts = rule.getExampleAlerts();
        // Then
        assertThat(alerts.size(), is(equalTo(1)));
        Alert alert = alerts.get(0);
        Map<String, String> tags = alert.getTags();
        assertThat(tags.size(), is(equalTo(5)));
        assertThat(alert.getRisk(), is(equalTo(Alert.RISK_LOW)));
        assertThat(alert.getConfidence(), is(equalTo(Alert.CONFIDENCE_MEDIUM)));
    }

    @Test
    @Override
    public void shouldHaveValidReferences() {
        super.shouldHaveValidReferences();
    }
}
