/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2025 The ZAP Development Team
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
package org.zaproxy.addon.llm.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpSender;
import org.zaproxy.zap.utils.ThreadUtils;

public class HistoryPersister {

    private static final Logger LOGGER = LogManager.getLogger(HistoryPersister.class);

    private final ExtensionHistory extHistory;

    public HistoryPersister() {
        this.extHistory =
                Control.getSingleton().getExtensionLoader().getExtension(ExtensionHistory.class);
    }

    public void handleMessage(final HttpMessage message, int initiator) {
        HistoryReference historyRef;

        try {
            historyRef =
                    new HistoryReference(
                            Model.getSingleton().getSession(),
                            initiator == HttpSender.SPIDER_INITIATOR
                                    ? HistoryReference.TYPE_SPIDER
                                    : HistoryReference.TYPE_ZAP_USER,
                            message);

        } catch (Exception e) {
            LOGGER.warn("Failed to persist the message: {}", e.getMessage(), e);
            return;
        }

        ThreadUtils.invokeAndWaitHandled(
                () -> {
                    if (extHistory != null) {
                        extHistory.addHistory(historyRef);
                    }
                    Model.getSingleton().getSession().getSiteTree().addPath(historyRef, message);
                });
    }
}
