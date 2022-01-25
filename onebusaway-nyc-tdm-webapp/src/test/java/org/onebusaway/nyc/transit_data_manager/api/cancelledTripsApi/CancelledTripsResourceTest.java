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
package org.onebusaway.nyc.transit_data_manager.api.cancelledTripsApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItem;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CancelledTripsResourceTest {

    /**
     * unit testing for canceled trips integrator
     *
     * @author caylasavitzky
     *
     */

    @InjectMocks
    PersonalConfigurationDatastoreInterface datastoreInterface = new PersonalConfigurationDatastoreInterface();

    @InjectMocks
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

    @Test
    public void testCapiOutToNYCCancelledTripBeans() throws Exception {

        MockitoAnnotations.initMocks(this);
        String capiData = "";
        capiData = "{\"impacted\":[{\"block\":\"MTABC_JKPA2-JK_A2-Weekday-01-SDon_6193636\",\"trip\":\"MTABC_32246617-JKPA2-JK_A2-Weekday-01-SDon\",\"status\":\"canceled\",\"timestamp\":1642743832000,\"scheduledPullOut\":\"2022-01-21T07:03:00\",\"humanReadableTimestamp\":\"2022-01-21T00:43:52\",\"serviceDate\":\"2022-01-21\",\"route\":\"Q9\",\"firstStopId\":\"MTA_550031\",\"firstStopDepartureTime\":\"07:23:00\",\"lastStopArrivalTime\":\"07:43:00\"},{\"block\":\"MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207\",\"trip\":\"MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207\",\"status\":\"canceled\",\"timestamp\":1642734418000,\"scheduledPullOut\":\"2022-01-21T07:23:00\",\"humanReadableTimestamp\":\"2022-01-20T22:06:58\",\"serviceDate\":\"2022-01-21\",\"route\":\"B41\",\"firstStopId\":\"MTA_303215\",\"firstStopDepartureTime\":\"07:29:00\",\"lastStopArrivalTime\":\"07:49:00\"},{\"block\":\"MTA NYCT_FP_A2-Weekday-SDon_E_FP_34740_Q54-721\",\"trip\":\"MTA NYCT_FP_A2-Weekday-SDon-058900_Q54_721\",\"status\":\"canceled\",\"timestamp\":1642734447000,\"scheduledPullOut\":\"2022-01-21T09:39:00\",\"humanReadableTimestamp\":\"2022-01-20T22:07:27\",\"serviceDate\":\"2022-01-21\",\"route\":\"Q54\",\"firstStopId\":\"MTA_308488\",\"firstStopDepartureTime\":\"09:49:00\",\"lastStopArrivalTime\":\"09:59:00\"}],\"timestamp\":\"2022-01-21T10:40:51\"}";
        //capiData = new String(this.getClass().getResourceAsStream("CAPI_202201241142.txt").readAllBytes());
        StringBuffer cancelledTripData = new StringBuffer();
        cancelledTripData.append(capiData);
        CancelledTripsResource resource = new CancelledTripsResource();
        resource.setupObjectMapper();
        resource.setCancelledTripsBeans(resource.makeCancelledTripBeansFromCapiOutput(cancelledTripData));

        List<NycCancelledTripBean> beans = readOutput((String) resource.getCancelledTripsList().getEntity());

        assertTrue(beans.size()==3);
        NycCancelledTripBean bean = beans.get(1);


        assertEquals("MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207",bean.getBlock());
        assertEquals("MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207",bean.getTrip());
        assertEquals("canceled",bean.getStatus());
        assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2022-01-21"),bean.getServiceDate());
        assertEquals("B41",bean.getRoute());
        assertEquals("MTA_303215",bean.getFirstStopId());
        assertEquals(new SimpleDateFormat("HH:mm:ss").parse("07:29:00").getTime(),bean.getFirstStopDepartureTime().getTime());
        assertEquals(new SimpleDateFormat("HH:mm:ss").parse("07:49:00").getTime(),bean.getLastStopArrivalTime().getTime());
        assertTrue(bean.getTimestamp()==Long.valueOf("1642734418000"));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        DateTime dateTime = new DateTime(format.parse("2022-01-21T07:23:00"),DateTimeZone.getDefault());
        assertEquals(dateTime,bean.getScheduledPullOut());
        dateTime = new DateTime(format.parse("2022-01-20T22:06:58"),DateTimeZone.getDefault());
        assertEquals(dateTime,bean.getHumanReadableTimestamp());
    }

    private List<NycCancelledTripBean> readOutput(String str) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.setTimeZone(Calendar.getInstance().getTimeZone());
        return mapper.readValue(str, new TypeReference<List<NycCancelledTripBean>>(){});
    }


    @Test
    public void testFailedDataCollectionToResponse() throws Exception {
        MockitoAnnotations.initMocks(this);

        CancelledTripsResource resource = new CancelledTripsResource();
        resource.setupObjectMapper();

        resource.setUrl("http://blookyisdsflijewnmvldinejkd.com");
        resource.update();
        Response response = resource.getCancelledTripsList();
        assertEquals(200,response.getStatus());
        assertEquals("null",response.getEntity());
    }

    @Test
    public void SetupTest() {
        MockitoAnnotations.initMocks(this);
        datastoreInterface.setConfigItemByComponentKey("cancelledTrips", "cancelledTrips.CAPIUrl","http://capi.dev.obanyc.com:8084/api/canceled-trips.json");
        CancelledTripsResource resource = new CancelledTripsResource();
        resource.setConfig(datastoreInterface);
        resource.setup();
        assertEquals(resource.getUrl(),datastoreInterface.getConfigItemByComponentKey("cancelledTrips", "cancelledTrips.CAPIUrl").getValue());
    }


    private class PersonalConfigurationDatastoreInterface implements ConfigurationDatastoreInterface {
        HashMap<String,HashMap<String,ConfigItem>> configs = new HashMap<String,HashMap<String,ConfigItem>>();

        @Override
        public List<ConfigItem> getCompleteSetConfigItems() {
            return null;
        }

        @Override
        public List<ConfigItem> getConfigItemsForComponent(String component) {
            return null;
        }

        @Override
        public ConfigItem getConfigItemByComponentKey(String component, String key) {
            return configs.get(component).get(key);
        }

        public void setConfigItemByComponentKey(String component, String key, String configString) {
            ConfigItem config = new ConfigItem();
            config.setValue(configString);
            if(!configs.containsKey(component)) {
                configs.put(component,new HashMap<String, ConfigItem>());
            }
            configs.get(component).put(key,config);
        }

        @Override
        public void setConfigItemByComponentKey(String component, String key, ConfigItem config) {
            if(!configs.containsKey(component)) {
                configs.put(component,new HashMap<String, ConfigItem>());
            }
            configs.get(component).put(key,config);
        }

        @Override
        public boolean getHasComponent(String component) {
            return false;
        }

        @Override
        public boolean getComponentHasKey(String component, String key) {
            return false;
        }

        @Override
        public ConfigItem deleteConfigItemByKey(String component, String key) {
            return null;
        }
    };

}
