#!/bin/bash
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2019 Nordix Foundation.
#  Modifications Copyright (C) 2019 AT&T Intellectual Property.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#

JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk/
POLICY_PAP_HOME=/opt/app/policy/pap
KEYSTORE="${POLICY_HOME}/etc/ssl/policy-keystore"
KEYSTORE_PASSWD="Pol1cy_0nap"
TRUSTSTORE="${POLICY_HOME}/etc/ssl/policy-truststore"
TRUSTSTORE_PASSWD="Pol1cy_0nap"


if [ "$#" -ge 1 ]; then
    CONFIG_FILE=$1
else
    CONFIG_FILE=${CONFIG_FILE}
fi

if [ "$#" -ge 2 ]; then
    PROP_FILE=$2
else
    PROP_FILE=${PROP_FILE}
fi

if [ -z "$CONFIG_FILE" ]
  then
    CONFIG_FILE="$POLICY_PAP_HOME/etc/defaultConfig.json"
fi

if [ -z "$PROP_FILE" ]
  then
    PROP_FILE="$POLICY_PAP_HOME/etc/topic.properties"
fi

echo "Policy pap config file: $CONFIG_FILE"
echo "Policy pap property file: $PROP_FILE"

$JAVA_HOME/bin/java -cp "$POLICY_PAP_HOME/etc:$POLICY_PAP_HOME/lib/*" -Djavax.net.ssl.keyStore="$KEYSTORE" -Djavax.net.ssl.keyStorePassword="$KEYSTORE_PASSWD" -Djavax.net.ssl.trustStore="$TRUSTSTORE" -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASSWD" org.onap.policy.pap.main.startstop.Main -c $CONFIG_FILE -p $PROP_FILE
