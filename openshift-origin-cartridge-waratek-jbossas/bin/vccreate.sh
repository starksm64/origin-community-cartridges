#!/bin/bash -x

exec 1> /var/lib/javad/tmp/vccreate.log

# A tools script that creates the waratek jvc for the jbossas application
# this uses the jmx interface to the waratek cloud-jvm global service

WARATEK_TOOLS_HOME=/usr/lib/jvm/java-waratek/tools/
uuid=$OPENSHIFT_GEAR_UUID

# Import Environment Variables
#for f in /var/lib/openshift/${uuid}/.env/*
#do
#    . $f
#done
env

cartridge_type="jbossas-waratek"
CART_DIR=$OPENSHIFT_HOMEDIR/$cartridge_type

function print_sed_exp_replace_env_var {
  sed_exp=""
  for openshift_var in $(env | grep OPENSHIFT_ | awk -F '=' '{print $1}')
  do
    variable_val=$(echo "${!openshift_var}" | sed -e "s@\/@\\\\/@g")
    sed_exp="${sed_exp} -e s/\${env.${openshift_var}}/${variable_val}/g"
  done
  printf "%s\n" "$sed_exp"
}


# Create a link for each file in user config to server standalone/config
if [ -d ${OPENSHIFT_REPO_DIR}/.openshift/config ]
then
  for f in ${OPENSHIFT_REPO_DIR}/.openshift/config/*
  do
    target=$(basename $f)
    # Remove any target that is being overwritten
    if [ -e "${CART_DIR}/standalone/configuration/$target" ]
    then
       echo "Removing existing $target"
       rm -rf "${CART_DIR}/standalone/configuration/$target"
    fi
    ln -s $f "${CART_DIR}/standalone/configuration/"
  done
fi
# Now go through the standalone/configuration and remove any stale links from previous
# deployments
for f in "${CART_DIR}/standalone/configuration"/*
do
    target=$(basename $f)
    if [ ! -e $f ]
    then
        echo "Removing obsolete $target"
        rm -rf $f
    fi
done

MYSQL_ENABLED="false"
if [ -n "$OPENSHIFT_MYSQL_DB_URL" ]
then
    MYSQL_ENABLED="true"
fi

POSTGRESQL_ENABLED="false"
if [ -n "$OPENSHIFT_POSTGRESQL_DB_URL" ]
then
    POSTGRESQL_ENABLED="true"
fi

# Get user based limits or non-root defaults
if ! [ $(id -u) -eq 0 ]; then
    max_memory_bytes=`oo-cgroup-read memory.limit_in_bytes`
    max_threads=`ulimit -u`
fi

if ! [[ "$max_memory_bytes" =~ ^[0-9]+$ ]] ; then
        max_memory_bytes=536870912
fi

if ! [[ "$max_threads" =~ ^[0-9]+$ ]] ; then
        max_threads=1024
fi

if [ -z $JVM_HEAP_RATIO ]; then
	JVM_HEAP_RATIO=0.5
fi
if [ -z $JVM_PERMGEN_RATIO ]; then
	JVM_PERMGEN_RATIO=0.2
fi
if [ -z $MESSAGING_THREAD_RATIO ]; then
	MESSAGING_THREAD_RATIO=0.2
fi

max_memory_mb=`expr $max_memory_bytes / 1048576`
max_heap=$( echo "$max_memory_mb * $JVM_HEAP_RATIO" | bc | awk '{print int($1+0.5)}')
max_permgen=$( echo "$max_memory_mb * $JVM_PERMGEN_RATIO" | bc | awk '{print int($1+0.5)}')

messaging_thread_pool_max_size=$( echo "$max_threads * $MESSAGING_THREAD_RATIO" | bc | awk '{print int($1+0.5)}')
messaging_scheduled_thread_pool_max_size=$( echo "$max_threads * $MESSAGING_THREAD_RATIO" | bc | awk '{print int($1+0.5)}')

if [ $max_permgen -gt 256 ]
then
	max_permgen=256
fi

if [ $max_heap -lt 1024 ]
then
	memory_options="-Xmx${max_heap}m -XX:MaxPermSize=${max_permgen}m -XX:+AggressiveOpts -Dorg.apache.tomcat.util.LOW_MEMORY=true"
else
	memory_options="-Xmx${max_heap}m -XX:MaxPermSize=${max_permgen}m -XX:+AggressiveOpts"
fi

sed_replace_env=$(print_sed_exp_replace_env_var)

sed -i -e "s/\${mysql.enabled}/$MYSQL_ENABLED/g" \
       -e "s/\${postgresql.enabled}/$POSTGRESQL_ENABLED/g" \
       -e "s/\${messaging.thread.pool.max.size}/$messaging_thread_pool_max_size/g" \
       -e "s/\${messaging.scheduled.thread.pool.max.size}/$messaging_scheduled_thread_pool_max_size/g" \
       -e "s/\${OPENSHIFT_INTERNAL_IP}/OPENSHIFT_JBOSSAS_WARATEK_IP/g" \
       ${sed_replace_env} \
       "${CART_DIR}"/standalone/configuration/standalone.xml > /dev/null 2>&1

sed -i -e "s/\${env.OPENSHIFT_MYSQL_DB_HOST}/localhost/g" \
       -e "s/\${env.OPENSHIFT_MYSQL_DB_PORT}/3306/g" \
       -e "s/\${env.OPENSHIFT_MYSQL_DB_USERNAME}/username/g" \
       -e "s/\${env.OPENSHIFT_MYSQL_DB_PASSWORD}/password/g" \
       "${CART_DIR}"/standalone/configuration/standalone.xml > /dev/null 2>&1



#
# Specify options to pass to the Java VM.
#
if [ -z $JAVA_OPTS ]; then
   JAVA_OPTS="$memory_options -Dorg.jboss.resolver.warning=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djboss.node.name=${OPENSHIFT_GEAR_DNS} -Djgroups.bind_addr=${OPENSHIFT_JBOSSAS_WARATEK_IP} -Dorg.apache.coyote.http11.Http11Protocol.COMPRESSION=on"
fi

# Add the user module path ahead of the server modules root
if [ -z JBOSS_MODULEPATH_ADD ]; then
   if [ -z $OPENSHIFT_JBOSSAS_MODULE_PATH ]; then
      export JBOSS_MODULEPATH=${CART_DIR}/standalone/configuration/modules:${CART_DIR}/modules
   else
      export JBOSS_MODULEPATH=${CART_DIR}/standalone/configuration/modules:${OPENSHIFT_JBOSSAS_MODULE_PATH}:${CART_DIR}/modules
   fi
else
   if [ -z $OPENSHIFT_JBOSSAS_MODULE_PATH ]; then
      export JBOSS_MODULEPATH=${CART_DIR}/standalone/configuration/modules:${JBOSS_MODULEPATH_ADD}:${CART_DIR}/modules
   else
      export JBOSS_MODULEPATH=${CART_DIR}/standalone/configuration/modules:${JBOSS_MODULEPATH_ADD}:${OPENSHIFT_JBOSSAS_MODULE_PATH}:${CART_DIR}/modules
   fi
fi

export JAVA_OPTS
/etc/alternatives/jre/bin/java -jar ${WARATEK_TOOLS_HOME}/vcadmin.jar $*
