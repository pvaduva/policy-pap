FROM ubuntu:16.04

ARG HTTP_PROXY=${HTTP_PROXY}
ARG HTTPS_PROXY=${HTTPS_PROXY}
ARG BUILD_VERSION=${BUILD_VERSION}
ARG POLICY_LOGS=/var/log/onap/policy/pap

ENV http_proxy $HTTP_PROXY
ENV https_proxy $HTTPS_PROXY
ENV BUILD_VERSION ${BUILD_VERSION}
ENV POLICY_LOGS ${POLICY_LOGS}

ENV POLICY_HOME=/opt/app/policy
ENV POLICY_PAP_HOME=${POLICY_HOME}/pap

RUN \
        apt-get clean && \
        apt-get update && \
        apt-get install -y zip unzip curl wget ssh telnet maven && \
        apt-get install -y software-properties-common && \
        apt-get install -y jq httpie && \
        apt-get install -y python-pip && \
        add-apt-repository ppa:openjdk-r/ppa && \
        apt-get clean && \
        apt-get update && \
        apt-get install -y openjdk-8-jdk

RUN groupadd policy
RUN useradd --create-home --shell /bin/bash -g policy policy

RUN mkdir -p ${POLICY_PAP_HOME} ${POLICY_LOGS} ${POLICY_HOME}/etc/ssl ${POLICY_PAP_HOME}/bin && \
    chown  -R policy:policy ${POLICY_HOME} ${POLICY_PAP_HOME} ${POLICY_LOGS}

RUN mkdir /packages
COPY /maven/* /packages
RUN tar xvfz /packages/policy-pap.tar.gz --directory ${POLICY_PAP_HOME}
RUN rm /packages/policy-pap.tar.gz

WORKDIR ${POLICY_PAP_HOME}
COPY policy-pap.sh  bin/.
RUN chown -R policy:policy * && chmod +x bin/*.sh
RUN cp ${POLICY_PAP_HOME}/etc/ssl/* ${POLICY_HOME}/etc/ssl && chown policy:policy ${POLICY_HOME}/etc/ssl/*

USER policy
WORKDIR ${POLICY_PAP_HOME}/bin
ENTRYPOINT [ "bash", "./policy-pap.sh" ]