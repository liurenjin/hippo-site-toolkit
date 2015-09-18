/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;

import javax.servlet.ServletContext;

import org.apache.commons.lang.time.DateFormatUtils;
import org.easymock.EasyMock;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ParameterUtilsTest {

    private HstComponent component;
    private HstRequestContext requestContext;
    private HstRequest request;
    private ComponentConfiguration componentConfig;
    private ResolvedSiteMapItem resolvedSiteMapItem;

    @Before
    public void setUp() throws Exception {
        component = new TestSearchComponent();
        request = EasyMock.createNiceMock(HstRequest.class);
        requestContext = EasyMock.createNiceMock(HstRequestContext.class);
        HstParameterInfoProxyFactory parametersInfoProxyFactory = new HstParameterInfoProxyFactoryImpl();
        EasyMock.expect(requestContext.getParameterInfoProxyFactory()).andReturn(parametersInfoProxyFactory).anyTimes();
        EasyMock.expect(request.getRequestContext()).andReturn(requestContext).anyTimes();
        resolvedSiteMapItem = EasyMock.createNiceMock(ResolvedSiteMapItem.class);
        EasyMock.expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();
        componentConfig = EasyMock.createNiceMock(ComponentConfiguration.class);

        EasyMock.replay(request);
        EasyMock.replay(requestContext);
        EasyMock.replay(resolvedSiteMapItem);
    }

    @Test
    public void testParameterValues() throws Exception {
        EasyMock.expect(componentConfig.getParameter("queryOption", resolvedSiteMapItem)).andReturn("queryOptionValue1").anyTimes();
        EasyMock.expect(componentConfig.getParameter("since", resolvedSiteMapItem)).andReturn("2014-10-7").anyTimes();
        EasyMock.replay(componentConfig);

        SearchInfo searchInfo = ParameterUtils.getParametersInfo(component, componentConfig, request);

        assertEquals("queryOptionValue1", searchInfo.getQueryOption());

        Calendar since = searchInfo.getSince();
        assertEquals("2014-10-07", DateFormatUtils.format(since, ParameterUtils.ISO_DATE_FORMAT));
    }

    @Ignore(value="As long as HSTTWO-3405 is not done this test will fail")
    @Test
    public void test_parameter_values_when_parametersInfo_on_super_class()  throws Exception {
        final TestSubSearchComponent testSubSearchComponent = new TestSubSearchComponent();
        EasyMock.expect(componentConfig.getParameter("queryOption", resolvedSiteMapItem)).andReturn("queryOptionValue1").anyTimes();
        EasyMock.expect(componentConfig.getParameter("since", resolvedSiteMapItem)).andReturn("2014-10-7").anyTimes();
        EasyMock.replay(componentConfig);

        SearchInfo searchInfo = ParameterUtils.getParametersInfo(testSubSearchComponent, componentConfig, request);

        assertEquals("queryOptionValue1", searchInfo.getQueryOption());

        Calendar since = searchInfo.getSince();
        assertEquals("2014-10-07", DateFormatUtils.format(since, ParameterUtils.ISO_DATE_FORMAT));
    }


    @Test
    public void testDateParameterValues() throws Exception {
        EasyMock.expect(componentConfig.getParameter("since", resolvedSiteMapItem)).andReturn("2014-10-7T11:59:59").anyTimes();
        EasyMock.replay(componentConfig);
        SearchInfo searchInfo = ParameterUtils.getParametersInfo(component, componentConfig, request);
        Calendar since = searchInfo.getSince();
        assertEquals("2014-10-07T11:59:59", DateFormatUtils.format(since, ParameterUtils.ISO_DATETIME_FORMAT));

        EasyMock.reset(componentConfig);
        EasyMock.expect(componentConfig.getParameter("since", resolvedSiteMapItem)).andReturn("2014-10-7").anyTimes();
        EasyMock.replay(componentConfig);
        searchInfo = ParameterUtils.getParametersInfo(component, componentConfig, request);
        since = searchInfo.getSince();
        assertEquals("2014-10-07", DateFormatUtils.format(since, ParameterUtils.ISO_DATE_FORMAT));

        EasyMock.reset(componentConfig);
        EasyMock.expect(componentConfig.getParameter("since", resolvedSiteMapItem)).andReturn("T11:59:59").anyTimes();
        EasyMock.replay(componentConfig);
        searchInfo = ParameterUtils.getParametersInfo(component, componentConfig, request);
        since = searchInfo.getSince();
        assertEquals("T11:59:59", DateFormatUtils.format(since, ParameterUtils.ISO_TIME_FORMAT));
    }

    @Test
    public void emptyDateParameterReturnsNull() throws Exception {
        EasyMock.expect(componentConfig.getParameter("dateWithoutDefaultValue", resolvedSiteMapItem)).andReturn("").anyTimes();
        EasyMock.replay(componentConfig);
        SearchInfo searchInfo = ParameterUtils.getParametersInfo(component, componentConfig, request);
        Calendar date = searchInfo.getDateWithoutDefaultValue();
        assertNull("Empty date parameter should return null", date);
    }

    @ParametersInfo(type=SearchInfo.class)
    public class TestSearchComponent implements HstComponent {

        @Override
        public void init(ServletContext servletContext, ComponentConfiguration componentConfig)
                throws HstComponentException {
        }

        @Override
        public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        }

        @Override
        public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        }

        @Override
        public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        }

        @Override
        public void destroy() throws HstComponentException {
        }

    }

    // parameters info on super class
    public class TestSubSearchComponent extends TestSearchComponent {
    }

    /**
     * NOTE: Calendar or Date return type requires ISO8601 date string.
     * @see {@link ParameterUtils#ISO8601_DATETIME_PATTERNS}.
     */
    public interface SearchInfo {

        @Parameter(name = "queryOption")
        String getQueryOption();

        @Parameter(name = "since", defaultValue = "2012-1-27", displayName = "Since")
        Calendar getSince();

        @Parameter(name = "dateWithoutDefaultValue")
        Calendar getDateWithoutDefaultValue();

    }

}
