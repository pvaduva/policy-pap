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

package org.onap.policy.pap.main.parameters;

import lombok.Getter;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

/**
 * Class to hold all parameters needed for pap component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@NotNull
@NotBlank
@Getter
public class PapParameterGroup extends ParameterGroupImpl {
    private RestServerParameters restServerParameters;
    private PdpParameters pdpParameters;
    private PolicyModelsProviderParameters databaseProviderParameters;
    private TopicParameterGroup topicParameterGroup;

    /**
     * Create the pap parameter group.
     *
     * @param name the parameter group name
     */
    public PapParameterGroup(final String name) {
        super(name);
    }
}
