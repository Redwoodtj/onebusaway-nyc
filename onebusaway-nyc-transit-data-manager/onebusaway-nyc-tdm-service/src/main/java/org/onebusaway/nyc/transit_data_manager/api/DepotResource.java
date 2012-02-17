package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotFileToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.VehicleFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.DepotsMessage;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.VehiclesMessage;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.DepotAssignmentsSoapDownloadsFilePicker;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.MostRecentFilePicker;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CPTVehicleIden;

@Path("/depot")
@Component
@Scope("request")
public class DepotResource {

  public DepotResource() throws IOException {
    mostRecentPicker = new DepotAssignmentsSoapDownloadsFilePicker(System.getProperty("tdm.depotAssignsDownloadDir"));
    
    try {
      depotIdTranslator = new DepotIdTranslator(new File(System.getProperty("tdm.depotIdTranslationFile")));
    } catch (IOException e) {
      // Set depotIdTranslator to null and otherwise do nothing.
      // Everything works fine without the depot id translator.
      depotIdTranslator = null;
    }
  }
  
  private static Logger _log = LoggerFactory.getLogger(DepotResource.class);
  
  @Autowired
  private JsonTool jsonTool;
  @Autowired(required=false)
  private DepotIdTranslator depotIdTranslator = null;
  
  private MostRecentFilePicker mostRecentPicker;
  
  public void setJsonTool(JsonTool jsonTool) {
    this.jsonTool = jsonTool;
  }
  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;
  }

  @Path("/list")
  @GET
  @Produces("application/json")
  public String getDepotList() {
    _log.info("Starting getDepotList.");
    
    VehicleDepotData data = getVehicleDepotDataObject();
    
    List<String> allDepotNames = data.getAllDepotNames();

    DepotsMessage message = new DepotsMessage();
    message.setDepots(allDepotNames);
    message.setStatus("OK");
    
    String outputJson;
    try {
      StringWriter stringWriter = new StringWriter();
      
      jsonTool.writeJson(stringWriter, message);
      
      outputJson = stringWriter.toString();
      
      stringWriter.close();
    } catch (IOException e) {
      // This is unlikely.
      _log.info("Exception writing json output at DepotResource.getDepotList.");
      _log.debug(e.getMessage());
      
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    
    _log.info("getDepotList returning JSON output.");
    return outputJson;
  }

  @Path("/{depotName}/vehicles/list")
  @GET
  @Produces("application/json")
  public String getDepotAssignments(@PathParam("depotName") String depotName)
      throws FileNotFoundException {
    
    _log.info("Starting getDepotAssignments");

    VehicleDepotData data = getVehicleDepotDataObject();
    
    // Then I need to get the data for the input depot
    List<CPTVehicleIden> depotVehicles = data.getVehiclesByDepotNameStr(depotName);

    // Now convert the data to my json model.
    ModelCounterpartConverter<CPTVehicleIden, Vehicle> toJsonModelConv = new VehicleFromTcip();

    List<Vehicle> depotVehiclesJson = new ArrayList<Vehicle>();

    // now iterate through all the vehicles at that depot, converting each
    // vehicle to its json model representation.
    Iterator<CPTVehicleIden> vehIt = depotVehicles.iterator();
    while (vehIt.hasNext()) {
      depotVehiclesJson.add(toJsonModelConv.convert(vehIt.next()));
    }

    // Now add it to a message object
    VehiclesMessage message = new VehiclesMessage();
    message.setVehicles(depotVehiclesJson);
    message.setStatus("OK");

    StringWriter writer = new StringWriter();
    String output = null;
    try {
      jsonTool.writeJson(writer, message);
      output = writer.toString();
      writer.close();
    } catch (IOException e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
   
    _log.info("getDepotAssignments returning json output.");
    
    return output;

  }

  private VehicleDepotData getVehicleDepotDataObject() throws WebApplicationException {
    File inputFile = mostRecentPicker.getMostRecentSourceFile();

    _log.debug("Getting VehicleDepotData object in getVehicleDepotDataObject from " + inputFile.getPath());
    
    VehicleDepotData resultData = null;

    MtaBusDepotFileToDataCreator process;
    try {
      process = new MtaBusDepotFileToDataCreator(
          inputFile);
      
      if (depotIdTranslator == null) {
        _log.info("Depot ID translation has not been enabled properly. Depot ids will not be translated.");
      } else {
        _log.info("Using depot ID translation.");
      }
      process.setDepotIdTranslator(depotIdTranslator);
      
      resultData = process.generateDataObject();
    } catch (IOException e) {
      _log.info("Could not create data object from " + inputFile.getPath());
      _log.info(e.getMessage());
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }

    _log.debug("Returning VehicleDepotData object in getVehicleDepotDataObject.");
    return resultData;
  }

}
