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

package org.onebusaway.nyc.transit_data.model;

/**
 * Serializable model bean for cancelled trips
 *
 * @author caylasavitzky
 *
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;

public class NycCancelledTripBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String block;
    private String trip;
    private String status;
    private long timestamp;
    private DateTime scheduledPullOut;
    private DateTime humanReadableTimestamp;
    private Date serviceDate;
    private String route;
    private String firstStopId;
    private Time firstStopDepartureTime;
    private Time lastStopArrivalTime;

//        block	"MTABC_JKPA2-JK_A2-Weekday-01-SDon_6193636"
//        trip	"MTABC_32246617-JKPA2-JK_A2-Weekday-01-SDon"
//        status	"canceled"
//        timestamp	1642743832000
//        scheduledPullOut	"2022-01-21T07:03:00"
//        humanReadableTimestamp	"2022-01-21T00:43:52"
//        serviceDate	"2022-01-21"
//        route	"Q9"
//        firstStopId	"MTA_550031"
//        firstStopDepartureTime	"07:23:00"

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setTrip(String trip) {
        this.trip = trip;
    }


    public void setBlock(String block) {
        this.block = block;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public void setFirstStopDepartureTime(Time firstStopDepartureTime) {
        this.firstStopDepartureTime = firstStopDepartureTime;
    }

    public void setLastStopArrivalTime(Time lastStopArrivalTime) {
        this.lastStopArrivalTime = lastStopArrivalTime;
    }

    public void setFirstStopId(String firstStopId) {
        this.firstStopId = firstStopId;
    }

    public void setHumanReadableTimestamp(DateTime humanReadableTimestamp) {
        this.humanReadableTimestamp = humanReadableTimestamp;
    }

    public void setScheduledPullOut(DateTime scheduledPullOut) {
        this.scheduledPullOut = scheduledPullOut;
    }

    public void setServiceDate(Date serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getBlock() {
        return block;
    }

    public String getTrip() {
        return trip;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public String getRoute() {
        return route;
    }

    public DateTime getHumanReadableTimestamp() {
        return humanReadableTimestamp;
    }

    public DateTime getScheduledPullOut() {
        return scheduledPullOut;
    }

    public Date getServiceDate() {
        return serviceDate;
    }

    public String getFirstStopId() {
        return firstStopId;
    }

    public Time getFirstStopDepartureTime() {
        return firstStopDepartureTime;
    }

    public Time getLastStopArrivalTime() {
        return lastStopArrivalTime;
    }
}
