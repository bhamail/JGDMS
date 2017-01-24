/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.river.test.spec.lookupservice.test_set00;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.StubNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lookup.ServiceEvent;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceRegistration;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.io.MarshalledInstance;
import net.jini.lookup.SafeServiceRegistrar;
import org.apache.river.api.io.AtomicMarshalledInstance;
import org.apache.river.qa.harness.QAConfig;
import org.apache.river.qa.harness.Test;
import org.apache.river.qa.harness.TestException;
import org.apache.river.test.spec.lookupservice.QATestRegistrar;
import org.apache.river.test.spec.lookupservice.QATestUtils;
import org.apache.river.test.spec.lookupservice.RemoteEventComparator;

/** This class is used to verify that after using templates containing only 
 *  a service ID to request notification of MATCH_MATCH|MATCH_NOMATCH events,
 *  upon the modification of existing attributes of the registered service
 *  items, the expected set of events will be generated by the lookup service.
 *
 *  @see org.apache.river.qa.harness.TestEnvironment
 *  @see org.apache.river.test.spec.lookupservice.QATestRegistrar
 *  @see org.apache.river.test.spec.lookupservice.QATestUtils
 */
public class NotifyOnAttrMod extends QATestRegistrar {

    /** Class which handles all events sent by the lookup service */
    private class Listener extends BasicListener implements RemoteEventListener {
        public Listener() throws RemoteException {
            super();
        }
        /** Method called remotely by lookup to handle the generated event. */
        public void notify(RemoteEvent ev) {
            ServiceEvent srvcEvnt = (ServiceEvent)ev;
            synchronized (NotifyOnAttrMod.this){
                evntVec.add(srvcEvnt);
            }
        }
    }

    protected final Collection<ServiceEvent> evntVec 
	    = new ConcurrentSkipListSet<ServiceEvent>(new RemoteEventComparator());

    private ServiceItem[] srvcItems ;
    private ServiceRegistration[] srvcRegs ;
    private ServiceTemplate[] srvcIDTmpl;
    private ServiceRegistrar proxy;

    private Entry[] attrEntries;
    private Entry[][] addAttrs;
    private Entry[][] modAttrs;
    private int attrsLen;

    /* Change to n if you want to skip the first n attributes in the set */
    private int attrStartIndx = 0; 

    /* The event handler for the services registered by this class */
    private static RemoteEventListener listener;

    /* The transition expected to be returned for all services */
    private static int EXPECTED_TRANSITION
                                     = ServiceRegistrar.TRANSITION_MATCH_MATCH;

    /** Performs actions necessary to prepare for execution of the 
     *  current QA test.
     *
     *  Creates the lookup service. Creates a single event handler to handle 
     *  all events generated by any of the registered service items. Loads 
     *  each of the attribute classes and creates one set of initialized 
     *  instances (non-null fields) of the loaded attributes. Instantiates
     *  a second set of initialized attributes; different from the first
     *  set of attributes. Loads and instantiates all service classes. 
     *  Registers each service class instance with the maximum service 
     *  lease duration. Retrieves the proxy to the lookup Registrar.  
     *  Creates an array of ServiceTemplates in which each element contains 
     *  the service ID of one of the registered service items. For each
     *  registered service, adds one of the attributes from the first
     *  set of attributes. For each registered service, registers an 
     *  event notification request, with the maximum lease duration, based 
     *  on the contents of the corresponding template and the appropriate 
     *  transition mask; along with a handback containing the service ID . 
     */
    public synchronized Test construct(QAConfig sysConfig) throws Exception {

        int i,j,k;
        ServiceID curSrvcID;
	EventRegistration[] evntRegs;
        int regTransitions =   ServiceRegistrar.TRANSITION_MATCH_MATCH
                             | ServiceRegistrar.TRANSITION_MATCH_NOMATCH;
	super.construct(sysConfig);

	logger.log(Level.FINE, "in setup() method.");

	listener = new Listener();
        ((BasicListener) listener).export();
        attrEntries = super.createAttributes(ATTR_CLASSES);
        addAttrs = new Entry[attrEntries.length][1];
        for (i=attrStartIndx,k=0;i<attrEntries.length;i++) {
	    addAttrs[k++][0] = attrEntries[i];
	}
        attrsLen = k;
        attrEntries = super.createModifiedAttributes(ATTR_CLASSES);
        modAttrs = new Entry[attrsLen][1];
        for (i=attrStartIndx,k=0;i<attrEntries.length;i++) {
	    modAttrs[k++][0] = attrEntries[i];
	}
	srvcItems = super.createServiceItems(TEST_SRVC_CLASSES);
	srvcRegs = super.registerAll();
	proxy = super.getProxy();

	srvcIDTmpl = new ServiceTemplate[srvcRegs.length];
        for (i=0;i<srvcRegs.length;i++) {
            curSrvcID = srvcRegs[i].getServiceID();
            srvcIDTmpl[i] = new ServiceTemplate(curSrvcID,null,null);
	}

	for(i=0; i<srvcRegs.length; i++) {
	    /* Not necessary to add each attribute to each service 
             * instance; just pick a different attribute set from
             * attrs[][] for each service class instance
             */
            j = i%attrsLen;
	    srvcRegs[i].addAttributes(addAttrs[j]);
	}
	evntRegs = new EventRegistration[srvcRegs.length];
        for (i=0;i<srvcRegs.length;i++) {
            curSrvcID = srvcRegs[i].getServiceID();
	    EventRegistration er;
	    er = ((SafeServiceRegistrar)proxy).notiFy(srvcIDTmpl[i],regTransitions,listener,
			      new AtomicMarshalledInstance(curSrvcID),
			      Long.MAX_VALUE);
	    evntRegs[i] = prepareEventRegistration(er);
	}
        return this;
    }

