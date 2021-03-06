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
package org.hippoecm.hst.restapi.content.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hippoecm.hst.restapi.content.linking.Link;

public class SearchResultItem {
        @JsonProperty("name")
        public final String name;

        @JsonProperty("id")
        public final String uuid;

        @JsonProperty("link")
        public final Link link;

        public SearchResultItem(final String name, final String uuid, final Link link) {
            this.name = name;
            this.uuid = uuid;
            this.link = link;
        }
    }
