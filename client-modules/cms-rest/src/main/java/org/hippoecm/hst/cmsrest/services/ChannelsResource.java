/*
*  Copyright 2012 Hippo.
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

package org.hippoecm.hst.cmsrest.services;

import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.MESSAGE_CHANNELS_RETRIEVAL_ERROR;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.MESSAGE_CHANNEL_PERSISTING_ERROR;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.MESSAGE_CHANNEL_SAVING_ERROR;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.MESSAGE_CHEANNELS_RESOURCE_REQUEST_PROCESSING_ERROR;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.PARAM_MESSAGE_CHANNELS_RESOURCE_REQUEST_PROCESSING_ERROR;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.PARAM_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.PARAM_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_INFO_CLASS;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.PARAM_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_RESOURCE_VALUES;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.WARNING_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.WARNING_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_INFO_CLASS;
import static org.hippoecm.hst.cmsrest.services.ChannelsResourceConsts.WARNING_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_RESOURCE_VALUES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.rest.ChannelService;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.HstPropertyDefinitionInfo;
import org.hippoecm.hst.rest.beans.InformationObjectsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ChannelService} for CMS to interact with {@link Channel} resources
 */
public class ChannelsResource extends BaseResource implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelsResource.class);

	/* (non-Javadoc)
	 * @see org.hippoecm.hst.rest.ChannelService#getChannels()
	 */
	@Override
	public List<Channel> getChannels() {
		try {
			// Do required validations and throw @{link ResourceRequestValidationException} if there are violations
			// COMMENT - MNour: We should use a proper validation framework!
			validate();
	        return Collections.unmodifiableList(new ArrayList<Channel>(channelManager.getChannels().values()));
		} catch (ResourceRequestValidationException rrve) {
			if (log.isWarnEnabled()) {
				log.warn(MESSAGE_CHEANNELS_RESOURCE_REQUEST_PROCESSING_ERROR);
			}
			// COMMENT - MNour: This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			//                  For now return empty list
			// throw rrve;
			return Collections.emptyList();
		} catch (ChannelException ce) {
			if (log.isErrorEnabled()) {
				log.error(String.format(MESSAGE_CHANNELS_RETRIEVAL_ERROR, ce.getClass().getName(), ce.getMessage(), ce));
			}
			// COMMENT - MNour: This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
			//                  For now return empty list
			// throw ce;
			return Collections.emptyList();
		}
	}

	/* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#save(Channel channel)
     */
    @Override
    public void save(Channel channel) {
        try {
            // Do required validations and throw @{link ResourceRequestValidationException} if there are violations
            // COMMENT - MNour: We should use a proper validation framework!
            validate();
            // COMMENT - MNour: This is only test data
            channelManager.save(channel);
        } catch (ResourceRequestValidationException rrve) {
            if (log.isWarnEnabled()) {  
                log.warn(PARAM_MESSAGE_CHANNELS_RESOURCE_REQUEST_PROCESSING_ERROR, channel.getId());
            }
            // COMMENT - MNour: This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
            //                  For now return empty list
            // throw rrve;
            // COMMENT - MNour: I know returning 'null' is not clean at all but thats *only* for now!
        } catch (ChannelException ce) {
            if (log.isErrorEnabled()) {
                if (log.isErrorEnabled()) {
                    log.error(String.format(MESSAGE_CHANNEL_SAVING_ERROR, channel, ce.getClass().getName(), ce.getMessage(), ce));
                }
            }
        }
    }

    @Override
    public String persist(String blueprintId, Channel channel) {
        try {
            // Do required validations and throw @{link ResourceRequestValidationException} if there are violations
            // COMMENT - MNour: We should use a proper validation framework!
            validate();
            // COMMENT - MNour: This is only test data
            return channelManager.persist(blueprintId, channel);
        } catch (ResourceRequestValidationException rrve) {
            if (log.isWarnEnabled()) {  
                log.warn(PARAM_MESSAGE_CHANNELS_RESOURCE_REQUEST_PROCESSING_ERROR, channel.getId());
            }
            // COMMENT - MNour: This line of code is commented out intentionally. I want to know how exceptions are handled with HST REST services
            //                  For now return empty list
            // throw rrve;
            // COMMENT - MNour: I know returning 'null' is not clean at all but thats *only* for now!
        } catch (ChannelException ce) {
            if (log.isErrorEnabled()) {
                if (log.isErrorEnabled()) {
                    log.error(String.format(MESSAGE_CHANNEL_PERSISTING_ERROR, channel, ce.getClass().getName(), ce.getMessage(), ce));
                }
            }
        }

        // COMMENT - MNour: Bad, JAX-RS and exception handling and mapping should be leveraged and standardized across
        //                  HST, CMS and services!
        return null;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#getChannelPropertyDefinitions(String id)
     */
    @Override
    public List<HstPropertyDefinitionInfo> getChannelPropertyDefinitions(String id) {
        return InformationObjectsBuilder.buildHstPropertyDefinitionInfos(channelManager.getPropertyDefinitions(id));
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#getChannel(String id)
     */
    @Override
    public Channel getChannel(String id) {
        try {
            return channelManager.getChannelById(id);
        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn(String.format(WARNING_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL, id), ce);
            } else {
                log.warn(PARAM_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL, id, ce);
            }
        }

        // COMMENT - MNour: Bad, JAX-RS and exception handling and mapping should be leveraged and standardized across
        //                  HST, CMS and services!
        return null;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#canUserModifyChannels()
     */
    @Override
    public boolean canUserModifyChannels() {
        return channelManager.canUserModifyChannels();
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.rest.ChannelService#getChannelInfoClassInfo(String id)
     */
    @Override
    public ChannelInfoClassInfo getChannelInfoClassInfo(String id) {
        ChannelInfoClassInfo channelInfoClassInfo = null;

        try {
            Class<? extends ChannelInfo> channelInfoClass = channelManager.getChannelInfoClass(id);

            if (channelInfoClass != null) {
                channelInfoClassInfo = InformationObjectsBuilder.buildChannelInfoClassInfo(channelInfoClass);
            }

            return channelInfoClassInfo;
        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn(String.format(WARNING_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_INFO_CLASS, id), ce);
            } else {
                log.warn(PARAM_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_INFO_CLASS, id, ce);
            }
        }

        // COMMENT - MNour: Bad, JAX-RS and exception handling and mapping should be leveraged and standardized across
        //                  HST, CMS and services!        
        return null;
    }

    @Override
    public Properties getChannelResourceValues(String id, String language) {
        try {
            Channel channel = channelManager.getChannelById(id);
            return InformationObjectsBuilder.buildResourceBundleProperties(channelManager.getResourceBundle(channel, new Locale(language)));
        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn(String.format(WARNING_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_RESOURCE_VALUES, id), ce);
            } else {
                log.warn(PARAM_MESSAGE_FAILED_TO_RETRIEVE_CHANNEL_RESOURCE_VALUES, id, ce);
            }
        }

        // COMMENT - MNour: Bad, JAX-RS and exception handling and mapping should be leveraged and standardized across
        //                  HST, CMS and services!        
        return null;
    }

}
