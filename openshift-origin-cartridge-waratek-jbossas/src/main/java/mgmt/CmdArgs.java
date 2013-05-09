package mgmt;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * The options to set from the command line
 */
public class CmdArgs {
    public enum Command {
        CREATE,
        DESTROY,
        START,
        STOP,
        STATUS,
        SUMMARY,
        LOG,
        LIST
    };
    @Parameter(names = {"-port", "--port"}, description = "The JMX/http port for the management connection")
    public int port = 6002;
    @Parameter(names = {"-vc", "--vc", "--jvc"}, description = "The name of the Java virtual container the app is running in")
    public String jvc = "jvc-1";
    @Parameter(names = {"-jvm", "--vm", "--jvm"}, description = "The name of the global JVM daemon name")
    public String jvm = "cloud-jvm";
    @Parameter(names = {"--command", "-command"}, description = "The JMX/http port for the management connection; ")
    public Command command = Command.STATUS;
    @Parameter(names = "-urlBase", description = "The JMX/http url for the management connection")
    public String urlBase = "service:jmx:rmi:///jndi/rmi://:%d/jmxrmi";
    @Parameter(names = {"-argsTemplate", "--argsTemplate"}, description = "")
    public String argsTemplate;
    @Parameter(names = {"-jbossHome", "--jbossHome"}, description = "")
    public String jbossHome = "";
    @Parameter(description = "Additional args to pass to the create command")
    private List<String> args = new ArrayList<String>();
    @Parameter(names = {"-h", "-help", "--help"}, help = true)
    public boolean help;
    @Parameter(names = "-dryRun", description = "Only print out the parsed command, don't execute it")
    public boolean dryRun;

    public String getArgs() {
        StringBuilder tmp = new StringBuilder();
        for (String arg : args) {
            tmp.append(arg);
            tmp.append(' ');
        }
        tmp.setLength(tmp.length()-1);
        return tmp.toString();
    }
}
