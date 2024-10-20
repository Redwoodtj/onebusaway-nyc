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

package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringJoiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * This class represents the combination of an observation and a BlockState.
 * Specifically, it contains information about the BlockState that is
 * conditional on when it was observed.
 * 
 * @author bwillard
 * 
 */
public final class BlockStateObservation implements
    Comparable<BlockStateObservation> {
  final private BlockState _blockState;

  private final Boolean _isOpAssigned;

  private final Boolean _isRunReported;

  private final boolean _isAtPotentialLayoverSpot;

  private final boolean _isSnapped;

  /*
   * Schedule deviation in minutes
   */
  private final double _scheduleDeviation;

  private final Observation _obs;

  private final boolean _isRunFormal;

  private final boolean _isOnTrip;

  public BlockStateObservation(BlockStateObservation state, Observation obs) {
    final String runId = state.getBlockState().getRunId();
    _blockState = state._blockState;
    _isOpAssigned = obs.getOpAssignedRunId() != null
        ? obs.getOpAssignedRunId().equals(runId) : null;
    _isRunReported = ((runId != null) && (obs.getBestFuzzyRunIds() != null) && (obs.getFuzzyMatchDistance() != null))
        ? obs.getBestFuzzyRunIds().contains(runId) : null;
    _isRunFormal = decideIfRunIsFormal(obs, getInferredBlockId(state.getBlockState()));
    _isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(_blockState, obs);
    _isSnapped = state._isSnapped;
    _obs = obs;
    _isOnTrip = state._isOnTrip;
    _scheduleDeviation = computeScheduleDeviation(obs, _blockState);
    
    /*
     * We now recompute these, since some of this info could change simply with time.
     */
//    this(state._blockState, state._isRunReported, state._isOpAssigned,
//        state._isRunFormal, state._isAtPotentialLayoverSpot, state._isSnapped,
//        obs, state._isOnTrip);
  }

  public BlockStateObservation(BlockState blockState, Observation obs, boolean isSnapped) {
    Preconditions.checkNotNull(obs);
    _blockState = Preconditions.checkNotNull(blockState);

    final String runId = blockState.getRunId();
    _isOpAssigned = obs.getOpAssignedRunId() != null
        ? obs.getOpAssignedRunId().equals(runId) : null;
    _isRunReported = ((runId != null) && (obs.getBestFuzzyRunIds() != null) && (obs.getFuzzyMatchDistance() != null))
        ? obs.getBestFuzzyRunIds().contains(runId) : null;

    _isRunFormal = decideIfRunIsFormal(obs, getInferredBlockId(blockState));
    _isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(_blockState, obs);;
    _isSnapped = isSnapped;
    _scheduleDeviation = computeScheduleDeviation(obs, blockState);
    _obs = obs;
    _isOnTrip = JourneyStateTransitionModel.isLocationOnATrip(blockState);
    

  }

  private String getInferredBlockId(BlockState blockState) {
    final BlockInstance blockInstance = blockState.getBlockInstance();
    final BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    final BlockEntry block = blockConfig.getBlock();

    return AgencyAndIdLibrary.convertToString(block.getId());
  }

  private boolean decideIfRunIsFormal(Observation obs, String inferredBlockId) {
    String assignedBlockId = obs.getAssignedBlockId();
    return _isOpAssigned == Boolean.TRUE
        || ((_isRunReported == Boolean.TRUE && obs.getFuzzyMatchDistance() == 0) ? true : false)
        || (StringUtils.equals(assignedBlockId, inferredBlockId));
  }

  /**
   * Computes the schedule deviation in minutes (obs.time - sched.time).
   * Note: for states that haven't started, or that have ended, the value returned is zero.
   * 
   * @param obs
   * @param blockState
   * @return
   */
  public static double computeScheduleDeviation(Observation obs,
      BlockState blockState) {

    final double schedDev = ((obs.getTime() - blockState.getBlockInstance().getServiceDate()) / 1000d - blockState.getBlockLocation().getScheduledTime()) / 60d;
    final double dab = blockState.getBlockLocation().getDistanceAlongBlock();
    if ((dab <= 0d && schedDev <= 0d)
        || (dab >= blockState.getBlockInstance().getBlock().getTotalBlockDistance() && schedDev >= 0d))
      return 0d;
    else
      return schedDev;
  }

  public BlockState getBlockState() {
    return _blockState;
  }

  public Boolean getRunReported() {
    return _isRunReported;
  }

  public Boolean getOpAssigned() {
    return _isOpAssigned;
  }

  @Override
  public int compareTo(BlockStateObservation rightBs) {

    if (this == rightBs)
      return 0;

    final int res = ComparisonChain.start().compare(this._isRunFormal,
        rightBs.isRunFormal(), Ordering.natural().nullsLast()).compare(
        this._isRunReported, rightBs.getRunReported(),
        Ordering.natural().nullsLast()).compare(this._isOpAssigned,
        rightBs.getOpAssigned(), Ordering.natural().nullsLast()).compare(
        this._blockState, rightBs._blockState).result();
    return res;
  }

  @Override
  public String toString() {
    return new StringJoiner("BlockStateObservation").add(String.valueOf(_blockState)).add(
        "isSnapped=" + _isSnapped).add("isOpAssigned=" + _isOpAssigned).add(
        "isRunReported=" + _isRunReported).add("isRunFormal=" + _isRunFormal).add(
        "schedDev=" + _scheduleDeviation).toString();
  }

  public boolean isAtPotentialLayoverSpot() {
    return _isAtPotentialLayoverSpot;
  }

  public boolean isSnapped() {
    return _isSnapped;
  }

  public double getScheduleDeviation() {
    return _scheduleDeviation;
  }

  public Observation getObs() {
    return _obs;
  }

  public boolean isRunFormal() {
    return _isRunFormal;
  }

  int _hashCode = 0;

  @Override
  public int hashCode() {
    if (_hashCode != 0)
      return _hashCode;

    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((_blockState == null) ? 0 : _blockState.hashCode());
    result = prime * result + (_isAtPotentialLayoverSpot ? 1231 : 1237);
    result = prime * result
        + ((_isOpAssigned == null) ? 0 : _isOpAssigned.hashCode());
    result = prime * result + (_isRunFormal ? 1231 : 1237);
    result = prime * result
        + ((_isRunReported == null) ? 0 : _isRunReported.hashCode());
    result = prime * result + (_isSnapped ? 1231 : 1237);
    result = prime * result + ((_obs == null) ? 0 : _obs.hashCode());
    long temp;
    temp = Double.doubleToLongBits(_scheduleDeviation);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    _hashCode = result;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof BlockStateObservation)) {
      return false;
    }
    final BlockStateObservation other = (BlockStateObservation) obj;
    if (_blockState == null) {
      if (other._blockState != null) {
        return false;
      }
    } else if (!_blockState.equals(other._blockState)) {
      return false;
    }
    if (_isAtPotentialLayoverSpot != other._isAtPotentialLayoverSpot) {
      return false;
    }
    if (_isOpAssigned == null) {
      if (other._isOpAssigned != null) {
        return false;
      }
    } else if (!_isOpAssigned.equals(other._isOpAssigned)) {
      return false;
    }
    if (_isRunFormal != other._isRunFormal) {
      return false;
    }
    if (_isRunReported == null) {
      if (other._isRunReported != null) {
        return false;
      }
    } else if (!_isRunReported.equals(other._isRunReported)) {
      return false;
    }
    if (_isSnapped != other._isSnapped) {
      return false;
    }
    if (_obs == null) {
      if (other._obs != null) {
        return false;
      }
    } else if (!_obs.equals(other._obs)) {
      return false;
    }
    if (Double.doubleToLongBits(_scheduleDeviation) != Double.doubleToLongBits(other._scheduleDeviation)) {
      return false;
    }
    return true;
  }

  public boolean isOnTrip() {
    return _isOnTrip;
  }
}
