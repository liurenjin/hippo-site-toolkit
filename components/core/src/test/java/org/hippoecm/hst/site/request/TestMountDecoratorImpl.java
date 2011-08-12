/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.site.request;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.junit.Test;

public class TestMountDecoratorImpl {
    @Test
    public void testDecorationsOfLiveToPreviewMount() throws Exception {
        ContextualizableMount mount = createNiceMock(ContextualizableMount.class);

        expect(mount.isPreview()).andReturn(false).anyTimes();
        expect(mount.getMountPoint()).andReturn("/hst:hst/hst:sites/myproject").anyTimes();
        expect(mount.getPreviewMountPoint()).andReturn("/hst:hst/hst:sites/myproject-preview").anyTimes();
        expect(mount.getType()).andReturn("live").anyTimes();
        String[] arr = {"foo", "bar", "lux"};
        List<String> types = Arrays.asList(arr);
        expect(mount.getTypes()).andReturn(types).anyTimes();
        
        replay(mount);
        Mount decoratedMount = new MountDecoratorImpl().decorateMountAsPreview(mount);
        assertTrue("The decorated mount is expected to be a preview. ", decoratedMount.isPreview());
        assertEquals("The decorated mount should have a mountPoint '/hst:hst/hst:sites/myproject-preview'. ","/hst:hst/hst:sites/myproject-preview", decoratedMount.getMountPoint());

        assertEquals("The decorated mount should should NEVER change the type, also not live to preview. ",decoratedMount.getType(), "live");
        assertEquals("The decorated mount should should NEVER change the types ",decoratedMount.getTypes().get(0), "foo");
        
    }
    
    @Test
    public void testDecorationsOfAlreadyPreviewMount() throws Exception {
        ContextualizableMount mount = createNiceMock(ContextualizableMount.class);

        expect(mount.isPreview()).andReturn(true).anyTimes();
        replay(mount);
        Mount decoratedMount = new MountDecoratorImpl().decorateMountAsPreview(mount);
        assertTrue("The decoratedMount of a mount that is already preview should be the same instance. ", decoratedMount == mount);
        
    }
}
