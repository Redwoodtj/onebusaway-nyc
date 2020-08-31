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

package org.onebusaway.nyc.transit_data_federation.impl.vtw;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.impl.vtw.VehiclePulloutServiceImpl;
import org.onebusaway.nyc.util.impl.vtw.PullOutApiLibrary;

import tcip_final_4_0_0.CPTTransitFacilityIden;
import tcip_final_4_0_0.CPTVehicleIden;
import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SCHBlockIden;
import tcip_final_4_0_0.SCHPullInOutInfo;
import tcip_final_4_0_0.SchPullOutList.PullOuts;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


@RunWith(MockitoJUnitRunner.class)
public class VehiclePulloutServiceImplTest {

  @Mock
  private PullOutApiLibrary mockApiLibrary;

  @InjectMocks
  private VehiclePulloutServiceImpl service;
  
  @Before
  public void prepare(){
	  MockitoAnnotations.initMocks(this);
	  this.service.setupJaxbContext();
  }

  @Test
  public void testVehiclePulloutEmptyList() throws Exception {
    ObaSchPullOutList o = new ObaSchPullOutList();
    o.setErrorCode("1");
    o.setErrorDescription("No description here");

    String xml = service.getAsXml(o);
    
    when(mockApiLibrary.getContentsOfUrlAsString("uts","active","tcip","")).thenReturn(xml);
        
    service.refreshData();

    AgencyAndId vehicle = new AgencyAndId("MTA", "7788");
    SCHPullInOutInfo pullouts = service.getVehiclePullout(vehicle);
    assertNull(pullouts);
  }

  @Test
  public void testVehiclePulloutNonEmptyList() throws Exception {

    ObaSchPullOutList o = new ObaSchPullOutList();
    
    PullOuts pullouts = new PullOuts();
    
    o.setPullOuts(pullouts);
    
    List<SCHPullInOutInfo> list = pullouts.getPullOut();
    
    SCHPullInOutInfo pullinoutinfo = new SCHPullInOutInfo();
    
    list.add(pullinoutinfo);
    
    CPTVehicleIden vehicle = new CPTVehicleIden();
    pullinoutinfo.setVehicle(vehicle);
    vehicle.setAgdesig("MTA");
    vehicle.setId("7788");

    CPTTransitFacilityIden garage = new CPTTransitFacilityIden();
    pullinoutinfo.setGarage(garage);
    garage.setAgdesig("MTA");
    
    SCHBlockIden block = new SCHBlockIden();
    pullinoutinfo.setBlock(block);
    block.setId("test block id");
    
    String xml = service.getAsXml(o);

    when(mockApiLibrary.getContentsOfUrlAsString("uts","active","tcip","")).thenReturn(xml);
    
    service.refreshData();
    
    AgencyAndId lookupVehicle = new AgencyAndId("MTA", "7788");
    SCHPullInOutInfo resultPullouts = service.getVehiclePullout(lookupVehicle);
    assertNotNull(resultPullouts);
    assertEquals("MTA", resultPullouts.getVehicle().getAgdesig());
    assertEquals("7788", resultPullouts.getVehicle().getId());
    assertEquals("test block id", resultPullouts.getBlock().getId());
  }

  @Test
  public void testVehiclePulloutFromXml() throws Exception {
    Path path = Paths.get(getClass().getResource("vehicle_pipo.xml").getFile());
    String xml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    ObaSchPullOutList pullOutList = service.getFromXml(xml);

    Map<AgencyAndId, SCHPullInOutInfo> vehicleIdToPullouts = new HashMap<>();
    service.processVehiclePipoList(pullOutList, vehicleIdToPullouts);
    assertEquals(1, vehicleIdToPullouts.size());

    List<SCHPullInOutInfo> pullInOutInfoList =  new ArrayList<>(vehicleIdToPullouts.values());
    SCHPullInOutInfo pipoInfo = pullInOutInfoList.get(0);
    assertEquals(pipoInfo.getBlock().getId(), "MTABC_BPPC0-BP_C0-Weekday-10_5440668");
    assertEquals(pipoInfo.getGarage().getAgdesig(), "MTABC");
    assertEquals(pipoInfo.getVehicle().getId(), "3773");
    assertEquals(pipoInfo.getRun().getId(), "417");
  }

}
