package mgmt.jmx;

import cloudvm.management.JavaContainerMXBean;
import cloudvm.management.VirtualMachineMXBean;
import com.beust.jcommander.JCommander;
import mgmt.CmdArgs;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.*;
import java.lang.management.RuntimeMXBean;
import java.util.*;

/**
 * A waratek JVC admin class that uses jmx to communicate with the global waratek JVM.
 */
public class VCAdmin {
   public static JavaContainerMXBean[] pollMXBeans(MBeanServerConnection conn)
      throws Exception {
      ObjectName pattern = new ObjectName("com.waratek:type=*,name=VirtualContainer");
      TreeMap<Integer, JavaContainerMXBean> jvcs = new TreeMap<Integer, JavaContainerMXBean>();
      for (ObjectName name : conn.queryNames(pattern, null)) {
         JavaContainerMXBean jvc = JMX.newMXBeanProxy(conn, name, JavaContainerMXBean.class);
         jvcs.put(jvc.getContainerId(), jvc);
      }
      JavaContainerMXBean[] beans = new JavaContainerMXBean[jvcs.size()];
      return jvcs.values().toArray(beans);
   }

   /**
    * @param args The java arguments which will be executed Arguments should not contain whitespace characters e.g String [] args = new String[] { "java", "-classpath", ".", "HelloWorld" };
    * @param dir  Directory to execute java command
    */
   public static void createJavaContainer(VirtualMachineMXBean vmBean, String name, String[] args, String dir)
      throws Exception {
      String id = getId();
      System.out.printf("createJavaContainer(args=%s, dir=%s, user=%s)\n", Arrays.asList(args), dir, id);
      //Object result = vmBean.createJavaContainer(name, cmd, dir);
        /*
        ObjectName vm = new ObjectName("com.waratek:type=VirtualMachine");
        String[] sig = {"java.lang.String;", "java.lang.String;", "java.lang.String"};
        Object[] iargs = {args, env, dir};
        Object result = mbsc.invoke(vm, "createJavaContainer", iargs, sig);
        */
      StringBuilder cmd = new StringBuilder();
      for (String arg : args) {
         cmd.append(arg);
         cmd.append(' ');
      }
      String containerID = vmBean.createContainer(name, cmd.toString(), dir);
      System.out.printf("result=%s\n", containerID);
   }

   public static int findJVC(MBeanServerConnection mbsc, String name) throws Exception {
      // Find the vcid for the vc==stdio1 jvc
      int vcid = -1;
      JavaContainerMXBean[] jvcs = pollMXBeans(mbsc);
      for (JavaContainerMXBean jvc : jvcs) {
         String vcname = jvc.getContainerName();
         if (vcname.equals(name)) {
            vcid = jvc.getContainerId();
            System.out.printf("Mapped %s to vcid=%s\n", name, vcid);
         }
      }
      return vcid;
   }

   static String getId() throws Exception {
      ProcessBuilder pb = new ProcessBuilder("/usr/bin/id");
      Process p = pb.start();
      InputStream is = p.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String id = reader.readLine();
      reader.close();
      p.waitFor();
      return id;
   }

   private static void ls(String path) {
      File dir = new File(path);
      System.out.printf("%s STATUS\n", dir.getAbsolutePath());
      System.out.printf("\tcanRead:%s\n", dir.canRead());
      System.out.printf("\tlength:%s\n", dir.length());
      for (String child : dir.list())
         System.out.printf("\t:%s\n", child);
   }

   private static Properties getEnv() {
      Map<String, String> env = System.getenv();
      Properties props = new Properties();
      for (String key : env.keySet()) {
         props.setProperty(key, env.get(key));
      }
      return props;
   }

