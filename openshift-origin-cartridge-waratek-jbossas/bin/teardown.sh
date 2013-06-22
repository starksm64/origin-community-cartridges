#!/bin/bash -x

WARATEK_TOOLS_HOME=${OPENSHIFT_JBOSSAS_WARATEK_DIR}/bin
VCADMIN="/etc/alternatives/jre/bin/java -jar ${WARATEK_TOOLS_HOME}/vcadmin.jar"

rm -rf ${OPENSHIFT_HOMEDIR}/.m2

# Create the container
uuid=`id -nu`
export OPENSHIFT_GEAR_UUID=$uuid
${VCADMIN} --command destroy --jvm cloud-jvm --jvc jvc-${uuid}