    /** Executes the current QA test.
     *
     *  For each service instance created during construct, modifies the attribute
     *  belonging to that service with the corresponding attribute from the
     *  second set of attributes created during construct. Waits a configured
     *  amount of time to allow for all of the events to be generated
     *  and collected. Determines if all of the expected -- as well as 
     *  no un-expected -- events have arrived. This test depends on the 
     *  semantics of event-notification. That is, it uses the fact that 
     *  if the events were generated for each service item in sequence 
     *  (which they were), then the events will arrive in that same sequence. 
     *  This means one can expect, when examining the event corresponding 
     *  to index i, that the service ID returned in the ServiceEvent should 
     *  correspond to the i_th service item registered. If it does not, then 
     *  failure is declared. Thus, this test does the following:  1. Verifies
     *  that the number of expected events equals the number of events that 
     *  have arrived. 2. Verifies that the transition returned in event[i] 
     *  corresponds to the expected transition (MATCH_MATCH). 3. Verifies 
     *  that the service ID returned in event[i] equals the service ID
     *  of the i_th service registered. 4. Verifies that the handback 
     *  returned in the i_th event object equals the service ID of the 
     *  i_th service.
     */
    public synchronized void run() throws Exception {
	logger.log(Level.FINE, "in run() method.");
	int i,j,k;
	int nExpectedEvnts = 0;

	logger.log(Level.FINE, "Modifying attributes ...");
	for(i=0; i<srvcRegs.length; i++) {
	    j = i%attrsLen;
	    srvcRegs[i].modifyAttributes(addAttrs[j],modAttrs[j]);
	    nExpectedEvnts++;
	}

	/* give the Listener a chance to collect all events */
	logger.log(Level.FINE, "Waiting " + deltaTListener + " milliseconds " +
			  "for all events to arrive.");
	try {
            QATestUtils.waitDeltaT(deltaTListener, this);
//	    Thread.sleep(deltaTListener);
	} catch (InterruptedException e) {
	}

	logger.log(Level.FINE, "Verifying expected event set.");
//        Collections.sort(evntVec, new RemoteEventComparator());
	QATestUtils.verifyEventVector(evntVec,nExpectedEvnts,
				      EXPECTED_TRANSITION,srvcRegs);
    }

    /** Performs cleanup actions necessary to achieve a graceful exit of 
     *  the current QA test.
     *
     *  Unexports the listener and then performs any remaining standard
     *  cleanup duties.
     */
    public synchronized void tearDown() {
	logger.log(Level.FINE, "in tearDown() method.");
	try {
	    unexportListener(listener, true);
	} finally {
	    super.tearDown();
	}
    }
}
