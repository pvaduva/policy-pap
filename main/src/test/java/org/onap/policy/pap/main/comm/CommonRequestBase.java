/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.comm;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.common.endpoints.listeners.TypedMessageListener;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.msgdata.RequestListener;
import org.onap.policy.pap.main.comm.msgdata.StateChangeReq;
import org.onap.policy.pap.main.comm.msgdata.UpdateReq;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pap.main.parameters.PdpStateChangeParameters;
import org.onap.policy.pap.main.parameters.PdpUpdateParameters;
import org.onap.policy.pap.main.parameters.RequestParams;

/**
 * Common base class for request tests.
 */
public class CommonRequestBase {
    protected static final String PDP1 = "pdp_1";
    protected static final String MY_REQ_NAME = "my-request";
    protected static final String DIFFERENT = "different-value";
    protected static final String MY_GROUP = "my-group";
    protected static final String MY_SUBGROUP = "my-subgroup";
    protected static final String MY_NAME = "my-name";
    protected static final PdpState MY_STATE = PdpState.SAFE;
    protected static final PdpState DIFF_STATE = PdpState.TERMINATED;
    protected static final int RETRIES = 1;

    protected Publisher publisher;
    protected RequestIdDispatcher<PdpStatus> dispatcher;
    protected Object lock;
    protected TimerManager timers;
    protected TimerManager.Timer timer;
    protected Queue<QueueToken<PdpMessage>> queue;
    protected RequestListener listener;
    protected PolicyModelsProviderFactoryWrapper daoFactory;
    protected PolicyModelsProvider dao;
    protected RequestParams reqParams;
    protected PdpModifyRequestMapParams mapParams;

    /**
     * Sets up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        publisher = mock(Publisher.class);
        dispatcher = mock(RequestIdDispatcher.class);
        lock = new Object();
        timers = mock(TimerManager.class);
        timer = mock(TimerManager.Timer.class);
        queue = new LinkedList<>();
        listener = mock(RequestListener.class);
        daoFactory = mock(PolicyModelsProviderFactoryWrapper.class);
        dao = mock(PolicyModelsProvider.class);

        PdpParameters pdpParams = mock(PdpParameters.class);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                queue.add(invocation.getArgumentAt(0, QueueToken.class));
                return null;
            }
        }).when(publisher).enqueue(any());

        when(timers.register(any(), any())).thenReturn(timer);

        when(daoFactory.create()).thenReturn(dao);

        PdpStateChangeParameters stateParams = mock(PdpStateChangeParameters.class);
        when(stateParams.getMaxRetryCount()).thenReturn(RETRIES);
        when(pdpParams.getStateChangeParameters()).thenReturn(stateParams);

        PdpUpdateParameters updateParams = mock(PdpUpdateParameters.class);
        when(updateParams.getMaxRetryCount()).thenReturn(RETRIES);
        when(pdpParams.getUpdateParameters()).thenReturn(updateParams);

        reqParams = new RequestParams().setMaxRetryCount(RETRIES).setModifyLock(lock).setPublisher(publisher)
                        .setResponseDispatcher(dispatcher).setTimers(timers);

        mapParams = new PdpModifyRequestMapParams().setModifyLock(lock).setPublisher(publisher)
                        .setResponseDispatcher(dispatcher).setDaoFactory(daoFactory).setUpdateTimers(timers)
                        .setStateChangeTimers(timers).setParams(pdpParams);
    }

    /**
     * Gets the listener that was registered with the dispatcher and invokes it.
     *
     * @param response the response to pass to the listener
     */
    @SuppressWarnings("unchecked")
    protected void invokeProcessResponse(PdpStatus response) {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<TypedMessageListener> processResp = ArgumentCaptor.forClass(TypedMessageListener.class);

        verify(dispatcher).register(any(), processResp.capture());

        processResp.getValue().onTopicEvent(CommInfrastructure.NOOP, PapConstants.TOPIC_POLICY_PDP_PAP, response);
    }

    /**
     * Gets the timeout handler that was registered with the timer manager and invokes it.
     */
    @SuppressWarnings("unchecked")
    protected void invokeTimeoutHandler() {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Consumer> timeoutHdlr = ArgumentCaptor.forClass(Consumer.class);

        verify(timers).register(any(), timeoutHdlr.capture());

        timeoutHdlr.getValue().accept(PDP1);
    }

    /**
     * Creates a policy with the given name and version.
     *
     * @param name policy name
     * @param version policy version
     * @return a new policy
     */
    protected ToscaPolicy makePolicy(String name, String version) {
        ToscaPolicy policy = new ToscaPolicy();

        policy.setName(name);
        policy.setVersion(version);

        return policy;
    }

    /**
     * Makes an update request with a new message.
     *
     * @param pdpName PDP name
     * @param group group name
     * @param subgroup subgroup name
     * @return a new update request
     */
    protected UpdateReq makeUpdateReq(String pdpName, String group, String subgroup) {
        UpdateReq req = mock(UpdateReq.class);

        when(req.getName()).thenReturn(MY_REQ_NAME);
        when(req.getPriority()).thenReturn(1);
        when(req.getMessage()).thenReturn(makeUpdate(pdpName, group, subgroup));

        return req;
    }

    /**
     * Makes an update message.
     *
     * @param pdpName PDP name
     * @param group group name
     * @param subgroup subgroup name
     * @return a new update message
     */
    protected PdpUpdate makeUpdate(String pdpName, String group, String subgroup) {
        PdpUpdate message = new PdpUpdate();

        message.setName(pdpName);
        message.setPolicies(Collections.emptyList());
        message.setPdpGroup(group);
        message.setPdpSubgroup(subgroup);

        return message;
    }

    /**
     * Makes a state-change request with a new message.
     *
     * @param pdpName PDP name
     * @param state desired PDP state
     * @return a new state-change request
     */
    protected StateChangeReq makeStateChangeReq(String pdpName, PdpState state) {
        StateChangeReq req = mock(StateChangeReq.class);

        when(req.getName()).thenReturn(MY_REQ_NAME);
        when(req.getPriority()).thenReturn(0);
        when(req.getMessage()).thenReturn(makeStateChange(pdpName, state));

        return req;
    }

    /**
     * Makes a state-change message.
     *
     * @param pdpName PDP name
     * @param state desired PDP state
     * @return a new state-change message
     */
    protected PdpStateChange makeStateChange(String pdpName, PdpState state) {
        PdpStateChange message = new PdpStateChange();

        message.setName(pdpName);
        message.setState(state);

        return message;
    }
}
