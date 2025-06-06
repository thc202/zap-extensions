/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2019 The ZAP Development Team
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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.zaproxy.addon.commonlib.CommonAlertTag;
import org.zaproxy.addon.commonlib.PolicyTag;

class InformationDisclosureInUrlScanRuleUnitTest
        extends PassiveScannerTest<InformationDisclosureInUrlScanRule> {

    private static final String URI = "http://example.com/";
    private static final String BODY = "Some text in the response, doesn't matter.\nLine 2\n";

    @Override
    protected InformationDisclosureInUrlScanRule createScanner() {
        return new InformationDisclosureInUrlScanRule();
    }

    @Override
    public void setUpZap() throws Exception {
        super.setUpZap();

        Path xmlDir =
                Files.createDirectories(
                        Paths.get(
                                Constant.getZapHome(),
                                InformationDisclosureInUrlScanRule.URL_SENSITIVE_INFORMATION_DIR));
        Path testFile =
                xmlDir.resolve(InformationDisclosureInUrlScanRule.URL_SENSITIVE_INFORMATION_FILE);
        Files.write(testFile, Arrays.asList(" user", "password", "# notused", "session "));
    }

    protected HttpMessage createHttpMessageWithRespBody(String testURI)
            throws HttpMalformedHeaderException, URIException {

        HttpRequestHeader requestHeader = new HttpRequestHeader();
        requestHeader.setURI(new URI(testURI, false));

        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader(requestHeader);
        msg.setResponseBody(BODY);
        msg.setResponseHeader(
                "HTTP/1.1 200 OK\r\n"
                        + "Server: Apache-Coyote/1.1\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "Content-Length: "
                        + BODY.length()
                        + "\r\n");
        return msg;
    }

    @Test
    void shouldReturnExpectedMappings() {
        // Given / When
        Map<String, String> tags = rule.getAlertTags();
        // Then
        assertThat(tags.size(), is(equalTo(5)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2021_A01_BROKEN_AC.getTag()),
                is(equalTo(true)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2017_A03_DATA_EXPOSED.getTag()),
                is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.PENTEST.getTag()), is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.DEV_STD.getTag()), is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.QA_STD.getTag()), is(equalTo(true)));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2021_A01_BROKEN_AC.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2021_A01_BROKEN_AC.getValue())));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2017_A03_DATA_EXPOSED.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2017_A03_DATA_EXPOSED.getValue())));
    }

    @Test
    void shouldHaveExpectedExampleAlert() {
        // Given / When
        List<Alert> alerts = rule.getExampleAlerts();
        // Then
        assertThat(alerts.size(), is(equalTo(1)));
        Alert alert = alerts.get(0);
        assertThat(alert.getRisk(), is(equalTo(Alert.RISK_INFO)));
        assertThat(alert.getConfidence(), is(equalTo(Alert.CONFIDENCE_MEDIUM)));
        assertThat(alert.getCweId(), is(equalTo(598)));
    }

    @Test
    @Override
    public void shouldHaveValidReferences() {
        super.shouldHaveValidReferences();
    }

    @Test
    void sensitiveInfoInURLParamName() throws HttpMalformedHeaderException, URIException {

        // Given
        String sensitiveParamName = "UserName";
        String sensitiveValue = "Jonathon";
        String testURI =
                URI + "foo?bar=whodat&" + sensitiveParamName + "=" + sensitiveValue + "&what=up";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(1, alertsRaised.size());
        assertEquals(sensitiveParamName, alertsRaised.get(0).getParam());
        assertEquals(sensitiveParamName, alertsRaised.get(0).getEvidence());
    }

    @Test
    void noSensitiveInfoInURLParamName() throws HttpMalformedHeaderException, URIException {

        // Given
        String testURI = URI + "?notused=45365";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(0, alertsRaised.size());
    }

    @Test
    void creditCardNoDashesInURLParamValue() throws HttpMalformedHeaderException, URIException {

        // Given
        String sensitiveParamName = "docid";
        String sensitiveValue = "6011000990139424";
        String testURI = URI + "?" + sensitiveParamName + "=" + sensitiveValue + "&hl=en";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(1, alertsRaised.size());
        assertEquals(sensitiveParamName, alertsRaised.get(0).getParam());
        assertEquals(sensitiveValue, alertsRaised.get(0).getEvidence());
        assertEquals(
                Constant.messages.getString(
                        InformationDisclosureInUrlScanRule.MESSAGE_PREFIX + "otherinfo.cc"),
                alertsRaised.get(0).getOtherInfo());
    }

    @Test
    @Disabled(value = "Scanner does not yet eliminate dashes when looking for credit card numbers.")
    void creditCardDashesInURLParamValue() throws HttpMalformedHeaderException, URIException {

        // Given
        String testURI = URI + "?docid=6011-0009-9013-9424&hl=en";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(1, alertsRaised.size());
    }

    @Test
    void noCreditCardInURLParamValue() throws HttpMalformedHeaderException, URIException {

        // Given
        String testURI = URI + "?docid=123456&hl=en";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(0, alertsRaised.size());
    }

    @Test
    void emailAddressInURLParamValue() throws HttpMalformedHeaderException, URIException {

        // Given
        String sensitiveParamName = "docid";
        String sensitiveValue = "example@gmail.com";
        String testURI = URI + "?mailto=me&" + sensitiveParamName + "=" + sensitiveValue + "&hl=en";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(1, alertsRaised.size());
        assertEquals(sensitiveParamName, alertsRaised.get(0).getParam());
        assertEquals(sensitiveValue, alertsRaised.get(0).getEvidence());
        assertEquals(
                Constant.messages.getString(
                        InformationDisclosureInUrlScanRule.MESSAGE_PREFIX + "otherinfo.email"),
                alertsRaised.get(0).getOtherInfo());
    }

    @Test
    void noEmailAddressInURLParamValue() throws HttpMalformedHeaderException, URIException {

        // Given
        String testURI = URI + "?docid=exampleatgmail.com&hl=en";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(0, alertsRaised.size());
    }

    @Test
    void SsnDashesInURLParamValue() throws HttpMalformedHeaderException, URIException {

        // Given
        String sensitiveParamName = "docid";
        String sensitiveValue = "000-00-0000";
        String testURI = URI + "?" + sensitiveParamName + "=" + sensitiveValue + "&hl=en";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(1, alertsRaised.size());
        assertEquals(sensitiveParamName, alertsRaised.get(0).getParam());
        assertEquals(sensitiveValue, alertsRaised.get(0).getEvidence());
        assertEquals(
                Constant.messages.getString(
                        InformationDisclosureInUrlScanRule.MESSAGE_PREFIX + "otherinfo.ssn"),
                alertsRaised.get(0).getOtherInfo());
    }

    @Test
    void noSsnInURLParamValue() throws HttpMalformedHeaderException, URIException {

        // Given
        String testURI = URI + "?docid=snn-no-test&hl=en";
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(0, alertsRaised.size());
    }

    @Test
    void noQueryParams() throws HttpMalformedHeaderException, URIException {

        // Given
        String testURI = URI;
        HttpMessage msg = createHttpMessageWithRespBody(testURI);

        // When
        scanHttpRequestSend(msg);

        // Then
        assertEquals(0, alertsRaised.size());
    }
}
