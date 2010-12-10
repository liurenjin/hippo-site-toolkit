/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.mock.core.container;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.mock.util.MockBeanTestHelper;
import org.junit.Test;

public class TestMockHstComponentWindow {
    
    @Test
    public void testSimpleProperties() throws Exception {
        MockHstComponentWindow bean = new MockHstComponentWindow();
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "name", "test-name");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "referenceName", "test-referenceName");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "referenceNamespace", "test-referenceNamespace");
        
        HstComponent component = EasyMock.createNiceMock(HstComponent.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "component", component);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "renderPath", "test-renderPath");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "namedRenderer", "test-namedRenderer");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "serveResourcePath", "test-serveResourcePath");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "namedResourceServer", "test-namedResourceServer");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "pageErrorHandlerClassName", "test-pageErrorHandlerClassName");

        MockHstComponentWindow parentWindow = new MockHstComponentWindow();
        MockBeanTestHelper.verifyReadWriteProperty(bean, "parentWindow", parentWindow);
        
        HstResponseState responseState = EasyMock.createNiceMock(HstResponseState.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "responseState", responseState);
        
        HstComponentInfo componentInfo = EasyMock.createNiceMock(HstComponentInfo.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "componentInfo", componentInfo);
    }
    
}
