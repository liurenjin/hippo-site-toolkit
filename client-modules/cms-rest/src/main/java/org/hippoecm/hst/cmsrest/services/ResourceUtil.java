package org.hippoecm.hst.cmsrest.services;

import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for resource classes.
 */
class ResourceUtil {

    private static final Logger log = LoggerFactory.getLogger(ResourceUtil.class);

    private ResourceUtil() {
        // prevent instantiation
    }

    /**
     * Returns the node with the given UUID using the session of the given request context.
     *
     * @param requestContext the request context
     * @param uuidParam a UUID
     *
     * @return the node with the given UUID, or null if no such node could be found.
     */
    static Node getNode(final HstRequestContext requestContext, final String uuidParam) {
        if (uuidParam == null) {
            log.info("UUID is null, returning null", uuidParam);
            return null;
        }

        final String uuid = PathUtils.normalizePath(uuidParam);

        try {
            UUID.fromString(uuid);
        } catch(IllegalArgumentException e) {
            log.info("Illegal UUID: '{}', returning null", uuidParam);
            return null;
        }

        try {
            final Session jcrSession = requestContext.getSession();
            // We explicitly call a refresh here. Normally, the sessionstateful jcr session is already refreshed.
            // However, due to asychronous JCR event dispatching, there might be changes in the repository, but not
            // yet a JCR event was sent that triggers a JCR session refresh. Hence, here we explicitly refresh the
            // JCR session again.
            jcrSession.refresh(false);
            return jcrSession.getNodeByIdentifier(uuid);
        } catch (ItemNotFoundException e) {
            log.warn("Node not found: '{}', returning null", uuid);
        } catch (RepositoryException e) {
            log.warn("Error while fetching node with UUID '" + uuid + "', returning null", e);
        }
        return null;
    }

}
