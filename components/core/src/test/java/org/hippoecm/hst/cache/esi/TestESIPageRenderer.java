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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.constructs.web.Header;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestESIPageRenderer
 */
public class TestESIPageRenderer {

    private static final String EXAMPLE_LICENSE_TEXT = "Example License (\"http://www.example.com/LICENSE\")";

    private static Logger log = LoggerFactory.getLogger(TestESIPageRenderer.class);

    @Test
    public void testESIPageRendering() throws Exception {
        int statusCode = HttpServletResponse.SC_OK;
        String contentType = "text/html; charset=UTF-8";
        Collection cookies = new ArrayList();
        boolean storeGzipped = false;
        long timeToLiveSeconds = 60;
        Collection<Header<? extends Serializable>> headers = new ArrayList<Header<? extends Serializable>>();

        byte [] body = readURLContentAsByteArray(getClass().getResource("esi-source-page-1.html"));
        ESIPageInfo pageInfo = new ESIPageInfo(statusCode, contentType, cookies, body, storeGzipped, timeToLiveSeconds, headers);
        ESIPageScanner scanner = new ESIPageScanner();
        List<ESIFragmentInfo> fragmentInfos = scanner.scanFragmentInfos(pageInfo.getBodyContent());
        pageInfo.addAllFragmentInfos(fragmentInfos);

        StringWriter writer = new StringWriter();

        ESIPageRenderer renderer = new ESIPageRenderer() {
            @Override
            protected void writeIncludeElementFragment(Writer writer, ESIElementFragment fragment) {
                if (StringUtils.endsWith(fragment.getElement().getAttribute("src"), "example.com/LICENSE")) {
                    try {
                        writer.write(EXAMPLE_LICENSE_TEXT);
                    } catch (IOException e) {
                        log.warn("Failed to write content", e);
                    }
                }
            }
        };

        renderer.render(writer, pageInfo);

        String expectedBodyContent = new String(readURLContentAsByteArray(getClass().getResource("esi-result-page-1.html")), "UTF-8");
        String renderedBodyContent = writer.toString();
        log.info("renderedBodyContent:\n{}", renderedBodyContent);
        assertEquals(expectedBodyContent, renderedBodyContent);
    }

    private byte [] readURLContentAsByteArray(URL url) throws IOException {
        InputStream is = null;

        try {
            is = url.openStream();
            return IOUtils.toByteArray(url.openStream());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}