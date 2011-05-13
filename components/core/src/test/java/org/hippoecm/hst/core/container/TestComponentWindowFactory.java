package org.hippoecm.hst.core.container;

import java.util.TreeMap;
import java.util.TreeSet;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestComponentWindowFactory {

    @Test
    public void testDefaultAndNonMatchingConditionsAreIgnored() {
        HstComponentWindowFactoryImpl factory = new HstComponentWindowFactoryImpl();

        // set up request context
        MockHstRequestContext requestContext = new MockHstRequestContext();
        TreeSet<String> tags = new TreeSet<String>();
        tags.add("enabled");
        requestContext.setComponentFilterTags(tags);

        // mock environment
        HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
        HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
        expect(compConfig.getReferenceName()).andReturn("refName");
        HstComponentFactory compFactory = createNiceMock(HstComponentFactory.class);
        expect(compFactory.getComponentInstance(mockHstContainerConfig, compConfig)).andReturn(new GenericHstComponent());

        // container items with matching, non-matching and no tags
        TreeMap<String, HstComponentConfiguration> children = getContainerItemConfigurations();
        expect(compConfig.getChildren()).andReturn(children);

        // instantiate the window
        replay(mockHstContainerConfig, compConfig, compFactory);
        HstComponentWindow window = factory.create(mockHstContainerConfig, requestContext, compConfig, compFactory);

        // verify results
        verify(mockHstContainerConfig, compConfig, compFactory);
        assertNotNull(window.getChildWindow("enabled"));
        assertNull(window.getChildWindow("disabled"));
        assertNull(window.getChildWindow("default"));
    }

    private TreeMap<String, HstComponentConfiguration> getContainerItemConfigurations() {
        TreeMap<String, HstComponentConfiguration> children = new TreeMap<String, HstComponentConfiguration>();
        MockHstComponentConfiguration enabledChild = new MockHstComponentConfiguration("enabled") {
            @Override
            public String[] getComponentFilterTags() {
                return new String[]{"enabled"};
            }
        };
        enabledChild.setName("enabled");
        children.put("enabled", enabledChild);
        MockHstComponentConfiguration disabledChild = new MockHstComponentConfiguration("disabled") {
            @Override
            public String[] getComponentFilterTags() {
                return new String[]{"disabled"};
            }
        };
        disabledChild.setName("disabled");
        children.put("disabled", disabledChild);
        MockHstComponentConfiguration defaultChild = new MockHstComponentConfiguration("default");
        defaultChild.setName("default");
        children.put("default", defaultChild);
        return children;
    }

}
