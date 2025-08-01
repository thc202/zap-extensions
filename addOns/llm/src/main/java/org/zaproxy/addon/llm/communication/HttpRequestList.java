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
package org.zaproxy.addon.llm.communication;

import dev.langchain4j.model.output.structured.Description;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class HttpRequestList {

    @Description("List of HTTP request objects")
    @Getter
    @Setter
    private List<HttpRequest> requests;

    public HttpRequestList(List<HttpRequest> requests) {
        this.requests = requests;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("HttpRequestList {\n");
        for (HttpRequest request : requests) {
            sb.append("  ").append(request).append(",\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
