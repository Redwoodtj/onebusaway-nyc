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

package org.onebusaway.nyc.transit_data_manager.siri;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.siri.support.SiriXmlSerializer;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import uk.org.siri.siri.ParticipantRefStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;

@Entity
@Table(name = "obanyc_servicealert_subscription")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class ServiceAlertSubscription implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
  @GenericGenerator(name = "native", strategy = "native")
  @AccessType("property")
  private Integer id;

  @Column(name = "address", length = 512)
  private String address;

  @Column(name = "identifier", length = 64)
  private String subscriptionIdentifier;

  @Column(name = "ref", length = 64)
  private String subscriptionRef;
  
  @Column(name = "consecutive_failures")
  private Integer consecutiveFailures;
  
  @Column(name = "created_at")
  private Date createdAt;
  
  transient private WebResourceWrapper webResourceWrapper;
  
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getSubscriptionIdentifier() {
    return subscriptionIdentifier;
  }

  public void setSubscriptionIdentifier(String subscriptionIdentifier) {
    this.subscriptionIdentifier = subscriptionIdentifier;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getSubscriptionRef() {
    return subscriptionRef;
  }

  public void setSubscriptionRef(String subscriptionRef) {
    this.subscriptionRef = subscriptionRef;    
  }

  public void send(Map<String, ServiceAlertBean> serviceAlerts, Collection<String> deletedIds, String environment) throws Exception {
    send(serviceAlerts.values(), deletedIds, environment);
  }

  public void send(Collection<ServiceAlertBean> collection,
      Collection<String> deletedIds, String environment) throws Exception {
    SiriXmlSerializer siriXmlSerializer = new SiriXmlSerializer();
    ServiceAlertsHelper h = new ServiceAlertsHelper();
    Siri s = new Siri();
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    ParticipantRefStructure producer = new ParticipantRefStructure();
    producer.setValue(environment);
    serviceDelivery.setProducerRef(producer);
    
    h.addSituationExchangeToServiceDelivery(serviceDelivery, collection);
    h.addClosedSituationExchangesToSiri(serviceDelivery, deletedIds);
    
    s.setServiceDelivery(serviceDelivery);
    String xml = siriXmlSerializer.getXml(s);
    
    getWebResourceWrapper().post(xml, getAddress());
  }

  public WebResourceWrapper getWebResourceWrapper() {
    if (webResourceWrapper == null)
      webResourceWrapper = new WebResourceWrapper();
    return webResourceWrapper;
  }

  public void setWebResourceWrapper(WebResourceWrapper webResourceWrapper) {
    this.webResourceWrapper = webResourceWrapper;
  }

  public Integer getConsecutiveFailures() {
    return consecutiveFailures;
  }

  public void setConsecutiveFailures(Integer consecutiveFailures) {
    this.consecutiveFailures = consecutiveFailures;
  }

  public void updateFrom(ServiceAlertSubscription s) {
    this.address = s.getAddress();
    this.consecutiveFailures = s.getConsecutiveFailures();
    this.createdAt = s.getCreatedAt();
    this.subscriptionIdentifier = s.getSubscriptionIdentifier();
    this.subscriptionRef = s.getSubscriptionRef();
  }
  
}
