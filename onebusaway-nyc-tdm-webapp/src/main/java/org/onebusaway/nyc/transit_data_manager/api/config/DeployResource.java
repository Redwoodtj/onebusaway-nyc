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

package org.onebusaway.nyc.transit_data_manager.api.config;

import org.onebusaway.nyc.transit_data_manager.bundle.model.ConfigDeployStatus;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDeployer;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/tdm/config")
@Component
@Scope("singleton")
/**
 * Deploy configuration files staged in S3 to appropriate places on the TDM server. 
 *
 */
public class DeployResource {

  private static final String DEFAULT_CONFIG_DIRECTORY = "";
  private static final String DEPOT_PATH = "depot_id_map";
  private static final String DSC_PATH = "destination_sign_codes";
  private static Logger _log = LoggerFactory.getLogger(DeployResource.class);

  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private ConfigurationDeployer _configurationDeployer;
  private ExecutorService _executorService = null;
  private Map<String, ConfigDeployStatus> _deployMap = new HashMap<String, ConfigDeployStatus>();
  private Integer jobCounter = 0;
  
  @PostConstruct
  public void setup() {
      _executorService = Executors.newFixedThreadPool(1);
  }

  public ConfigDeployStatus lookupDeployRequest(String id) {
    return _deployMap.get(id);
  }

  @Path("/deploy/list/depot/{environment}")
  @GET
  /**
   * list the depot id maps that are on S3, potentials to be deployed.
   */
  public Response listDepots(@PathParam("environment") String environment) {
    String s3Path = environment + "/" + DEPOT_PATH;
    List<String> list = _configurationDeployer.listFiles(s3Path);
    try {
      // serialize the status object and send to client -- it contains an id for querying
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(jsonGenerator, list);
      return Response.ok(sw.toString()).build();
    } catch (Exception e) {
      _log.error("exception serializing response:", e);
    }
    return Response.serverError().build();
  }

  @Path("/deploy/list/dsc/{environment}")
  @GET
  /**
   * list the destination sign code CSV files that are on S3, potentials to be deployed.
   */
  public Response listDscs(@PathParam("environment") String environment) {
    String s3Path = environment + "/" + DSC_PATH;
    List<String> list = _configurationDeployer.listFiles(s3Path);
    try {
      // serialize the status object and send to client -- it contains an id for querying
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(jsonGenerator, list);
      return Response.ok(sw.toString()).build();
    } catch (Exception e) {
      _log.error("exception serializing response:", e);
    }
    return Response.serverError().build();
  }

  
  @Path("/deploy/status/{id}/list")
  @GET
  /**
   * query the status of a requested configuration deployment
   * @param id the id of a ConfigDeployStatus
   * @return a serialized version of the requested ConfigDeployStatus, null otherwise
   */
  public Response deployStatus(@PathParam("id") String id) {
    _log.info("Starting deployStatus(" + id+ ")...");
    ConfigDeployStatus status = this.lookupDeployRequest(id);
    try {
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(jsonGenerator, status);
      return Response.ok(sw.toString()).build();
    } catch (Exception e) {
      _log.error("exception serializing response:", e);
    }
    return Response.serverError().build();
    }


  @Path("/deploy/from/{environment}")
  @GET
  public Response deploy(@PathParam("environment") String environment) {
    String configDir = getConfigDirectory();
    String s3Path = null;
    if (configDir == null || configDir.length() == 0) {
      s3Path = environment + File.separator;
    } else {
      s3Path = getConfigDirectory() + File.separator + environment + File.separator;
    }
    ConfigDeployStatus status = new ConfigDeployStatus();
    status.setId(getNextId());
    _deployMap.put(status.getId(), status);
    _executorService.execute(new DeployThread(s3Path, status));
    _log.info("deploy request complete");
    
        try {
      // serialize the status object and send to client -- it contains an id for querying
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(jsonGenerator, status);
      return Response.ok(sw.toString()).build();
    } catch (Exception e) {
      _log.error("exception serializing response:", e);
    }
    return Response.serverError().build();

  }

    /**
   * Trivial implementation of creating unique Ids. Security is not a
   * requirement here.
   */
  private String getNextId() {
    return "" + inc();
  }

  private Integer inc() {
    synchronized (jobCounter) {
      jobCounter++;
    }
    return jobCounter;
  }

  private String getConfigDirectory() {
    if (configurationService != null) {
      try {
        return configurationService.getConfigurationValueAsString("admin.config_directory", DEFAULT_CONFIG_DIRECTORY);
      } catch (RemoteConnectFailureException e){
        _log.error("default bundle dir lookup failed:", e);
        return DEFAULT_CONFIG_DIRECTORY;
      }
    }
    return DEFAULT_CONFIG_DIRECTORY;
  }
  
  /**
   * Thread to perform the actual deployment of config files.  Downloading from S3 and
   * staging in appropriate directories
   *
   */
  private class DeployThread implements Runnable {
    private String s3Path;
    private ConfigDeployStatus status;
    public DeployThread(String s3Path, ConfigDeployStatus status){
      this.s3Path = s3Path;
      this.status = status;
    }
    
    @Override
    public void run() {
      _configurationDeployer.deploy(status, s3Path);
    }
  }

}
