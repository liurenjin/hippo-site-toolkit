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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.MultivaluedMap;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ServerErrorException;

public interface ContainerItemComponentService {

    Set<String> getVariants() throws ClientException, RepositoryException;

    ContainerItemComponentRepresentation getVariant(final String variantId, final String locale) throws ClientException, RepositoryException, ServerErrorException;

    Set<String> retainVariants(final Set<String> variants, final long versionStamp) throws ClientException, RepositoryException, ServerErrorException;

    void createVariant(final String variantId, final long versionStamp) throws ClientException, RepositoryException, ServerErrorException;

    void deleteVariant(final String variantId, final long versionStamp) throws ClientException, RepositoryException;

    void updateVariant(final String variantId, final long versionStamp, final MultivaluedMap<String, String> params) throws ClientException, RepositoryException;

    /**
     * Saves parameters for the given new variant, and also removes the old variant. This effectively renames the old
     * variant to the new one.
     *
     * @param oldVariantId the old variant to remove
     * @param newVariantId the new variant to store parameters for
     * @param params     the parameters to store
     * @return whether saving the parameters went successfully or not.
     */
    void moveAndUpdateVariant(final String oldVariantId, final String newVariantId,
                              final long versionStamp, final MultivaluedMap<String, String> params) throws ClientException, RepositoryException;
}
