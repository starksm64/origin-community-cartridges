#!/bin/bash

WARATEK_TOOLS_HOME=${OPENSHIFT_JBOSSAS_WARATEK_DIR}/bin

/etc/alternatives/jre/bin/java -jar ${WARATEK_TOOLS_HOME}/vcadmin.jar $*

