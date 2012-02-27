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

package org.hippoecm.hst.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hippoecm.hst.configuration.channel.Channel;

/**
 * JAX-RS service implementation which is responsible for interacting with {@link Channel} resources
 */
@Path("/channels/")
public interface ChannelService {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Channel> getChannels();

	@GET
	@Path("/{uuid}/")
	@Produces(MediaType.APPLICATION_JSON)
	public Channel getChannel(@PathParam("uuid") String uuid);

}
