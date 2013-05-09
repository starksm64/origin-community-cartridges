package mgmt.jmx;


import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class Utils {
    public static JMXConnector getMBeanServerConnection(int port) throws IOException {
        String url = String.format("service:jmx:rmi:///jndi/rmi://:%d/jmxrmi", port);
        JMXServiceURL serviceURL =  new JMXServiceURL(url);
        JMXConnector jmxc = JMXConnectorFactory.connect(serviceURL, null);
        return jmxc;
    }

    public static HashMap<String, Object> parseCommandLine(String[] args, String usage) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        System.out.printf("parseCommandLine: %s\n", Arrays.asList(args));
        // Default jmx port
        int port = 6002;
        for (int n = 0; n < args.length; n += 2) {
            if(args[n].startsWith("-h")) {
                System.out.printf("Usage: %s\n", usage);
                System.exit(0);
            }
            else if(args[n].equalsIgnoreCase("-port") || args[n].equalsIgnoreCase("--port")) {
                port = Integer.parseInt(args[n+1]);
                map.put("--port", port);
            } else if(args[n].equalsIgnoreCase("-vc") || args[n].equalsIgnoreCase("--jvc")) {
                String vc = args[n+1];
                map.put("--jvc", vc);
            } else if(args[n].equalsIgnoreCase("--jvm") ) {
                String vc = args[n+1];
                map.put("--jvm", vc);
            } else {
                // Default is to assume -var = string
                String name = args[n];
                String value = args[n+1];
                map.put(name, value);
            }
        }
        return map;
    }

    public static <T> T getArg(HashMap<String, Object> map, String key, T defaultValue) {
        T value = (T) map.get(key);
        if(value == null && defaultValue != null)
            value = defaultValue;
        return value;
    }

}
