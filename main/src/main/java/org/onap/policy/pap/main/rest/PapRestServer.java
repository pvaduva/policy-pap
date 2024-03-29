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

package org.onap.policy.pap.main.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.onap.policy.common.capabilities.Startable;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.pap.main.rest.depundep.PdpGroupDeleteControllerV1;
import org.onap.policy.pap.main.rest.depundep.PdpGroupDeployControllerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to manage life cycle of PAP rest server.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapRestServer implements Startable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PapRestServer.class);

    private List<HttpServletServer> servers = new ArrayList<>();

    private RestServerParameters restServerParameters;

    /**
     * Constructor for instantiating PapRestServer.
     *
     * @param restServerParameters the rest server parameters
     */
    public PapRestServer(final RestServerParameters restServerParameters) {
        this.restServerParameters = restServerParameters;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean start() {
        try {
            servers = HttpServletServerFactoryInstance.getServerFactory().build(getServerProperties());
            for (final HttpServletServer server : servers) {
                if (server.isAaf()) {
                    server.addFilterClass(null, PapAafFilter.class.getName());
                }
                server.start();
            }
        } catch (final Exception exp) {
            LOGGER.error("Failed to start pap http server", exp);
            return false;
        }
        return true;
    }

    /**
     * Creates the server properties object using restServerParameters.
     *
     * @return the properties object
     */
    private Properties getServerProperties() {
        final Properties props = new Properties();
        props.setProperty(PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES, restServerParameters.getName());

        final String svcpfx =
                        PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES + "." + restServerParameters.getName();

        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HOST_SUFFIX, restServerParameters.getHost());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_PORT_SUFFIX,
                        Integer.toString(restServerParameters.getPort()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_REST_CLASSES_SUFFIX,
                        String.join(",", HealthCheckRestControllerV1.class.getName(),
                                        StatisticsRestControllerV1.class.getName(),
                                        PdpGroupDeployControllerV1.class.getName(),
                                        PdpGroupDeleteControllerV1.class.getName(),
                                        PdpGroupStateChangeControllerV1.class.getName(),
                                        PdpGroupQueryControllerV1.class.getName(),
                                        PdpGroupHealthCheckControllerV1.class.getName()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_MANAGED_SUFFIX, "false");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SWAGGER_SUFFIX, "true");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_AUTH_USERNAME_SUFFIX,
                        restServerParameters.getUserName());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_AUTH_PASSWORD_SUFFIX,
                        restServerParameters.getPassword());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HTTPS_SUFFIX,
                        String.valueOf(restServerParameters.isHttps()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_AAF_SUFFIX,
                        String.valueOf(restServerParameters.isAaf()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SERIALIZATION_PROVIDER,
                        GsonMessageBodyHandler.class.getName());
        return props;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean stop() {
        for (final HttpServletServer server : servers) {
            try {
                server.stop();
            } catch (final Exception exp) {
                LOGGER.error("Failed to stop pap http server", exp);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void shutdown() {
        stop();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean isAlive() {
        return !servers.isEmpty();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("PapRestServer [servers=");
        builder.append(servers);
        builder.append("]");
        return builder.toString();
    }

}
