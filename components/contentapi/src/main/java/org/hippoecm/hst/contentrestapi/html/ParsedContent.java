/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.contentrestapi.html;

import java.util.Map;

import org.hippoecm.hst.contentrestapi.linking.Link;

import static java.util.Collections.emptyMap;

public class ParsedContent {

    private String rewrittenHtml;
    private Map<String, Link> linkMap = emptyMap();

    public ParsedContent(final String rewrittenHtml, final Map<String, Link> linkMap) {
        this.rewrittenHtml = rewrittenHtml;
        this.linkMap = linkMap;
    }

    public String getRewrittenHtml() {
        return rewrittenHtml;
    }

    public Map<String, Link> getLinks() {
        return linkMap;
    }
}

