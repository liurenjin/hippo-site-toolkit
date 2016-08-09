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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.ws.rs.Path;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.rest.beans.FieldGroupInfo;
import org.hippoecm.hst.rest.beans.HstPropertyDefinitionInfo;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.jaxrs.cxf.hst.HstCXFTestFixtureHelper;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

public class RootResourceTest extends AbstractResourceTest {
    private static final String MOCK_REST_PATH = "test-rootresource/";

    private ChannelService channelService;

    /**
     * Override the @Path annotation in the {@link ContainerItemComponentResource} for ease of testing
     */
    @Path(MOCK_REST_PATH)
    private static class RootResourceWithMockPath extends RootResource {
        @Override
        protected void resetSession() {
            // override to mock this method
        }
    }

    @Before
    public void setUp() throws RepositoryException {
        final HstRequestContext context = createMockHstContext();
        final HstCXFTestFixtureHelper helper = new HstCXFTestFixtureHelper(context);

        channelService = EasyMock.createMock(ChannelService.class);

        final RootResource rootResource = new RootResourceWithMockPath();
        rootResource.setChannelService(channelService);
        rootResource.setRootPath("/hst:hst");

        Config config = createDefaultConfig(JsonPojoMapperProvider.class)
                .addServerSingleton(rootResource)
                .addServerSingleton(helper);
        setup(config);
    }

    @Test
    public void can_get_channel_info_description() throws ChannelException {
        final Map<String, String> i18nResources = new HashMap<>();
        i18nResources.put("field1", "Field 1");
        i18nResources.put("field2", "Field 2");
        final List<FieldGroupInfo> fieldGroups = new ArrayList<>();
        final String[] fieldNames = {"field1", "field2"};
        fieldGroups.add(new FieldGroupInfo(fieldNames, "fieldGroup1"));
        fieldGroups.add(new FieldGroupInfo(fieldNames, "fieldGroup2"));

        final ChannelInfoDescription channelInfoDescription
                = new ChannelInfoDescription(fieldGroups, createPropertyDefinitions(), i18nResources, "tester");

        expect(channelService.getChannelInfoDescription("channel-foo", "nl"))
                .andReturn(channelInfoDescription);
        replay(channelService);

        when()
            .get(MOCK_REST_PATH + "channels/channel-foo/info?locale=nl")
        .then()
            .statusCode(200)
            .body("fieldGroups[0].titleKey", equalTo("fieldGroup1"),
                    "fieldGroups[1].titleKey", equalTo("fieldGroup2"),
                    "propertyDefinitions['field1'].name", equalTo("field1"),
                    "propertyDefinitions['field2'].name", equalTo("field2"),
                    "propertyDefinitions['field1'].annotations[0].type", equalTo("DropDownList"),
                    "propertyDefinitions['field1'].annotations[0].value", containsInAnyOrder("value-1", "value-2"),
                    "propertyDefinitions['field2'].annotations[0].value", containsInAnyOrder("value-3", "value-4"),
                    "i18nResources['field1']", equalTo("Field 1"),
                    "i18nResources['field2']", equalTo("Field 2"),
                    "lockedBy", equalTo("tester"));

        verify(channelService);
    }

    private Map<String, HstPropertyDefinitionInfo> createPropertyDefinitions() {
        final Map<String, HstPropertyDefinitionInfo> propertyDefinitions = new HashMap<>();

        final Annotation field1Annotation = createDropDownListAnnotation("value-1", "value-2");
        final Annotation field2Annotation = createDropDownListAnnotation("value-3", "value-4");

        propertyDefinitions.put("field1", createHstPropertyDefinitionInfo("field1", HstValueType.BOOLEAN, true, field1Annotation));
        propertyDefinitions.put("field2", createHstPropertyDefinitionInfo("field2", HstValueType.STRING, true, field2Annotation));
        return propertyDefinitions;
    }

    private static HstPropertyDefinitionInfo createHstPropertyDefinitionInfo(final String name,
                                                                             final HstValueType valueType,
                                                                             final boolean required,
                                                                             final Annotation annotation) {
        final HstPropertyDefinitionInfo propertyDefinitionInfo = new HstPropertyDefinitionInfo();
        propertyDefinitionInfo.setName(name);
        propertyDefinitionInfo.setValueType(valueType);
        propertyDefinitionInfo.setIsRequired(required);
        propertyDefinitionInfo.setAnnotations(Arrays.asList(annotation));
        return propertyDefinitionInfo;
    }

