/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.controller;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import org.onebusaway.nyc.gtfsrt.service.FeedMessageService;
import org.onebusaway.nyc.transit_data_manager.siri.NycSiriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class GtfsRealtimeController {

    private static long DEFAULT_ALERT_REFRESH_INTERVAL = 60 * 1000;

    private FeedMessageService _vehicleUpdateService;
    private FeedMessageService _tripUpdateService;
    private FeedMessageService _serviceAlertService;
    @Autowired
    private NycSiriService _siriService;

    private long lastAlertInvocation = System.currentTimeMillis();

    private long alertRefreshInMillis = DEFAULT_ALERT_REFRESH_INTERVAL;
    public void setAlertRefreshInMillis(long interval) {
        this.alertRefreshInMillis = interval;
    }
    private long getAlertRefreshIntervalInMillis() {
        return alertRefreshInMillis;
    }



    @Autowired
    @Qualifier("vehicleUpdateServiceImpl")
    public void setVehicleUpdateService(FeedMessageService vehicleUpdateService) {
        _vehicleUpdateService = vehicleUpdateService;
    }

    @Autowired
    @Qualifier("tripUpdateServiceImpl")
    public void setTripUpdateService(FeedMessageService tripUpdateService) {
        _tripUpdateService = tripUpdateService;
    }

    @Autowired
    @Qualifier("serviceAlertServiceImpl")
    public void setServiceAlertService(FeedMessageService serviceAlertService) {
        _serviceAlertService = serviceAlertService;
    }

    @RequestMapping(value = "/vehiclePositions")
    public void getVehiclePositions(HttpServletResponse response,
                                    @RequestParam(value = "debug", defaultValue = "false") boolean debug,
                                    @RequestParam(value = "time", required = false) Long time)
            throws IOException {
        FeedMessage msg = _vehicleUpdateService.getFeedMessage(time);
        writeFeed(response, msg, debug);
    }

    @RequestMapping(value = "/tripUpdates")
    public void getTripUpdates(HttpServletResponse response,
                                    @RequestParam(value = "debug", defaultValue = "false") boolean debug,
                                    @RequestParam(value = "time", required = false) Long time)
            throws IOException {
        FeedMessage msg = _tripUpdateService.getFeedMessage(time);
        writeFeed(response, msg, debug);
    }

    @RequestMapping(value = "/alerts")
    public void getAlerts(HttpServletResponse response,
                          @RequestParam(value = "debug", defaultValue = "false") boolean debug,
                          @RequestParam(value = "time", required = false) Long time)
        throws IOException {
        forceReloadOfAlerts();
        FeedMessage msg = _serviceAlertService.getFeedMessage(time);
        writeFeed(response, msg, debug);
    }

    // SIRI subscription doesn't work for gtfsrt -- and we wouldn't want
    // that endpoint public regardless.  Manually reload alerts on a
    // configurable interval
    private synchronized void forceReloadOfAlerts() {
        if (System.currentTimeMillis() - lastAlertInvocation > getAlertRefreshIntervalInMillis()) {
            _siriService.setup();
            lastAlertInvocation = System.currentTimeMillis();
        }
    }

    private void writeFeed(HttpServletResponse response, FeedMessage msg, boolean debug)
        throws IOException {
        if (debug) {
            response.getWriter().print(msg.toString());
        } else {
            msg.writeTo(response.getOutputStream());
        }
    }

}
