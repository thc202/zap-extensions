/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2024 The ZAP Development Team
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
package org.zaproxy.zap.extension.scripts.scanrules;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import net.htmlparser.jericho.Source;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.addon.commonlib.scanrules.ScanRuleMetadata;
import org.zaproxy.zap.control.AddOn;
import org.zaproxy.zap.extension.pscan.PluginPassiveScanner;
import org.zaproxy.zap.extension.script.ExtensionScript;
import org.zaproxy.zap.extension.script.ScriptEngineWrapper;
import org.zaproxy.zap.extension.script.ScriptWrapper;

public class PassiveScriptScanRule extends PassiveScriptHelper {

    private ExtensionScript extScript;
    private ScriptWrapper script;
    private CachedScriptInterfaces cachedScriptInterfaces;
    private ScanRuleMetadata metadata;

    public PassiveScriptScanRule(ScriptWrapper script, ScanRuleMetadata metadata) {
        this.script = script;
        cachedScriptInterfaces = new CachedScriptInterfaces(script);
        this.metadata = metadata;
    }

    @Override
    public void scanHttpResponseReceive(HttpMessage msg, int id, Source source) {
        try {
            var s = cachedScriptInterfaces.getInterface(script, PassiveScript.class);
            if (s != null) {
                s.scan(this, msg, source);
            }
        } catch (Exception e) {
            getExtScript().handleScriptException(script, e);
        }
    }

    // Note: This method is called always on the same scan rule instance
    // so should honour the threaded property of the script engine.
    @Override
    public boolean appliesToHistoryType(int historyType) {
        try {
            var s = cachedScriptInterfaces.getInterface(script, PassiveScript.class);
            if (s != null) {
                return execute(() -> s.appliesToHistoryType(historyType));
            }
        } catch (Exception e) {
            getExtScript().handleScriptException(script, e);
        }
        return false;
    }

    private <R> R execute(Callable<R> action) throws Exception {
        if (isSyncAccess()) {
            synchronized (this) {
                return action.call();
            }
        }
        return action.call();
    }

    private boolean isSyncAccess() {
        ScriptEngineWrapper engine = script.getEngine();
        return engine != null && engine.isSingleThreaded();
    }

    @Override
    public PluginPassiveScanner copy() {
        return new PassiveScriptScanRule(script, metadata);
    }

    @Override
    public AlertBuilder newAlert() {
        var alertBuilder =
                super.newAlert()
                        .setRisk(metadata.getRisk().getValue())
                        .setConfidence(metadata.getConfidence().getValue())
                        .setDescription(metadata.getDescription())
                        .setSolution(metadata.getSolution())
                        .setCweId(metadata.getCweId())
                        .setWascId(metadata.getWascId())
                        .setOtherInfo(metadata.getOtherInfo());
        if (metadata.getReferences() != null && !metadata.getReferences().isEmpty()) {
            alertBuilder.setReference(String.join("\n", metadata.getReferences()));
        }
        return alertBuilder;
    }

    @Override
    public String getName() {
        return metadata.getName();
    }

    @Override
    public int getPluginId() {
        return metadata.getId();
    }

    @Override
    public AddOn.Status getStatus() {
        return metadata.getStatus();
    }

    @Override
    public Map<String, String> getAlertTags() {
        return metadata.getAlertTags();
    }

    @Override
    public void setEnabled(boolean enabled) {
        script.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return script.isEnabled();
    }

    public String getCodeLink() {
        return metadata.getCodeLink();
    }

    public String getHelpLink() {
        return metadata.getHelpLink();
    }

    @Override
    public List<Alert> getExampleAlerts() {
        return List.of(newAlert().build());
    }

    final ScriptWrapper getScript() {
        return script;
    }

    void setMetadata(ScanRuleMetadata metadata) {
        this.metadata = metadata;
    }

    private ExtensionScript getExtScript() {
        if (extScript == null) {
            extScript =
                    Control.getSingleton().getExtensionLoader().getExtension(ExtensionScript.class);
        }
        return extScript;
    }
}
