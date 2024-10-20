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
package org.onebusaway.nyc.webapp.actions.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.api.model.RouteAtStop;
import org.onebusaway.nyc.webapp.actions.api.model.RouteDirection;
import org.onebusaway.nyc.webapp.actions.api.model.StopResult;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.StopMonitoringDeliveryStructure;

import javax.servlet.http.HttpServletRequest;

public class StopForIdAction extends OneBusAwayNYCActionSupport {
    
  private static final long serialVersionUID = 2L;

  @Autowired
  @Qualifier("NycRealtimeService")
  private RealtimeService _realtimeService;

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  HttpServletRequest _request;

  private ObjectMapper _mapper = new ObjectMapper();    

  private ServiceAlertsHelper _serviceAlertsHelper = new ServiceAlertsHelper();

  private Siri _response = null;

  private StopResult _result = null;
  
  private String _stopId = null;

  public void setStopId(String stopId) {
    _stopId = stopId;
  }
  
  @Override
  public String execute() {    
    if(_stopId == null) {
      return SUCCESS;
    }
    
    StopBean stop = _nycTransitDataService.getStop(_stopId);    

    if(stop == null) {
      return SUCCESS;
    }

    List<RouteAtStop> routesAtStop = new ArrayList<RouteAtStop>();
    for(RouteBean routeBean : stop.getRoutes()) {
      StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

      List<RouteDirection> routeDirections = new ArrayList<RouteDirection>();
      List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
      for (StopGroupingBean stopGroupingBean : stopGroupings) {
        for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
        	if (_nycTransitDataService.stopHasRevenueServiceOnRoute((routeBean.getAgency()!=null?routeBean.getAgency().getId():null),
	                    _stopId, routeBean.getId(), stopGroupBean.getId())) {
        	  
	          NameBean name = stopGroupBean.getName();
	          String type = name.getType();
	
	          if (!type.equals("destination"))
	            continue;
	        
	          // filter out route directions that don't stop at this stop
	          if(!stopGroupBean.getStopIds().contains(_stopId))
	            continue;
	
	          Boolean hasUpcomingScheduledService = 
	        		  _nycTransitDataService.stopHasUpcomingScheduledService((routeBean.getAgency()!=null?routeBean.getAgency().getId():null), System.currentTimeMillis(), stop.getId(), 
	        				  routeBean.getId(), stopGroupBean.getId());
	
	          // if there are buses on route, always have "scheduled service"
	          Boolean routeHasVehiclesInService = 
	        		  _realtimeService.getVehiclesInServiceForStopAndRoute(stop.getId(), routeBean.getId(), System.currentTimeMillis());
	
	          if(routeHasVehiclesInService) {
	        	  hasUpcomingScheduledService = true;
	          }
	          
	          routeDirections.add(new RouteDirection(stopGroupBean, null, null, hasUpcomingScheduledService));
	        }
        }
      }

      RouteAtStop routeAtStop = new RouteAtStop(routeBean, routeDirections);
      routesAtStop.add(routeAtStop);
    }

    _result = new StopResult(stop, routesAtStop);

    Boolean showApc = _realtimeService.showApc();
    Boolean showRawApc = _realtimeService.showRawApc();

    List<MonitoredStopVisitStructure> visits =
        _realtimeService.getMonitoredStopVisitsForStop(_stopId, 0, System.currentTimeMillis(),
                showApc, showRawApc, false);

    _response = generateSiriResponse(visits, AgencyAndIdLibrary.convertFromString(_stopId));
    
    return SUCCESS;
  }   
  
  private Siri generateSiriResponse(List<MonitoredStopVisitStructure> visits, AgencyAndId stopId) {
    
    List<AgencyAndId> stopIds = new ArrayList<AgencyAndId>();
    if (stopId != null) stopIds.add(stopId);
    
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    try {
      StopMonitoringDeliveryStructure stopMonitoringDelivery = new StopMonitoringDeliveryStructure();
      stopMonitoringDelivery.setResponseTimestamp(new Date(getTime()));
      
      Calendar gregorianCalendar = new GregorianCalendar();
      gregorianCalendar.setTimeInMillis(getTime());
      gregorianCalendar.add(Calendar.MINUTE, 1);
      stopMonitoringDelivery.setValidUntil(gregorianCalendar.getTime());
      
      stopMonitoringDelivery.getMonitoredStopVisit().addAll(visits);

      serviceDelivery.setResponseTimestamp(new Date(getTime()));
      serviceDelivery.getStopMonitoringDelivery().add(stopMonitoringDelivery);

      _serviceAlertsHelper.addSituationExchangeToSiriForStops(serviceDelivery, visits, _nycTransitDataService, stopIds);
      _serviceAlertsHelper.addGlobalServiceAlertsToServiceDelivery(serviceDelivery, _realtimeService);
    } catch (RuntimeException e) {
      throw e;
    }

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);
    
    return siri;
  }
  
  /** 
   * VIEW METHODS
   */
  public String getStopMonitoring() {
    try {
      return _realtimeService.getSiriJsonSerializer().getJson(_response, null);
    } catch(Exception e) {
      return e.getMessage();
    }
  }
  
  public String getStopMetadata() throws Exception {
    return _mapper.writeValueAsString(_result);
  }

}
