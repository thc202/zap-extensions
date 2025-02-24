/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2020 The ZAP Development Team
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
package org.zaproxy.zap.extension.domxss;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.quality.Strictness;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.core.scanner.Plugin.AttackStrength;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.ExtensionLoader;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.OptionsParam;
import org.parosproxy.paros.model.Session;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.addon.commonlib.CommonAlertTag;
import org.zaproxy.addon.commonlib.PolicyTag;
import org.zaproxy.addon.network.ExtensionNetwork;
import org.zaproxy.zap.extension.ascan.VariantFactory;
import org.zaproxy.zap.extension.selenium.Browser;
import org.zaproxy.zap.extension.selenium.SeleniumOptions;
import org.zaproxy.zap.model.Context;
import org.zaproxy.zap.testutils.ActiveScannerTestUtils;
import org.zaproxy.zap.testutils.NanoServerHandler;
import org.zaproxy.zap.utils.I18N;
import org.zaproxy.zap.utils.ZapXmlConfiguration;

class DomXssScanRuleUnitTest extends ActiveScannerTestUtils<DomXssScanRule> {

    private static ExtensionNetwork extensionNetwork;

    private static Model model;
    private Session session;

    @BeforeAll
    static void setupAll() {
        Constant.messages = new I18N(Locale.ROOT);
        WebDriverManager.firefoxdriver().setup();
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setupEach() {
        model = mock(Model.class, withSettings().strictness(Strictness.LENIENT));
        Model.setSingletonForTesting(model);
        given(model.getVariantFactory()).willReturn(new VariantFactory());
        given(model.getOptionsParam()).willReturn(new OptionsParam());
        extensionNetwork = new ExtensionNetwork();
        extensionNetwork.initModel(model);
        Control.initSingletonForTesting(model, mock(ExtensionLoader.class));
        extensionNetwork.init();
        extensionNetwork.hook(new ExtensionHook(model, null));

        model.getOptionsParam().load(new ZapXmlConfiguration());
        model.getOptionsParam().addParamSet(new SeleniumOptions());

        session = new Session(model);
        given(model.getSession()).willReturn(session);
    }

    @AfterEach
    void tearDown() {
        rule.setTimeFinished();
        DomXssScanRule.proxy = null;
    }

    static Stream<String> testBrowsers() throws Exception {
        // TODO chrome-headless is failing in travis - need to investigate at some point
        return Stream.of("firefox-headless");
    }

    @AfterAll
    static void tidyUp() {
        DomXssScanRule.tidyUp();
        extensionNetwork.stop();
    }

    @Override
    protected DomXssScanRule createScanner() {
        DomXssScanRule.extensionNetwork = extensionNetwork;
        return new DomXssScanRule();
    }

    @Override
    protected void setUpMessages() {
        mockMessages(new ExtensionDomXSS());
    }

    @Override
    protected int getRecommendMaxNumberMessagesPerParam(AttackStrength strength) {
        if (strength == AttackStrength.LOW) {
            return NUMBER_MSGS_ATTACK_STRENGTH_LOW + 9;
        }
        return super.getRecommendMaxNumberMessagesPerParam(strength);
    }

    @Test
    void shouldUseDefaultWhenUnsupportedBrowser() throws IOException {
        // Given
        HttpMessage msg = this.getHttpMessage("");
        this.rule.getConfig().setProperty("rules.domxss.browserid", "opera");
        this.rule.init(msg, this.parent);

        // When / Then
        assertThat(this.rule.getBrowser(), equalTo(Browser.FIREFOX_HEADLESS));
    }

    @Test
    void shouldUseDefaultWhenUnknownBrowser() throws IOException {
        // Given
        HttpMessage msg = this.getHttpMessage("");
        this.rule.getConfig().setProperty("rules.domxss.browserid", "invalid");
        this.rule.init(msg, this.parent);

        // When / Then
        assertThat(this.rule.getBrowser(), equalTo(Browser.FIREFOX_HEADLESS));
    }

    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldUseCorrectBrowser(String browser) throws IOException {
        // Given
        HttpMessage msg = this.getHttpMessage("");
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When / Then
        assertThat(this.rule.getBrowser(), equalTo(Browser.getBrowserWithId(browser)));
    }

    /** Test based on http://public-firing-range.appspot.com/address/location.hash/assign */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInLocationHashAssign(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInLocationHashAssign/";

        this.nano.addHandler(new TestNanoServerHandler(test, "LocationHashAssign.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    /** Alerts raised are timing dependent, so any of these are good. */
    private void assertAlertsRaised() {
        assertAlertsRaised("", "");
    }

    private void assertAlertsRaised(String param, String otherInfo) {
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo(""));
        assertThat(alertsRaised.get(0).getParam(), equalTo(param));
        if (!otherInfo.isEmpty()) {
            assertThat(alertsRaised.get(0).getOtherInfo(), equalTo(otherInfo));
        }
        assertThat(
                alertsRaised.get(0).getAttack(),
                Matchers.anyOf(
                        equalTo(DomXssScanRule.POLYGLOT_ALERT),
                        equalTo(DomXssScanRule.HASH_JAVASCRIPT_ALERT),
                        equalTo(DomXssScanRule.QUERY_HASH_IMG_ALERT),
                        equalTo(DomXssScanRule.JAVASCRIPT_ALERT),
                        equalTo("<img src=\"random.gif\" onerror=alert(5397)>")));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_HIGH));
    }

    /** Test based on http://public-firing-range.appspot.com/address/location.hash/eval */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInLocationHashEval(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInLocationHashEval/";

        this.nano.addHandler(new TestNanoServerHandler(test, "LocationHashEval.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    /** Test based on http://public-firing-range.appspot.com/address/location.hash/replace */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInLocationHashReplace(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInLocationHashReplace/";

        this.nano.addHandler(new TestNanoServerHandler(test, "LocationHashReplace.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldNotReportXssWhenRandomAlertEncountered(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldNotReportXssWhenRandomAlertEncountered/";
        this.nano.addHandler(new TestNanoServerHandler(test, "RandomAlert.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    /** Test based on http://public-firing-range.appspot.com/address/location.hash/setTimeout */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInLocationHashSetTimeout(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInLocationHashSetTimeout/";

        this.nano.addHandler(new TestNanoServerHandler(test, "LocationHashSetTimeout.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    /** Test to trigger XSS after cancel button is clicked */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssWhenCancelButtonIsClicked(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssWhenCancelButtonIsClicked/";

        this.nano.addHandler(new TestNanoServerHandler(test, "CancelButton.html"));

        HttpMessage msg = this.getHttpMessage(test + "?returnUrl=javascript:alert()");
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);

        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    /** Test based on http://public-firing-range.appspot.com/address/location.hash/function */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInLocationHashFunction(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInLocationHashFunction/";

        this.nano.addHandler(new TestNanoServerHandler(test, "LocationHashFunction.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    /** Test based on http://public-firing-range.appspot.com/address/location.hash/jshref */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInLocationHashInlineEvent(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInLocationHashInlineEvent/";

        this.nano.addHandler(new TestNanoServerHandler(test, "LocationHashInlineEvent.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    /** Test based on http://public-firing-range.appspot.com/address/location.hash/formaction */
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInLocationHashFormAction(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInLocationHashFormAction/";

        this.nano.addHandler(new TestNanoServerHandler(test, "LocationHashFormAction.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
        assertThat(
                alertsRaised.get(0).getOtherInfo(),
                equalTo(
                        ("The following steps were done to trigger the DOM XSS:\n"
                                        + "With <PAYLOAD_0> as: #jaVasCript:/*-/*`/*\\`/*'/*\"/**/(/* */oNcliCk=alert(5397) )//%0D%0A%0d%0a//</stYle/</titLe/</teXtarEa/</scRipt/--!>\\x3csVg/<sVg/oNloAd=alert(5397)//>\\x3e\n"
                                        + "Access: http://localhost:%PORT%/shouldReportXssInLocationHashFormAction/<PAYLOAD_0>\n"
                                        + "Write to /html/form/input the value: <PAYLOAD_0>\n"
                                        + "Click element: /html/form/input\n")
                                .replace("%PORT%", String.valueOf(nano.getListeningPort()))));
    }

    /**
     * Test based on
     * http://public-firing-range.appspot.com/dom/eventtriggering/document/formSubmission/innerHtml
     * Note that this only works in Firefox, not Chrome.
     */
    @Disabled
    @Test
    void shouldReportXssInEventInnerHtmlFirefox() throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInEventInnerHtml/";

        this.nano.addHandler(new TestNanoServerHandler(test, "EventInnerHtml.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule
                .getConfig()
                .setProperty("rules.domxss.browserid", Browser.FIREFOX_HEADLESS.name());
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    /**
     * Test based on
     * http://public-firing-range.appspot.com/dom/eventtriggering/document/inputTyping/innerHtml
     */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInTypingInnerHtml(String browser) throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInTypingInnerHtml/";

        this.nano.addHandler(new TestNanoServerHandler(test, "TypingInnerHtml.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    /** Test based on http://public-firing-range.appspot.com/dom/dompropagation/ */
    @Disabled
    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInDomPropagation(String browser) throws NullPointerException, IOException {
        // Given
        String test = "/shouldReportXssInDomPropagation/";

        this.nano.addHandler(new TestNanoServerHandler(test, "DomPropagation.html"));

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised();
    }

    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportXssInQueryParameters(String browser) throws NullPointerException, IOException {
        // Given
        String test = "/";
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                        String value = getFirstParamValue(session, "iv");
                        if (value == null) {
                            value = "";
                        }
                        return newFixedLengthResponse(
                                "<html><body>\n"
                                        + "<script>var input=\""
                                        + value
                                        + "\";eval(input);</script>\n"
                                        + "</body></html>");
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?iv=value");
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised("iv", "");
    }

    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldReportStepsInOtherInfoForXssInQueryParameters(String browser)
            throws NullPointerException, IOException {
        // Given
        String test = "/";
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                        String value = getFirstParamValue(session, "iv");
                        if (value != null
                                && value.startsWith(
                                        "<img src=\"random.gif\" onerror=alert(5397)>")) {
                            value = "javascript:alert(5397)";
                        } else {
                            value = "";
                        }
                        return newFixedLengthResponse(
                                "<html><body>\n"
                                        + "<script>var input=\""
                                        + value
                                        + "\";eval(input);</script>\n"
                                        + "</body></html>");
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?iv=value");
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertAlertsRaised("iv", "");
        assertThat(
                alertsRaised.get(0).getOtherInfo(),
                equalTo(
                        ("The following steps were done to trigger the DOM XSS:\n"
                                        + "With <PAYLOAD_1> as: %3Cimg%20src=%22random.gif%22%20onerror=alert(5397)%3E\n"
                                        + "Access: http://localhost:%PORT%/?iv=<PAYLOAD_1>\n")
                                .replace("%PORT%", String.valueOf(nano.getListeningPort()))));
    }

    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldRequestReferencedUrl(String browser) throws NullPointerException, IOException {
        // Given
        String test = "/";
        final AtomicBoolean accessedImage = new AtomicBoolean(false);
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                        if ("/".equals(session.getUri())) {
                            return newFixedLengthResponse(
                                    "<html><body><img src=\"test.png\" alt=\"Test\"/>\n</body></html>");
                        }
                        if ("/test.png".equals(session.getUri())) {
                            accessedImage.set(true);
                        }
                        return newFixedLengthResponse("<html><body>\n" + "test" + "</body></html>");
                    }
                });

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertThat(accessedImage.get(), is(equalTo(true)));
    }

    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldNotRequestReferencedUrlExcludedGlobally(String browser)
            throws NullPointerException, IOException {
        // Given
        session = mock(Session.class, withSettings().strictness(Strictness.LENIENT));
        given(model.getSession()).willReturn(session);
        given(session.getGlobalExcludeURLRegexs()).willReturn(List.of(".*.png"));

        String test = "/";
        final AtomicBoolean accessedImage = new AtomicBoolean(false);
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                        if ("/".equals(session.getUri())) {
                            return newFixedLengthResponse(
                                    "<html><body><img src=\"test.png\" alt=\"Test\"/>\n</body></html>");
                        }
                        if ("/test.png".equals(session.getUri())) {
                            accessedImage.set(true);
                        }
                        return newFixedLengthResponse("<html><body>\n" + "test" + "</body></html>");
                    }
                });

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertThat(accessedImage.get(), is(equalTo(false)));
    }

    @ParameterizedTest
    @MethodSource("testBrowsers")
    void shouldNotRequestReferencedUrlExcludedInSession(String browser)
            throws NullPointerException, IOException {
        // Given
        Context context = mock(Context.class);
        given(context.isExcluded(ArgumentMatchers.anyString())).willReturn(false);
        given(context.isExcluded(ArgumentMatchers.matches(".*/test.png"))).willReturn(true);

        String test = "/";
        final AtomicBoolean accessedImage = new AtomicBoolean(false);
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                        if ("/".equals(session.getUri())) {
                            return newFixedLengthResponse(
                                    "<html><body><img src=\"test.png\" alt=\"Test\"/>\n</body></html>");
                        }
                        if ("/test.png".equals(session.getUri())) {
                            accessedImage.set(true);
                        }
                        return newFixedLengthResponse("<html><body>\n" + "test" + "</body></html>");
                    }
                });

        HttpMessage msg = this.getHttpMessage(test);
        this.rule.getConfig().setProperty("rules.domxss.browserid", browser);
        this.rule.init(msg, this.parent);
        this.parent.setContext(context);

        // When
        this.rule.scan();

        // Then
        assertThat(accessedImage.get(), is(equalTo(false)));
    }

    @Test
    void shouldReturnExpectedMappings() {
        // Given / When
        int cwe = rule.getCweId();
        Map<String, String> tags = rule.getAlertTags();
        // Then
        assertThat(cwe, is(equalTo(79)));
        assertThat(tags.size(), is(equalTo(7)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2021_A03_INJECTION.getTag()),
                is(equalTo(true)));
        assertThat(tags.containsKey(CommonAlertTag.OWASP_2017_A07_XSS.getTag()), is(equalTo(true)));
        assertThat(
                tags.containsKey(CommonAlertTag.WSTG_V42_CLNT_01_DOM_XSS.getTag()),
                is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.DEV_FULL.getTag()), is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.QA_STD.getTag()), is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.QA_FULL.getTag()), is(equalTo(true)));
        assertThat(tags.containsKey(PolicyTag.SEQUENCE.getTag()), is(equalTo(true)));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2021_A03_INJECTION.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2021_A03_INJECTION.getValue())));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2017_A07_XSS.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2017_A07_XSS.getValue())));
        assertThat(
                tags.get(CommonAlertTag.WSTG_V42_CLNT_01_DOM_XSS.getTag()),
                is(equalTo(CommonAlertTag.WSTG_V42_CLNT_01_DOM_XSS.getValue())));
    }

    private class TestNanoServerHandler extends NanoServerHandler {
        private String fileName;

        public TestNanoServerHandler(String name, String fileName) {
            super(name);
            this.fileName = fileName;
        }

        @Override
        protected Response serve(IHTTPSession session) {
            String response = getHtml(fileName);
            return newFixedLengthResponse(response);
        }
    }
}
