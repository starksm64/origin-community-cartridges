package mgmt.jmx;

import cloudvm.management.VirtualMachineMXBean;
import com.beust.jcommander.JCommander;
import mgmt.CmdArgs;

import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.HashMap;

/**
 * A utility class to query the waratek vc related mbeans
 */
public class GetVCInfo {

    public static void main(String[] args) throws Exception {
        CmdArgs cmdArgs = new CmdArgs();
        JCommander parser = new JCommander(cmdArgs);
        parser.parse(args);
        int port = cmdArgs.port;
        String vc = cmdArgs.jvc;

        String url = String.format("service:jmx:rmi:///jndi/rmi://:%d/jmxrmi", port);
        JMXServiceURL serviceURL =  new JMXServiceURL(url);
        JMXConnector jmxc = JMXConnectorFactory.connect(serviceURL, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        ObjectName vm = new ObjectName("com.waratek:type=VirtualMachine");
        if (mbsc.isRegistered(vm) == false)
            throw new InstanceNotFoundException("com.waratek:type=VirtualMachine");
        System.out.printf("Found VM bean: %s\n", mbsc.getMBeanInfo(vm));
        ObjectName name = new ObjectName("java.lang:type=Threading");
        System.out.printf("%s\n", mbsc.getMBeanInfo(name));

        VirtualMachineMXBean vmBean = JMX.newMXBeanProxy(mbsc, vm, VirtualMachineMXBean.class);

        JVCMXBeans vcinfo = JVCMXBeans.loadMXBeansFor(mbsc, vc);
        System.out.printf("%s\n", vcinfo);
        jmxc.close();
    }
}
