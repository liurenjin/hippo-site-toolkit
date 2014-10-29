/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cache.webresources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.onehippo.cms7.services.webresources.Binary;

/**
 * Serialzable web resource binary that is stored in-memory and hence can be cached.
 */
public class CacheableBinary implements Binary, Serializable {

    private byte[] data = ArrayUtils.EMPTY_BYTE_ARRAY;

    public CacheableBinary(final Binary binary) throws IOException {
        final InputStream stream = binary.getStream();
        try {
            data = IOUtils.toByteArray(stream);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @Override
    public InputStream getStream() {
        // don't use a buffered stream since the data is already buffered in memory
        return new ByteArrayInputStream(data);
    }

    @Override
    public long getSize() {
        return data.length;
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }
}
