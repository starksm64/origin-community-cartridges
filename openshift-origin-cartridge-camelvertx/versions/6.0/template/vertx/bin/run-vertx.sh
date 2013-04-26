#!/usr/bin/env sh

export VERTX_HOME=$(dirname $0)/../vert.x-1.3.1.final

echo "PWD: $PWD"
echo "VERTX_HOME: $VERTX_HOME"

$VERTX_HOME/bin/vertx run  ./server.js  -cluster -cluster-port 25502 -cluster-host ${env.OPENSHIFT_CARTVERTX_IP}