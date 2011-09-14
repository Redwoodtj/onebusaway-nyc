/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycVehicleStatusBean;
import org.onebusaway.nyc.transit_data.model.UtsRecordBean;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.model.VehicleLocationManagementRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class VehicleTrackingManagementServiceImpl implements
    VehicleTrackingManagementService {

  private static Logger _log = LoggerFactory.getLogger(VehicleTrackingManagementServiceImpl.class);

  private double _vehicleOffRouteDistanceThreshold = 150;

  private int _vehicleStalledTimeThreshold = 2 * 60;

  private VehicleLocationInferenceService _vehicleLocationService;

  private DestinationSignCodeService _dscService;
  
  private File _configPath;

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @Autowired
  public void setDestinationSignCodeService(DestinationSignCodeService dscService) {
	  _dscService = dscService;
  }

  public void setConfigPath(File configPath) {
    _configPath = configPath;
  }

  @PostConstruct
  public void setup() {

    if (_configPath == null || !_configPath.exists())
      return;

    try {
      Properties p = new Properties();
      FileReader in = new FileReader(_configPath);
      p.load(in);
      in.close();

      if (p.containsKey("noProgressTimeout"))
        _vehicleStalledTimeThreshold = Integer.parseInt(p.getProperty("noProgressTimeout"));

      if (p.containsKey("offRouteDistance"))
        _vehicleOffRouteDistanceThreshold = Double.parseDouble("offRouteDistance");

    } catch (IOException ex) {
      _log.warn("error loading configuration properties from " + _configPath,
          ex);
    }
  }

  /****
   * {@link VehicleTrackingManagementService} Interface
   ****/

  // FIXME: PILOT HACK
  @Override
  public String getDefaultAgencyId() {
	return "MTA NYCT";
  }

  @Override
  public double getVehicleOffRouteDistanceThreshold() {
    return _vehicleOffRouteDistanceThreshold;
  }

  @Override
  public int getVehicleStalledTimeThreshold() {
    return _vehicleStalledTimeThreshold;
  }

  @Override
  public void setVehicleOffRouteDistanceThreshold(
      double vehicleOffRouteDistanceThreshold) {
    _vehicleOffRouteDistanceThreshold = vehicleOffRouteDistanceThreshold;
  }

  @Override
  public void setVehicleStalledTimeThreshold(int vehicleStalledTimeThreshold) {
    _vehicleStalledTimeThreshold = vehicleStalledTimeThreshold;
  }

  @Override
  public void setVehicleStatus(String vehicleId, boolean status) {
	AgencyAndId vid = new AgencyAndId(getDefaultAgencyId(), vehicleId);
    _vehicleLocationService.setVehicleStatus(vid, status);
  }

  @Override
  public void resetVehicleTrackingForVehicleId(String vehicleId) {
	AgencyAndId vid = new AgencyAndId(getDefaultAgencyId(), vehicleId);
    _vehicleLocationService.resetVehicleLocation(vid);
  }

  @Override
  public List<NycVehicleStatusBean> getAllVehicleStatuses() {
    List<VehicleLocationManagementRecord> records = _vehicleLocationService.getVehicleLocationManagementRecords();
    List<NycVehicleStatusBean> beans = new ArrayList<NycVehicleStatusBean>();
    for (VehicleLocationManagementRecord record : records)
      beans.add(getManagementRecordAsStatus(record));
    return beans;
  }

  @Override
  public NycVehicleStatusBean getVehicleStatusForVehicleId(String vehicleId) {
	AgencyAndId vid = new AgencyAndId(getDefaultAgencyId(), vehicleId);
    VehicleLocationManagementRecord record = 
    		_vehicleLocationService.getVehicleLocationManagementRecordForVehicle(vid);
    if (record == null)
      return null;
    return getManagementRecordAsStatus(record);
  }

  @Override
  public boolean isOutOfServiceDestinationSignCode(String destinationSignCode) {
	  return _dscService.isOutOfServiceDestinationSignCode(destinationSignCode);
  }
  
  @Override
  public boolean isUnknownDestinationSignCode(String destinationSignCode) {
	  return _dscService.isUnknownDestinationSignCode(destinationSignCode);	  
  }
  
  // FIXME: PILOT HACK
  @Override
  public List<UtsRecordBean> getCurrentUTSRecordsForDepot(String depotId) {
	  return new ArrayList<UtsRecordBean>();
  }
  
  /****
   * Private Methods
   ****/

  private NycVehicleStatusBean getManagementRecordAsStatus(
      VehicleLocationManagementRecord record) {

    if (record == null)
      return null;
    NycVehicleStatusBean bean = new NycVehicleStatusBean();
    bean.setVehicleId(AgencyAndIdLibrary.convertToString(record.getVehicleId()));
    bean.setEnabled(record.isEnabled());
    bean.setLastUpdateTime(record.getLastUpdateTime());
    bean.setLastGpsTime(record.getLastGpsTime());
    bean.setLastGpsLat(record.getLastGpsLat());
    bean.setLastGpsLon(record.getLastGpsLon());
    if (record.getPhase() != null)
      bean.setPhase(record.getPhase().toLabel());
    bean.setStatus(record.getStatus());
    bean.setMostRecentDestinationSignCode(record.getMostRecentDestinationSignCode());
    bean.setInferredDestinationSignCode(record.getInferredDestinationSignCode());
    return bean;
  }

}
