/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.transit_data_manager.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onebusaway.nyc.util.git.GitRepositoryHelper;
import org.onebusaway.nyc.util.model.GitRepositoryState;
import org.springframework.stereotype.Component;


/**
 * Webservice to show git status.
 *
 */
@Path("/release")
@Component
public class ReleaseResource {
	private ObjectMapper _mapper = new ObjectMapper();
	private GitRepositoryState gitState = null;
	
	public ReleaseResource() {
		
	}
	
	@GET
	@Produces("application/json")
	public Response getDetails() throws Exception {
		if (gitState == null) {
			gitState = new GitRepositoryHelper().getGitRepositoryState();
		}
		return Response.ok(_mapper.writeValueAsString(gitState)).build();
	}
}
