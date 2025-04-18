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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.addon.commonlib.CommonAlertTag;
import org.zaproxy.addon.commonlib.PolicyTag;

class InsecureJsfViewStatePassiveScanRuleUnitTest
        extends PassiveScannerTest<InsecureJsfViewStatePassiveScanRule> {

    private static final String BASE_RESOURCE_KEY = "pscanrules.insecurejsfviewstate.";
    private static final String INSECURE_JSF = BASE_RESOURCE_KEY + "name";

    @Override
    protected InsecureJsfViewStatePassiveScanRule createScanner() {
        return new InsecureJsfViewStatePassiveScanRule();
    }

    @Test
    void shouldReturnExpectedMappings() {
        // Given / When
        int cwe = rule.getCweId();
        int wasc = rule.getWascId();
        Map<String, String> tags = rule.getAlertTags();
        // Then
        assertThat(cwe, is(equalTo(642)));
        assertThat(wasc, is(equalTo(14)));
        assertThat(tags.size(), is(equalTo(4)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2021_A04_INSECURE_DESIGN.getTag()),
                is(equalTo(true)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2017_A06_SEC_MISCONFIG.getTag()),
                is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.PENTEST.getTag()), is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.QA_STD.getTag()), is(equalTo(true)));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2021_A04_INSECURE_DESIGN.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2021_A04_INSECURE_DESIGN.getValue())));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2017_A06_SEC_MISCONFIG.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2017_A06_SEC_MISCONFIG.getValue())));
    }

    @Test
    void shouldPassAsThereIsNoBody() throws HttpMalformedHeaderException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");
        msg.setResponseBody("");
        setTextHtmlResponseHeader(msg);
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldPassWhenViewStateContainsUnderscore() throws HttpMalformedHeaderException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");
        msg.setResponseBody(
                "<html><head></head>"
                        + "<body>"
                        + "<input type='hidden' id='javax.faces.viewstate' value='_id1231'/>"
                        + "</body>"
                        + "</html>");
        setTextHtmlResponseHeader(msg);
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldPassNoJavaWordInViewState() throws IOException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");
        String encoded = Base64.getEncoder().encodeToString(gzipCompress("secureValue".getBytes()));
        msg.setResponseBody(
                "<html><head></head>"
                        + "<body>"
                        + "<input type='hidden' id='javax.faces.viewstate' value='"
                        + encoded
                        + "'/>"
                        + "</body>"
                        + "</html>");
        setTextHtmlResponseHeader(msg);
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldPassIfViewStateIsStoredOnServer() throws IOException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");
        // server side contains :, clear text no base64 or encryption
        String serverSideViewState = "123:123";
        msg.setResponseBody(
                "<html><head></head>"
                        + "<body>"
                        + "<input type='hidden' id='javax.faces.viewstate' value='"
                        + serverSideViewState
                        + "'/>"
                        + "</body>"
                        + "</html>");
        setTextHtmlResponseHeader(msg);
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldRaiseAlertIfViewStateContainsJavaWord() throws IOException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");
        String encoded = Base64.getEncoder().encodeToString("insecureValue_java".getBytes());
        msg.setResponseBody(
                "<html><head></head>"
                        + "<body>"
                        + "<input type='hidden' id='javax.faces.viewstate' value='"
                        + encoded
                        + "'/>"
                        + "</body>"
                        + "</html>");
        setTextHtmlResponseHeader(msg);
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0), containsNameLoadedWithKey(INSECURE_JSF));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_MEDIUM));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_LOW));
        assertThat(alertsRaised.get(0).getCweId(), equalTo(642));
        assertThat(alertsRaised.get(0).getWascId(), equalTo(14));
    }

    @Test
    void shouldRaiseAlertIfViewStateContainsJavaWordCompressed() throws IOException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");
        String encoded =
                Base64.getEncoder().encodeToString(gzipCompress("insecureValue_java".getBytes()));
        msg.setResponseBody(
                "<html><head></head>"
                        + "<body>"
                        + "<input type='hidden' id='javax.faces.viewstate' value='"
                        + encoded
                        + "'/>"
                        + "</body>"
                        + "</html>");
        setTextHtmlResponseHeader(msg);
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0), containsNameLoadedWithKey(INSECURE_JSF));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_MEDIUM));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_LOW));
        assertThat(alertsRaised.get(0).getCweId(), equalTo(642));
        assertThat(alertsRaised.get(0).getWascId(), equalTo(14));
    }

    @Test
    void shouldRaiseAlertIfInsecureValueViewStateIdWithFormId() throws IOException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");
        String encoded =
                Base64.getEncoder().encodeToString(gzipCompress("insecureValue_java".getBytes()));
        msg.setResponseBody(
                "<html><head></head>"
                        + "<body>"
                        + "<input type='hidden' id='j_id1:javax.faces.ViewState:0' value='"
                        + encoded
                        + "'/>"
                        + "</body>"
                        + "</html>");
        setTextHtmlResponseHeader(msg);
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0), containsNameLoadedWithKey(INSECURE_JSF));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_MEDIUM));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_LOW));
        assertThat(alertsRaised.get(0).getCweId(), equalTo(642));
        assertThat(alertsRaised.get(0).getWascId(), equalTo(14));
    }

    @Test
    void shouldRaiseAlertIfInsecureViewStateManyInputs() throws IOException {
        // Given
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader("GET http://www.example.com/test/ HTTP/1.1");
        String encoded =
                Base64.getEncoder().encodeToString(gzipCompress("insecureValue_java".getBytes()));
        msg.setResponseBody(
                "<html><head></head>"
                        + "<body>"
                        + "<input type='text' id='input1' value='input1'/>"
                        + "<input type='text' id='input2' value='input2'/>"
                        + "<input type='hidden' id='input3' value='input3'/>"
                        + "<input type='hidden' id='javax.faces.viewstate' value='"
                        + encoded
                        + "'/>"
                        + "<input type='hidden' id='input4' value='input4'/>"
                        + "</body>"
                        + "</html>");
        setTextHtmlResponseHeader(msg);
        // When
        scanHttpResponseReceive(msg);
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0), containsNameLoadedWithKey(INSECURE_JSF));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_MEDIUM));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_LOW));
        assertThat(alertsRaised.get(0).getCweId(), equalTo(642));
        assertThat(alertsRaised.get(0).getWascId(), equalTo(14));
    }

    private static byte[] gzipCompress(byte[] value) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            IOUtils.copy(new ByteArrayInputStream(value), gzip, 2048);
        }
        return output.toByteArray();
    }

    private static void setTextHtmlResponseHeader(HttpMessage msg)
            throws HttpMalformedHeaderException {
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/html;charset=UTF-8\r\n");
    }

    @Test
    void shouldHaveExpectedExampleAlert() {
        // Given / When
        List<Alert> alerts = rule.getExampleAlerts();
        // Then
        assertThat(alerts.size(), is(equalTo(1)));
    }

    @Test
    @Override
    public void shouldHaveValidReferences() {
        super.shouldHaveValidReferences();
    }
}