   /**
    * @param args
    * @throws Exception
    */
   public static void main(String[] args) throws Exception {
      CmdArgs cmdArgs = new CmdArgs();
      JCommander parser = new JCommander(cmdArgs);
      parser.parse(args);
      if (cmdArgs.help) {
         parser.usage();
         System.exit(0);
      }

      String url = String.format(cmdArgs.urlBase, cmdArgs.port);
      JMXServiceURL serviceURL = new JMXServiceURL(url);
      System.out.printf("Connecting to: %s\n", serviceURL);
      JMXConnector jmxc = JMXConnectorFactory.connect(serviceURL, null);
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      ObjectName vm = new ObjectName("com.waratek:type=VirtualMachine");
      if (mbsc.isRegistered(vm) == false)
         throw new InstanceNotFoundException("com.waratek:type=VirtualMachine");
      VirtualMachineMXBean vmBean = JMX.newMXBeanProxy(mbsc, vm, VirtualMachineMXBean.class);


      try {
         CmdArgs.Command command = cmdArgs.command;
         switch (command) {
            case CREATE:
               System.out.printf("Creating jvc=%s...\n", cmdArgs.jvc);
               // First look for a --argsTemplate
               String argsTemplate = cmdArgs.argsTemplate;
               String createArgs;
               if (argsTemplate == null) {
                  createArgs = cmdArgs.getArgs();
               } else {
                  String jbossHome = cmdArgs.jbossHome;
                  ls(jbossHome);
                  Properties env = getEnv();
                  env.setProperty("jbossHome", jbossHome);
                  System.out.printf("Using env: %s\n", env);
                  createArgs = LaunchAS7.getAS7Args(argsTemplate, env);
               }
               String dir = "/var/lib/javad/cloud-jvm/" + cmdArgs.jvc;
               if (cmdArgs.dryRun)
                  System.out.printf("would call createJavaContainer(args=%s, dir=%s, user=%s)\n", createArgs, dir, getId());
               else
                  createJavaContainer(vmBean, cmdArgs.jvc, new String[]{createArgs}, dir);
               break;
            case START:
               System.out.printf("Starting jvc=%s...\n", cmdArgs.jvc);
               vmBean.startContainer(cmdArgs.jvc);
               break;
            case STOP:
               System.out.printf("Stopping jvc=%s...\n", cmdArgs.jvc);
               vmBean.shutdownContainer(cmdArgs.jvc);
               break;
            case DESTROY:
               System.out.printf("Destroying jvc=%s...\n", cmdArgs.jvc);
               vmBean.undefineContainer(cmdArgs.jvc);
               break;
            case STATUS:
               JavaContainerMXBean vcBean = vmBean.getJavaContainer(cmdArgs.jvc);
               String status = vcBean.getStatus();
               System.out.printf("%s\n", status);
               break;
            case SUMMARY:
               System.out.printf("Getting STATUS SUMMARY of vc=%s...\n", cmdArgs.jvc);
               JVCMXBeans vcinfo = JVCMXBeans.loadMXBeansFor(mbsc, cmdArgs.jvc);
               System.out.printf("%s\n", vcinfo);
               break;
            case LOG:
               System.out.printf("Getting LOG of vc=%s...\n", cmdArgs.jvc);
               vcinfo = JVCMXBeans.loadMXBeansFor(mbsc, cmdArgs.jvc);
               String logPath = vcinfo.getJvc().getLogFilePath();
               System.out.printf("logPath=%s\n", logPath);
               RandomAccessFile logFile = new RandomAccessFile(logPath, "r");
               long length = logFile.length();
               byte[] tail = new byte[4096];
               int start = 0;
               if (length > 4096) {
                  start = (int) (length - 4096);
               }
               logFile.seek(start);
               logFile.readFully(tail);
               System.out.printf("%s\n", new String(tail));
               break;
            case LIST:
               System.out.printf("Getting LIST of jvcs...\n");
               JavaContainerMXBean[] jvcs = pollMXBeans(mbsc);
               System.out.printf("VCID\tSTATUS\t\tNAME\t\tCOMMAND\n");
               for (JavaContainerMXBean jvc : jvcs) {
                  String cmd = "Missing...";
                  try {
                     ObjectName name = new ObjectName(String.format("com.waratek:type=%s,name=Runtime", jvc.getContainerName()));
                     RuntimeMXBean rt = JMX.newMXBeanProxy(mbsc, name, RuntimeMXBean.class);
                     List<String> rtargs = rt.getInputArguments();
                     cmd = rtargs.toString();
                  } catch (Exception e) {

                  }
                  System.out.printf("%d\t\t%s\t\t%s\t\t%s\n", jvc.getContainerId(), jvc.getStatus(), jvc.getContainerName(), cmd);
               }
               break;

            default:
               throw new IllegalArgumentException(String.format("Unknown command: %s", command));
         }
      } catch (Throwable e) {
         System.out.printf("Failed, %s\n", e.getMessage());
         System.out.flush();
         e.printStackTrace(System.out);
         System.out.flush();
         System.exit(1);
      }
      System.out.printf("Done");
      System.out.flush();
      System.exit(0);
   }

}
