/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.startstop;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpMessageType;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pap.main.rest.PapRestServer;
import org.onap.policy.pap.main.rest.PapStatisticsManager;

/**
 * This class wraps a distributor so that it can be activated as a complete service
 * together with all its pap and forwarding handlers.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapActivator extends ServiceManagerContainer {
    private static final String[] MSG_TYPE_NAMES = {"messageName"};
    private static final String[] REQ_ID_NAMES = {"response", "responseTo"};

    private final PapParameterGroup papParameterGroup;

    /**
     * The PAP REST API server.
     */
    private PapRestServer restServer;

    /**
     * Listens for messages on the topic, decodes them into a {@link PdpStatus} message,
     * and then dispatches them to {@link #reqIdDispatcher}.
     */
    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Listens for {@link PdpStatus} messages and then routes them to the listener
     * associated with the ID of the originating request.
     */
    private final RequestIdDispatcher<PdpStatus> reqIdDispatcher;

    /**
     * Instantiate the activator for policy pap as a complete service.
     *
     * @param papParameterGroup the parameters for the pap service
     * @param topicProperties properties used to configure the topics
     */
    public PapActivator(final PapParameterGroup papParameterGroup, Properties topicProperties) {
        super("Policy PAP");

        TopicEndpoint.manager.addTopicSinks(topicProperties);
        TopicEndpoint.manager.addTopicSources(topicProperties);

        try {
            this.papParameterGroup = papParameterGroup;
            this.msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
            this.reqIdDispatcher = new RequestIdDispatcher<>(PdpStatus.class, REQ_ID_NAMES);

        } catch (RuntimeException e) {
            throw new PolicyPapRuntimeException(e);
        }

        papParameterGroup.getRestServerParameters().setName(papParameterGroup.getName());

        final Object pdpUpdateLock = new Object();
        PdpParameters pdpParams = papParameterGroup.getPdpParameters();
        AtomicReference<Publisher> pdpPub = new AtomicReference<>();
        AtomicReference<TimerManager> pdpUpdTimers = new AtomicReference<>();
        AtomicReference<TimerManager> pdpStChgTimers = new AtomicReference<>();

        // @formatter:off
        addAction("PAP parameters",
            () -> ParameterService.register(papParameterGroup),
            () -> ParameterService.deregister(papParameterGroup.getName()));

        addAction("Request ID Dispatcher",
            () -> msgDispatcher.register(PdpMessageType.PDP_STATUS.name(), this.reqIdDispatcher),
            () -> msgDispatcher.unregister(PdpMessageType.PDP_STATUS.name()));

        addAction("Message Dispatcher",
            () -> registerMsgDispatcher(),
            () -> unregisterMsgDispatcher());

        addAction("topics",
            () -> TopicEndpoint.manager.start(),
            () -> TopicEndpoint.manager.shutdown());

        addAction("PAP statistics",
            () -> Registry.register(PapConstants.REG_STATISTICS_MANAGER, new PapStatisticsManager()),
            () -> Registry.unregister(PapConstants.REG_STATISTICS_MANAGER));

        addAction("PDP publisher",
            () -> {
                pdpPub.set(new Publisher(PapConstants.TOPIC_POLICY_PDP_PAP));
                startThread(pdpPub.get());
            },
            () -> pdpPub.get().stop());

        addAction("PDP update timers",
            () -> {
                pdpUpdTimers.set(new TimerManager("update", pdpParams.getUpdateParameters().getMaxWaitMs()));
                startThread(pdpUpdTimers.get());
            },
            () -> pdpUpdTimers.get().stop());

        addAction("PDP state-change timers",
            () -> {
                pdpStChgTimers.set(new TimerManager("state-change", pdpParams.getUpdateParameters().getMaxWaitMs()));
                startThread(pdpStChgTimers.get());
            },
            () -> pdpStChgTimers.get().stop());

        addAction("PDP modification lock",
            () -> Registry.register(PapConstants.REG_PDP_MODIFY_LOCK, pdpUpdateLock),
            () -> Registry.unregister(PapConstants.REG_PDP_MODIFY_LOCK));

        addAction("PDP modification requests",
            () -> Registry.register(PapConstants.REG_PDP_MODIFY_MAP, new PdpModifyRequestMap(
                            new PdpModifyRequestMapParams()
                                    .setModifyLock(pdpUpdateLock)
                                    .setParams(pdpParams)
                                    .setPublisher(pdpPub.get())
                                    .setResponseDispatcher(reqIdDispatcher)
                                    .setStateChangeTimers(pdpStChgTimers.get())
                                    .setUpdateTimers(pdpUpdTimers.get()))),
            () -> Registry.unregister(PapConstants.REG_PDP_MODIFY_MAP));

        addAction("Create REST server",
            () -> {
                restServer = new PapRestServer(papParameterGroup.getRestServerParameters());
            },
            () -> {
                restServer = null;
            });

        addAction("REST server",
            () -> restServer.start(),
            () -> restServer.stop());
        // @formatter:on
    }

    /**
     * Starts a background thread.
     *
     * @param runner function to run in the background
     */
    private void startThread(Runnable runner) {
        Thread thread = new Thread(runner);
        thread.setDaemon(true);

        thread.start();
    }

    /**
     * Get the parameters used by the activator.
     *
     * @return the parameters of the activator
     */
    public PapParameterGroup getParameterGroup() {
        return papParameterGroup;
    }

    /**
     * Registers the dispatcher with the topic source(s).
     */
    private void registerMsgDispatcher() {
        for (TopicSource source : TopicEndpoint.manager
                        .getTopicSources(Arrays.asList(PapConstants.TOPIC_POLICY_PDP_PAP))) {
            source.register(msgDispatcher);
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     */
    private void unregisterMsgDispatcher() {
        for (TopicSource source : TopicEndpoint.manager
                        .getTopicSources(Arrays.asList(PapConstants.TOPIC_POLICY_PDP_PAP))) {
            source.unregister(msgDispatcher);
        }
    }
}
