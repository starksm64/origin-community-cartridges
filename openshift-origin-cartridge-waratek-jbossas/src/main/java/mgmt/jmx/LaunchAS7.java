package mgmt.jmx;

import com.beust.jcommander.JCommander;
import mgmt.CmdArgs;
import mgmt.StringPropertyReplacer;

import java.util.HashMap;
import java.util.Properties;

/**
 * Create/start a jbossas7 jvc.
 */
public class LaunchAS7 {
   public static String[] AS7 = {
      "java",
      "-D[Standalone]",
      "-server",
      "-XX:+UseCompressedOops",
      "-XX:+TieredCompilation",
      "-Xms64m",
      "-Xmx512m",
      "-XX:MaxPermSize=256m",
      "-Djava.net.preferIPv4Stack=true",
      "-Dorg.jboss.resolver.warning=true",
      "-Dsun.rmi.dgc.client.gcInterval=3600000",
      "-Dsun.rmi.dgc.server.gcInterval=3600000",
      "-Djboss.modules.system.pkgs=org.jboss.byteman",
      "-Djava.awt.headless=true",
      "-Djboss.server.default.config=standalone.xml",
      "-Dorg.jboss.boot.log.file=${jbossHome}/standalone/log/boot.log",
      "-Dlogging.configuration=file:${jbossHome}/standalone/configuration/logging.properties",
      "-jar",
      "${jbossHome}/jboss-modules.jar",
      "-mp",
      "${jbossHome}/modules",
      "-jaxpmodule",
      "javax.xml.jaxp-provider",
      "org.jboss.as.standalone",
      "-Djboss.home.dir=${jbossHome}"
   };
   public static String[] AS7_OS = {
      "java",
      "-D[Standalone]",
      "-server",
      "-XX:+UseCompressedOops",
      "-XX:+TieredCompilation",
      "${JAVA_OPTS}",
      "-Djava.net.preferIPv4Stack=true",
      "-Dorg.jboss.resolver.warning=true",
      "-Dsun.rmi.dgc.client.gcInterval=3600000",
      "-Dsun.rmi.dgc.server.gcInterval=3600000",
      "-Djboss.modules.system.pkgs=org.jboss.byteman",
      "-Djava.awt.headless=true",
      "-Djboss.server.default.config=standalone.xml",
      "-Dorg.jboss.boot.log.file=${jbossHome}/standalone/log/boot.log",
      "-Dlogging.configuration=file:${jbossHome}/standalone/configuration/logging.properties",
      "-jar",
      "${jbossHome}/jboss-modules.jar",
      "-mp",
      "${JBOSS_MODULEPATH}",
      "-jaxpmodule",
      "javax.xml.jaxp-provider",
      "org.jboss.as.standalone",
      "-Djboss.home.dir=${jbossHome}"
   };
   static HashMap<String, String[]> templateArgs = new HashMap<String, String[]>();

   static {
      templateArgs.put("AS7", AS7);
      templateArgs.put("AS7_OS", AS7_OS);

   }

   public static String getAS7Args(String template, Properties props) {
      String[] args;
      template = template.toUpperCase();
      if (templateArgs.containsKey(template) == false)
         throw new IllegalArgumentException(String.format("Unknown template type:%s; supported types are: %s", templateArgs.keySet()));

      args = templateArgs.get(template);
      StringBuilder tmp = new StringBuilder();
      for (String arg : args) {
         tmp.append(arg);
         tmp.append(' ');
      }
      tmp.setLength(tmp.length() - 1);
      String as7args = StringPropertyReplacer.replaceProperties(tmp.toString(), props);
      return as7args;
   }

   static void create(String jvc, String as7Home) throws Exception {
      String[] jargs = {
         "--command",
         "create",
         "--jvm", "cloud-jvm",
         "--jvc", jvc,
         "--args",
         ""
      };

      Properties props = new Properties();
      props.setProperty("jbossHome", as7Home);
      String as7args = getAS7Args("AS7", props);
      jargs[jargs.length - 1] = as7args;
      VCAdmin.main(jargs);
   }

   static void start(String jvc) throws Exception {
      String[] jargs = {
         "--command",
         "start",
         "--jvm", "cloud-jvm",
         "--jvc", jvc,
      };
      VCAdmin.main(jargs);
   }

   public static void main(String[] args) throws Exception {
      CmdArgs cmdArgs = new CmdArgs();
      JCommander parser = new JCommander(cmdArgs);
      parser.parse(args);
      String AS7_HOME = "/tmp/jboss-as-7.1.1.Final";
      String command = cmdArgs.command.name();
      String jvc = cmdArgs.jvc;
      if (command.equalsIgnoreCase("create"))
         create(jvc, AS7_HOME);
      else
         start(jvc);
   }

}
