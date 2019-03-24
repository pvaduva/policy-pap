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

package org.onap.policy.pap.main.comm.msgdata;

import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;

/**
 * Wraps a STATE-CHANGE.
 */
public abstract class StateChangeData extends MessageData {
    private PdpStateChange stateChange;

    /**
     * Constructs the object.
     *
     * @param message message to be wrapped by this
     * @param params the parameters
     */
    public StateChangeData(PdpStateChange message, PdpModifyRequestMapParams params) {
        super(message, params.getParams().getStateChangeParameters().getMaxRetryCount(), params.getStateChangeTimers());

        stateChange = message;
    }

    @Override
    public String checkResponse(PdpStatus response) {
        if (!stateChange.getName().equals(response.getName())) {
            return "name does not match";
        }

        if (response.getState() != stateChange.getState()) {
            return "state is " + response.getState() + ", but expected " + stateChange.getState();
        }

        return null;
    }
}
