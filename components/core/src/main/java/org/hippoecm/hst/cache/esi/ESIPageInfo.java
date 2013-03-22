/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.esi;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.Header;
import net.sf.ehcache.constructs.web.PageInfo;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * ESIPageInfo
 */
public class ESIPageInfo extends PageInfo {

    private static final long serialVersionUID = 1L;

    private String bodyContent;
    private List<ESIFragmentInfo> fragmentInfos;

    public ESIPageInfo(int statusCode, String contentType, Collection cookies, byte [] body,
            boolean storeGzipped, long timeToLiveSeconds, Collection<Header<? extends Serializable>> headers)
            throws AlreadyGzippedException, UnsupportedEncodingException {
        this(statusCode, contentType, cookies, transformBodyBytesToString(body, contentType), storeGzipped, timeToLiveSeconds, headers);
    }

    public ESIPageInfo(int statusCode, String contentType, Collection cookies, String bodyContent,
            boolean storeGzipped, long timeToLiveSeconds, Collection<Header<? extends Serializable>> headers)
            throws AlreadyGzippedException {
        super(statusCode, contentType, cookies, ArrayUtils.EMPTY_BYTE_ARRAY, storeGzipped, timeToLiveSeconds, headers);
        this.bodyContent = bodyContent;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public void addAllFragmentInfos(Collection<ESIFragmentInfo> fragmentInfos) {
        if (this.fragmentInfos == null) {
            this.fragmentInfos = new LinkedList<ESIFragmentInfo>();
        }

        this.fragmentInfos.addAll(fragmentInfos);
    }

    public void addFragmentInfo(ESIFragmentInfo fragmentInfo) {
        if (fragmentInfos == null) {
            fragmentInfos = new LinkedList<ESIFragmentInfo>();
        }

        fragmentInfos.add(fragmentInfo);
    }

    public void removeAllFragmentInfos() {
        if (fragmentInfos != null) {
            fragmentInfos.clear();
        }
    }

    public List<ESIFragmentInfo> getFragmentInfos() {
        if (fragmentInfos != null) {
            return Collections.unmodifiableList(fragmentInfos);
        }

        return Collections.emptyList();
    }

    public boolean hasAnyFragmentInfo() {
        return (fragmentInfos != null && !fragmentInfos.isEmpty());
    }

    private static String transformBodyBytesToString(byte [] body, String contentType) throws UnsupportedEncodingException {
        if (body == null || body.length == 0) {
            return "";
        }

        String encoding = "UTF-8";

        if (contentType != null) {
            Map<String, String> params = MimeUtil.getHeaderParams(contentType);
            String charset = params.get("charset");

            if (StringUtils.isNotBlank(charset)) {
                encoding = charset;
            }
        }

        return new String(body, encoding);
    }
}