package mgmt.http;

import com.beust.jcommander.JCommander;
import mgmt.CmdArgs;
import mgmt.jmx.Utils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

/**
 * Alternate version of VCAdmin that uses the jmx/http rest interface
 * TODO
 */
public class VCAdmin {
    static void dumpStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        while(line != null) {
            System.out.printf("%s\n", line);
            line = br.readLine();
        }
        br.close();
    }

    public static void main(String[] args) throws Exception {
        CmdArgs cmdArgs = new CmdArgs();
        JCommander parser = new JCommander(cmdArgs);
        parser.parse(args);
        int port = cmdArgs.port;
        String vc = cmdArgs.jvc;
        String command = cmdArgs.command.name();
        String urlBase = cmdArgs.urlBase;

        String urlRoot = String.format(urlBase, port);
        String url = urlRoot + "com.waratek:type=VirtualMachine";
        URL vmURL = new URL(url);
        // Check for VirtualMachine
        URLConnection vmConn = vmURL.openConnection();
        System.out.printf("--- VirtualMachine: %s\n", vmConn.getContentType());
        InputStream vmIS = vmConn.getInputStream();
        dumpStream(vmIS);

        // The JVC url
        url = urlRoot + String.format("com.waratek:type=%s,name=VirtualContainer", vc);
        URL jvcURL = new URL(url);
        URLConnection jvcConn = jvcURL.openConnection();
        System.out.printf("--- VirtualContainer: %s\n", vmConn.getContentType());
        InputStream jvcIS = jvcConn.getInputStream();
        dumpStream(jvcIS);

        if(command.equalsIgnoreCase("create")) {
            String tmp = cmdArgs.getArgs();
            String[] cargs = tmp.split(",");
            String dir = "/var/lib/javad/cloud-jvm/"+vc;
        } else if(command.equals("start")) {
            System.out.printf("Starting vc=%s...\n", vc);
        } else if(command.equals("STOP")) {
            System.out.printf("Stopping vc=%s...\n", vc);
        } else if(command.equals("destroy")) {
        } else if(command.equals("STATUS")) {

        } else if(command.equals("SUMMARY")) {
            System.out.printf("Getting STATUS SUMMARY of vc=%s...\n", vc);
        } else if(command.equals("LOG")) {
            System.out.printf("Getting LOG of vc=%s...\n", vc);
        } else if(command.equals("LIST")) {
            System.out.printf("Getting LIST of vcs...\n");
        } else {
            throw new IllegalArgumentException(String.format("Unknown command: %s", command));
        }
    }

}
