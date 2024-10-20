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
package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.realtime.api.OccupancyStatus;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;

import java.io.Serializable;

/**
 * Represents the specific vehicle and some data about it that
 * is returned in a search.
 */
public class VehicleResult implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum OccupancyConfig {
    NONE,
    OCCUPANCY,
    LOAD_FACTOR,
    PASSENGER_COUNT,
    LOAD_FACTOR_PASSENGER_COUNT
  }

  private String timeOrDistance;
  private String vehicleId;
  private VehicleOccupancyRecord vor;
  private OccupancyConfig occupancyConfig;

  public VehicleResult(String timeOrDistance,
                       String vehicleId,
                       VehicleOccupancyRecord vor,
                       OccupancyConfig vorConfig) {
    this.timeOrDistance = timeOrDistance;
    this.vehicleId = vehicleId;
    this.vor = vor;
    this.occupancyConfig = vorConfig;
  }

  public String getTimeOrDistance() {
    return timeOrDistance;
  }

  public String getTimeOrDistanceAndOccupancy() {
    String occupancyStr = "";
    if (vor != null) {
      switch (occupancyConfig) {
        case NONE:
          occupancyStr = "";
        case OCCUPANCY:
          occupancyStr = getPresentableOccupancy(vor.getOccupancyStatus());
        case LOAD_FACTOR:
        case LOAD_FACTOR_PASSENGER_COUNT:
        case PASSENGER_COUNT:
          if (vor.getRawCount() != null)
            occupancyStr = ", ~" + vor.getRawCount() + " passengers";
      }
    }
    return timeOrDistance + occupancyStr;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public boolean hasApc() {
    if (vor == null) return false;
    switch (occupancyConfig) {
      case NONE:
        return false;
      case OCCUPANCY:
        return vor.getOccupancyStatus() != null;
      case LOAD_FACTOR:
      case LOAD_FACTOR_PASSENGER_COUNT:
      case PASSENGER_COUNT:
        return vor.getRawCount() != null;
    }
    if (vor.getRawCount() != null || vor.getOccupancyStatus() != null)
      return true;
    return false;
  }

  private String getPresentableOccupancy(OccupancyStatus occupancyStatus) {
    if (occupancyStatus == null) return null;
    String loadOccupancy = occupancyStatus.name();
    if(loadOccupancy.equals("SEATS_AVAILABLE") || loadOccupancy.equals("MANY_SEATS_AVAILABLE"))
      loadOccupancy = "seats available";
    else if (loadOccupancy.equals("FEW_SEATS_AVAILABLE"))
      loadOccupancy = "almost full";
    else if (loadOccupancy.equals("FULL"))
      loadOccupancy = "full";
    return loadOccupancy;
  }
}
