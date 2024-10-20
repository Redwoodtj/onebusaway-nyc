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

package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterOperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.CrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPCrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

public class UtsCrewAssignsToDataCreator {

  private static Logger _log = LoggerFactory.getLogger(UtsCrewAssignsToDataCreator.class);

  private File inputFile;

  private DepotIdTranslator depotIdTranslator;

  public UtsCrewAssignsToDataCreator(File inputFile) {
    this.inputFile = inputFile;
  }

  public OperatorAssignmentData generateDataObject() throws FileNotFoundException  {

    _log.debug("Importing crew assignments from source data file using CSVCrewAssignsInputConverter.");
    CrewAssignsInputConverter inConv = new CSVCrewAssignsInputConverter(
        inputFile);

    List<MtaUtsCrewAssignment> crewAssignments = inConv.getCrewAssignments();

    _log.debug("Converting List<MtaUtsCrewAssignment> imported from source data to TCIP List<SCHOperatorAssignment> using TCIPCrewAssignmentsOutputConverter.");
    CrewAssignmentsOutputConverter converter = new TCIPCrewAssignmentsOutputConverter(
        crewAssignments);
    converter.setDepotIdTranslator(depotIdTranslator);
    
    List<SCHOperatorAssignment> opAssignments = converter.convertAssignments();

    // Set up a data object to interface with the tcip data.
    OperatorAssignmentData data = new ImporterOperatorAssignmentData(
        opAssignments); // a data object to represent the data.

    _log.debug("Returning OperatorAssignmentData object.");
    return data;
  }

  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;
    
  }
}
