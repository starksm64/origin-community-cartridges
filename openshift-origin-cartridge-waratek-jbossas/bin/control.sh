#!/bin/bash -x

exec 1> /var/lib/javad/tmp/config.log

source $OPENSHIFT_CARTRIDGE_SDK_BASH

WARATEK_TOOLS_HOME=/usr/lib/jvm/java-waratek/tools/
VCADMIN="/etc/alternatives/jre/bin/java -jar ${WARATEK_TOOLS_HOME}/vcadmin.jar"

cartridge_type="jbossas-waratek"
echo "input uuid=$uuid, id=`id -nu`"
if [ "$uuid" == "" ]; then
        echo "Using id..."
        uuid=`id -nu`
fi

# Import Environment Variables
for f in /var/lib/openshift/${uuid}/.env/*
do
    var=`basename $f`
    echo "Setting ${var}..."
    export ${var}=`cat $f`
done

# Checks for the presence of the user-specified hot_deploy marker in the app
# repo. Returns 0 if the marker is present, otherwise 1.
function hot_deploy_marker_is_present {
  if [ -f "${OPENSHIFT_REPO_DIR}/.openshift/markers/hot_deploy" ]; then
    return 0
  else
    return 1
  fi
}

# Check if the jbossas JVC is running
function isrunning() {
    status=`${VCADMIN} --command status --jvc jvc-${uuid}`
    echo "status=$status"
    if echo $status | grep Running > /dev/null; then
        return 0
    fi
    return 1
}


# Check if the server http port is up
function ishttpup() {

    let count=0
    while [ ${count} -lt 24 ]
    do
        echo "Checking for ${OPENSHIFT_JBOSSAS_WARATEK_IP}:${OPENSHIFT_JBOSSAS_HTTP_PORT} listening port"
        if /usr/sbin/lsof -P -n -i "@${OPENSHIFT_JBOSSAS_WARATEK_IP}:${OPENSHIFT_JBOSSAS_HTTP_PORT}" | grep "(LISTEN)" > /dev/null; then
            echo "Found ${OPENSHIFT_JBOSSAS_WARATEK_IP}:${OPENSHIFT_JBOSSAS_HTTP_PORT} listening port"
            return 0
        fi
        let count=${count}+1

        sleep 2
    done

    return 1
}

function build() {
    
	CONFIG_DIR="${OPENSHIFT_JBOSSAS_WARATEK_DIR}/standalone/configuration"
	OPENSHIFT_MAVEN_MIRROR="${CONFIG_DIR}/settings.base.xml"
  if [[ $OPENSHIFT_GEAR_DNS =~ .*\.rhcloud\.com$ ]]
	then
	    OPENSHIFT_MAVEN_MIRROR="${CONFIG_DIR}/settings.rhcloud.xml"
	fi

	max_memory_bytes=$(oo-cgroup-read memory.limit_in_bytes)
	max_memory_mb=$(expr $max_memory_bytes / 1048576)

	# If hot deploy is enabled, we need to restrict the Maven memory size to fit
	# alongside the running application server. For now, just hard-code it to 64
	# and figure out how to apply a scaling factor later.
	if hot_deploy_marker_is_present
    then
    	echo "Scaling down Maven heap settings due to presence of hot_deploy marker"
    
    	if [ -z $MAVEN_JVM_HEAP_RATIO ]
        then
			  MAVEN_JVM_HEAP_RATIO=0.25
		fi
	else
		if [ -z $MAVEN_JVM_HEAP_RATIO ]
        then
			MAVEN_JVM_HEAP_RATIO=0.75
		fi
	fi

	max_heap=$( echo "$max_memory_mb * $MAVEN_JVM_HEAP_RATIO" | bc | awk '{print int($1+0.5)}')

	OPENSHIFT_MAVEN_XMX="-Xmx${max_heap}m"

	if [ -z "$BUILD_NUMBER" ]
	then
    	SKIP_MAVEN_BUILD=false
    	if git show master:.openshift/markers/skip_maven_build > /dev/null 2>&1
    	then
    	    SKIP_MAVEN_BUILD=true
    	fi
    
    	if [ -f "${OPENSHIFT_REPO_DIR}/.openshift/markers/force_clean_build" ]
    	then
        	echo ".openshift/markers/force_clean_build found!  Removing Maven dependencies." 1>&2
        	rm -rf ${OPENSHIFT_HOMEDIR}.m2/* ${OPENSHIFT_HOMEDIR}.m2/.[^.]*
    	fi

    	if [ -f ${OPENSHIFT_REPO_DIR}pom.xml ] && ! $SKIP_MAVEN_BUILD
    	then
        	if [ -e ${OPENSHIFT_REPO_DIR}.openshift/markers/java7 ];
        	then
           		export JAVA_HOME=/etc/alternatives/java_sdk_1.7.0
        	else
          		export JAVA_HOME=/etc/alternatives/java_sdk_1.6.0
        	fi

          echo $JAVA_HOME >$OPENSHIFT_JBOSSAS_WARATEK_DIR/env/JAVA_HOME
          echo "$JAVA_HOME/bin:$M2_HOME/bin" >$OPENSHIFT_JBOSSAS_WARATEK_DIR/env/OPENSHIFT_JBOSSAS_PATH

        	export MAVEN_OPTS="$OPENSHIFT_MAVEN_XMX"
        	export PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH
        	pushd ${OPENSHIFT_REPO_DIR} > /dev/null

        	if [ -z "$MAVEN_OPTS" ]; then
        		export MAVEN_OPTS="$OPENSHIFT_MAVEN_XMX"
        	fi
        	
        	if [ -z "$MAVEN_ARGS" ]; then
		    	export MAVEN_ARGS="clean package -Popenshift -DskipTests"
        	fi
        
        	echo "Found pom.xml... attempting to build with 'mvn -e ${MAVEN_ARGS}'"
        
        	if [ -n "$OPENSHIFT_MAVEN_MIRROR" ]
        	then
            	mvn --global-settings $OPENSHIFT_MAVEN_MIRROR --version
            	mvn --global-settings $OPENSHIFT_MAVEN_MIRROR $MAVEN_ARGS
        	else
            	mvn --version
            	mvn $MAVEN_ARGS
        	fi
        	popd > /dev/null
    	fi
	fi

}

function deploy() {
  echo "Deploying JBoss"
  
	if [ "$(ls ${OPENSHIFT_REPO_DIR}/deployments)" ]; then
		rsync -r --delete --exclude ".*" ${OPENSHIFT_REPO_DIR}/deployments/ ${OPENSHIFT_JBOSSAS_WARATEK_DIR}/standalone/deployments/
	else
    rm -rf ${OPENSHIFT_JBOSSAS_WARATEK_DIR}/standalone/deployments/*
  fi
}

function start() {
  echo "Starting $cartridge_type cart"
    
  if marker_present "enable_jpda"
  then
    export ENABLE_JPDA=1
  fi

  # Check for running app
  if isrunning; then
      echo "Application is already running"
  else    
    echo "Sending start to jvc-${uuid} ..." 1>&2
    ${VCADMIN} --command start --jvm cloud-jvm --jvc jvc-${uuid}
    if ! ishttpup; then
        echo "Timed out waiting for http listening port"
        exit 1
    fi
  fi
}


function stop() {
  echo "Stopping $cartridge_type cart"
  
    if ! isrunning; then
        echo "Application is already stopped" 1>&2
    else
        echo "Sending stop to jvc-${uuid} ..." 1>&2
        ${VCADMIN} --command stop --jvm cloud-jvm --jvc jvc-${uuid}
    fi
}

function restart() {
    echo "Restarting $cartridge_type cart"
   
  	stop
  	
  	start
}

function status() {
   if isrunning
   then
      echo "Application is running"
   else
      echo "Application is either stopped or inaccessible"
   fi
}

function reload() {
    echo "Reloading $cartridge_type cart"
    restart
}

# Clean up any log files
function tidy() {
  client_message "Emptying log dir: $OPENSHIFT_JBOSSAS_LOG_DIR"
  shopt -s dotglob
  rm -rf $OPENSHIFT_JBOSSAS_LOG_DIR/*
}

function threaddump() {
	echo "Thread dump for $cartridge_type cart"
	
    if ! isrunning; then
        echo "Application is stopped"
        exit 1
    else
        ${VCADMIN} --command summary --jvm cloud-jvm --jvc jvc-${uuid}
    fi
}

function prebuild() {
	echo "prebuild"
}

function postdeploy() {
	echo "postdeploy"
}

echo "Running control for $1, uuid=${uuid}"

case "$1" in
  build)		build ;;
  deploy)	    deploy ;;
  start)     	start ;;
  stop)      	stop ;;
  restart)   	restart ;;
  status)    	status ;;
  reload)    	reload ;;
  tidy)      	tidy ;;
  threaddump)   threaddump ;;
  pre-build)    prebuild ;;
  post-deploy)  postdeploy ;;
  *)         	exit 0
esac
