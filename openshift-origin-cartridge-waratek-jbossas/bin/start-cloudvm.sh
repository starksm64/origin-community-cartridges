#!/bin/bash
# start-cloudvm - Starts and stops a single instance of waratek Cloud VM
#
# the following is chkconfig init header
#
# start-cloudvm: waratek cloudvm daemon
#
# chkconfig: 345 97 03
# description:
#
# processname: javad
# pidfile: /var/lib/javad/cloud-jvm/jvm.pid
#

set -x
export INSTALL_PREFIX=""
export WARATEK_HOME="${INSTALL_PREFIX}/usr/lib/jvm/java-waratek/jre"
export WARATEK_OWNER="jboss"
export DEFAULT_JVM_NAME="cloud-jvm"
export WARATEK_OPTS="-noverify -Xdaemon -Xmx6144m"
JMX_PORT=6002
# The JMX/HTTP REST flag
JMX_HTTP="-Dcom.waratek.jmxhttp.jolokia"
# The JMX/RMI flag
JMX_RMI="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=${JMX_PORT} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
# Which JMX access to use
JMX_OPTS="${JMX_HTTP} ${JMX_RMI}"

SERVICE=$WARATEK_HOME/bin/javad

VARLIB_PATH="${INSTALL_PREFIX}/var/lib/javad"

VMPID=0

case "$1" in

fix)
    if [[ $(id -u) -eq 0 ]]; then
        echo "Creating and updating /var/{lib,log,run}/javad directories and permissions..."
        mkdir -p /var/lib/javad/$DEFAULT_JVM_NAME
        chown -R $WARATEK_OWNER.$WARATEK_OWNER $VARLIB_PATH/$DEFAULT_JVM_NAME/
        mkdir -p /var/log/javad
        chown -R $WARATEK_OWNER.$WARATEK_OWNER /var/log/javad
        mkdir -p /var/run/javad/$DEFAULT_JVM_NAME
        chown -R $WARATEK_OWNER.$WARATEK_OWNER /var/run/javad
        echo "done"
    else
        echo "Skipping as script is not running as root"
    fi
;;

start)
	if [ -d $VARLIB_PATH ]; then
			PIDFILE=$VARLIB_PATH/$DEFAULT_JVM_NAME/jvm.pid
			JVM_NAME=$DEFAULT_JVM_NAME
            if [ -f $PIDFILE ] && [[ -n `ps -p $(cat $PIDFILE) | grep $(cat $PIDFILE)` ]]; then
              echo "$JVM_NAME already running (pid $(cat $PIDFILE)).."
            else
              mkdir -p $VARLIB_PATH/$DEFAULT_JVM_NAME/
                echo "PIDFILE=${PIDFILE}"
		mcs_level="s0:c0,c497"
		CMD="runcon -u system_u -r system_r -t initrc_t -l $mcs_level $SERVICE $WARATEK_OPTS ${JMX_OPTS} -Dcom.waratek.jvm.name=${JVM_NAME}"
              if [[ $(id -u) -eq 0 ]]; then
                echo "This script runs as root. $WARATEK_OWNER will own the JVM process"
        	echo `date` > /var/lib/javad/nohup-${JVM_NAME}.out
        	echo "CMD=$CMD" >> /var/lib/javad/nohup-${JVM_NAME}.out
                $CMD >> /var/lib/javad/nohup-${JVM_NAME}.out 2>&1 &
                ps -ef | grep javad >>  /var/lib/javad/nohup-${JVM_NAME}.out
                echo "Checking for process..."
                sleep 5
                ps -ef | grep 'Dcom.waratek.jvm.name=' | grep -v grep
                VMPID=`ps -ef | awk '/[D]com.waratek.jvm.name=/{print $2}'`
                declare -a pids=($VMPID);
                VMPID=${pids[${#pids[@]}-1]}
              elif [ "$(id -nu)" == "jboss" -o "x$(id -nu)" == "x$WARATEK_OWNER" ]; then
                echo "$(id -u) is running this script and  will own the JVM process"
                echo "Run command: $CMD"
                /bin/bash "$CMD > /tmp/nohup-${JVM_NAME}.out 2>&1 &"
                echo "Checking for process..."
                sleep 5
                VMPID=`ps -ef | awk '/[D]com.waratek.jvm.name=/{print $2}'`
               else
                 echo "$(id -u) cannot run this script. Exit!"
                 exit 1
              fi
              echo "Wrote $VMPID to $PIDFILE"
              echo "Starting $JVM_NAME pid $VMPID"
            fi
	fi
;;

stop)
	if [ -d ${VARLIB_PATH} ]; then
		PIDFILE=$VARLIB_PATH/$DEFAULT_JVM_NAME/jvm.pid
		JVM_NAME=$DEFAULT_JVM_NAME
        if [ -f $PIDFILE ] && [[ -n `ps -p $(cat $PIDFILE) | grep $(cat $PIDFILE)` ]]; then
           cat $PIDFILE | xargs kill -9
           RETVAL=$?
           if [ $RETVAL -eq 0 ]; then
              rm -f $PIDFILE
              echo "$JVM_NAME stopped.."
           else
              echo "Cannot stop $JVM_NAME"
              exit 1
           fi
        else
           echo "$JVM_NAME is not running.."
           [ -f $PIDFILE ] && rm -f $PIDFILE
        fi
	fi
;;

restart)
    $0 stop
    $0 start
;;

status)
	if [ -d ${VARLIB_PATH} ]; then
		PIDFILE=$VARLIB_PATH/$DEFAULT_JVM_NAME/jvm.pid
		JVM_NAME=$DEFAULT_JVM_NAME
        if [ -f $PIDFILE ] && [[ -n `ps -p $(cat $PIDFILE) |grep $(cat $PIDFILE)` ]]; then
           echo "$JVM_NAME pid $(cat $PIDFILE) is running"
           exit 0
        fi
        echo "$JVM_NAME is not running"
        [ -f $PIDFILE ] && rm -f $PIDFILE
        exit 1
	fi
;;

list)
    ls -lR /var/lib/javad
    ls -lR /var/log/javad
    ls -lR /var/run/javad
;;

*)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
esac

