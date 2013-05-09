#!/bin/bash -eu

case "$1" in
  -v|--version)
    version="$2"
esac

# Create additional directories required by JBOSSAS
mkdir -p ${OPENSHIFT_HOMEDIR}/.m2
mkdir -p ${OPENSHIFT_JBOSSAS_WARATEK_DIR}/{template,standalone/tmp,standalone/deployments,standalone/configuration,standalone/log,standalone/data}

# Copy the version specific files up to jbossas directory
cp -r ${OPENSHIFT_JBOSSAS_WARATEK_DIR}/versions/${version}/standalone/configuration/* ${OPENSHIFT_JBOSSAS_WARATEK_DIR}/standalone/configuration
cp ${OPENSHIFT_JBOSSAS_WARATEK_DIR}/versions/${version}/bin/* ${OPENSHIFT_JBOSSAS_WARATEK_DIR}/bin
