/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.util;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.container.ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL;
import static org.onehippo.cms7.services.webfiles.WebFilesService.JCR_ROOT_PATH;

public class WebFileUtils {

    private static final Logger log = LoggerFactory.getLogger(WebFileUtils.class);

    public static final String DEFAULT_BUNDLE_NAME = "site";

    public static String getBundleName(HstRequestContext requestContext) {
        String bundleName = requestContext.getResolvedMount().getMount().getContextPath();

        if (bundleName == null || bundleName.length() == 0) {
            bundleName = DEFAULT_BUNDLE_NAME;
        } else if (bundleName.startsWith("/")) {
            bundleName = bundleName.substring(1);
        }

        HstSite hstSite = requestContext.getResolvedMount().getMount().getHstSite();
        if (hstSite != null && hstSite.getChannel() != null && hstSite.getChannel().getBranchId() != null) {
            final String branchBundleName = bundleName + "-" + hstSite.getChannel().getBranchId();
            try {
                if (requestContext.getSession().nodeExists(JCR_ROOT_PATH + "/" + branchBundleName)) {
                    log.info("Using branch bundle name '{}'", branchBundleName);
                    return branchBundleName;
                }
            } catch (RepositoryException e) {
                log.warn("RepositoryException ", e);
            }
        }

        return bundleName;
    }

    public static String webFilePathToJcrPath(final String templateSource) {
        final String webFilePath = "/" + PathUtils.normalizePath(templateSource.substring(
                FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL.length()));
        final String bundleName = getBundleName();
        return JCR_ROOT_PATH + "/" + bundleName + webFilePath;
    }

    public static String jcrPathToWebFilePath(final String variantJcrPath) {
        final String bundleName = getBundleName();
        final String requiredPrefix = JCR_ROOT_PATH + "/" + bundleName + "/";
        if (!variantJcrPath.startsWith(requiredPrefix)) {
            final String msg = String.format("Cannot translate '%s' to web file path because '%s' does not start" +
                    " with '%s'", variantJcrPath, variantJcrPath, requiredPrefix);
            throw new IllegalArgumentException(msg);
        }
        return FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL + "/" + variantJcrPath.substring(requiredPrefix.length());
    }

    private static String getBundleName() {
        final HstRequestContext ctx = RequestContextProvider.get();
        if (ctx == null) {
            throw new IllegalStateException("Cannot serve freemarker template from web file because there is no HstRequestContext.");
        }
        return getBundleName(ctx);
    }

}