    @Test
    public void can_get_channel_info_description_with_default_locale() throws ChannelException {
        final Map<String, String> i18nResources = new HashMap<>();
        i18nResources.put("field1", "Field 1");
        final List<FieldGroupInfo> fieldGroups = new ArrayList<>();
        fieldGroups.add(new FieldGroupInfo(null, "fieldGroup1"));

        final ChannelInfoDescription channelInfoDescription
                = new ChannelInfoDescription(fieldGroups, createPropertyDefinitions(), i18nResources, null);

        expect(channelService.getChannelInfoDescription("channel-foo", "en"))
                .andReturn(channelInfoDescription);
        replay(channelService);

        when()
            .get(MOCK_REST_PATH + "channels/channel-foo/info")
        .then()
            .statusCode(200)
            .body("fieldGroups[0].titleKey", equalTo("fieldGroup1"),
                    "i18nResources", hasEntry("field1", "Field 1"));

        verify(channelService);
    }

    @Test
    public void cannot_get_channelsettings_information_when_sever_has_error() throws ChannelException {
        expect(channelService.getChannelInfoDescription("channel-foo", "en"))
                .andThrow(new ChannelException("unknown error"));
        replay(channelService);

        when()
            .get(MOCK_REST_PATH + "channels/channel-foo/info?locale=en")
        .then()
            .statusCode(500)
            .body(equalTo("Could not get channel setting information"));

        verify(channelService);
    }

    @Test
    public void can_save_channelsettings() throws ChannelException, RepositoryException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("foo", "bah");
        final Channel channelFoo = new Channel("channel-foo");
        channelFoo.setProperties(properties);

        channelService.saveChannel(mockSession, "channel-foo", channelFoo);
        expectLastCall();
        replay(channelService);

        given()
            .contentType(JSON)
            .body(channelFoo)
        .when()
            .put(MOCK_REST_PATH + "channels/channel-foo")
        .then()
            .statusCode(200)
            .body("properties.foo", equalTo("bah"));

        verify(channelService);
    }

    @Test
    public void cannot_save_channelsettings_when_server_has_error() throws ChannelException, RepositoryException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("foo", "bah");
        final Channel channelFoo = new Channel("channel-foo");
        channelFoo.setProperties(properties);

        final Capture<Channel> capturedArgument = new Capture<>();
        channelService.saveChannel(eq(mockSession), eq("channel-foo"), capture(capturedArgument));
        expectLastCall().andThrow(new IllegalStateException("something is wrong"));

        replay(channelService);

        given()
            .contentType(JSON)
            .body(channelFoo)
        .when()
            .put(MOCK_REST_PATH + "channels/channel-foo")
        .then()
            .statusCode(500);

        verify(channelService);
        assertThat(capturedArgument.getValue().getProperties().get("foo"), equalTo("bah"));
    }

    @Test
    public void can_delete_a_channel() throws ChannelException, RepositoryException {
        channelService.deleteChannel(eq(mockSession), eq("channel-foo"));
        expectLastCall();
        replay(channelService);

        given()
            .contentType(JSON)
        .when()
            .delete(MOCK_REST_PATH + "channels/channel-foo")
        .then()
            .statusCode(200);
        verify(channelService);
    }

    @Test
    public void cannot_delete_non_existent_channel() throws ChannelException, RepositoryException {
        channelService.deleteChannel(eq(mockSession), eq("channel-foo"));
        expectLastCall().andThrow(new ChannelNotFoundException("channel-foo"));
        replay(channelService);

        given()
            .contentType(JSON)
        .when()
            .delete(MOCK_REST_PATH + "channels/channel-foo")
        .then()
            .statusCode(404);
        verify(channelService);
    }

    private static Annotation createDropDownListAnnotation(String... values) {
        return new DropDownList() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return DropDownList.class;
            }

            @Override
            public String[] value() {
                return values;
            }
        };
    }
}