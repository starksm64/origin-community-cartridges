package mgmt.jmx;

import cloudvm.management.*;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Represents the collection of MXBean proxies that make up the information associated with a JVC
 */
public class JVCMXBeans {
    private MBeanServerConnection mbsc;
    private JavaContainerMXBean jvc;
    private VirtualContainerInfoMXBean vcinfo;
    private VirtualMemoryMXBean memory;
    private VirtualOperatingSystemMXBean os;
    private RuntimeMXBean runtime;
    private VirtualThreadMXBean threads;
    private ThreadMXBean threadMXBean;
    private String jvcName;


    public static JVCMXBeans loadMXBeansFor(MBeanServerConnection mbsc, String vc) throws Exception {
        JVCMXBeans beans = new JVCMXBeans(vc);
        beans.setMbsc(mbsc);
        JavaContainerMXBean jvc = getJVC(mbsc, vc);
        beans.setJvc(jvc);
        VirtualContainerInfoMXBean vcinfo = getVCInfo(mbsc, vc);
        beans.setVcinfo(vcinfo);
        VirtualMemoryMXBean memory = getVCMemory(mbsc, vc);
        beans.setMemory(memory);
        VirtualOperatingSystemMXBean os = getVCOS(mbsc, vc);
        beans.setOs(os);
        RuntimeMXBean runtime = getVCRuntime(mbsc, vc);
        beans.setRuntime(runtime);
        VirtualThreadMXBean threads = getVCThreads(mbsc, vc);
        beans.setThreads(threads);
        ThreadMXBean threadMXBean = getThreadBean(mbsc);
        beans.setThreadBean(threadMXBean);
        return beans;
    }

    public static JavaContainerMXBean getJVC(MBeanServerConnection mbsc, String vc) throws Exception {
        ObjectName name = new ObjectName( String.format("com.waratek:type=%s,name=VirtualContainer", vc) );
        JavaContainerMXBean vcinfo = JMX.newMXBeanProxy(mbsc, name, JavaContainerMXBean.class);
        return vcinfo;
    }
    public static VirtualContainerInfoMXBean getVCInfo(MBeanServerConnection mbsc, String vc) throws Exception {
        ObjectName name = new ObjectName( String.format("com.waratek:type=%s,name=Info", vc) );
        VirtualContainerInfoMXBean vcinfo = JMX.newMXBeanProxy(mbsc, name, VirtualContainerInfoMXBean.class);
        return vcinfo;
    }
    public static VirtualMemoryMXBean getVCMemory(MBeanServerConnection mbsc, String vc) throws Exception {
        ObjectName name = new ObjectName( String.format("com.waratek:type=%s,name=Memory", vc) );
        VirtualMemoryMXBean vcinfo = JMX.newMXBeanProxy(mbsc, name, VirtualMemoryMXBean.class);
        return vcinfo;
    }
    public static VirtualOperatingSystemMXBean getVCOS(MBeanServerConnection mbsc, String vc) throws Exception {
        ObjectName name = new ObjectName( String.format("com.waratek:type=%s,name=OperatingSystem", vc) );
        VirtualOperatingSystemMXBean vcinfo = JMX.newMXBeanProxy(mbsc, name, VirtualOperatingSystemMXBean.class);
        return vcinfo;
    }
    public static RuntimeMXBean getVCRuntime(MBeanServerConnection mbsc, String vc) throws Exception {
        ObjectName name = new ObjectName( String.format("com.waratek:type=%s,name=Runtime", vc) );
        RuntimeMXBean vcinfo = JMX.newMXBeanProxy(mbsc, name, RuntimeMXBean.class);
        return vcinfo;
    }
    public static VirtualThreadMXBean getVCThreads(MBeanServerConnection mbsc, String vc) throws Exception {
        ObjectName name = new ObjectName( String.format("com.waratek:type=%s,name=Threading", vc) );
        VirtualThreadMXBean vcinfo = JMX.newMXBeanProxy(mbsc, name, VirtualThreadMXBean.class);
        return vcinfo;
    }
    public static ThreadMXBean getThreadBean(MBeanServerConnection mbsc) throws Exception {
        ObjectName name = new ObjectName("java.lang:type=Threading");
        ThreadMXBean bean = JMX.newMBeanProxy(mbsc, name, ThreadMXBean.class);
        return bean;
    }

    public static String getVCInfoString(VirtualContainerInfoMXBean vcinfo) {
        return String.format("VirtualContainerInfo[Name=%s, Status=%s, CpuUsage=%f(GHz-hours), Active/MaxHeap=%d/%d, "
            + "AliveThreadCount=%d, PeakThreadCount=%d, FileDescriptorCount=%d, ActiveSocketCount=%d,"
            + "NetworkBytesReceived=%d, NetworkBytesSent=%d]",
            vcinfo.getName(),
            vcinfo.getStatus(),
            vcinfo.getCpuUsage(),
            vcinfo.getUsedHeapMemory(),
            vcinfo.getMaximumHeapMemory(),
            vcinfo.getAliveThreadCount(),
            vcinfo.getPeakThreadCount(),
            vcinfo.getFileDescriptorCount(),
            vcinfo.getActiveSocketCount(),
            vcinfo.getBytesReceived(),
            vcinfo.getBytesSent()
            );
    }
    public static String getVCOSString(VirtualOperatingSystemMXBean os) {
        return String.format("VirtualOperatingSystem[AvailableProcessors=%d, ...]", os.getAvailableProcessors());
    }
    public static String getVCMemoryString(VirtualMemoryMXBean memory) {
        return String.format("VirtualMemory[HeapMemory=%d, MaximumHeapMemor=%d, ElasticGroupId=%d, ...Usages...]",
            memory.getUsedHeapMemory(),
            memory.getMaximumHeapMemorySize(),
            memory.getElasticGroupId());
    }
    public static String getVCRuntimeString(RuntimeMXBean runtime) {
        return String.format("Runtime[Uptime=%d, Args=%s, ...]",
            runtime.getUptime(),
            runtime.getInputArguments());
    }
    public static String getVCThreadsString(VirtualThreadMXBean threads) {
        return String.format("VirtualThreads[SchedulerPolicy=%s, AllThreadCpuUsage=%d, TotalCpuUsage=%d, CpuMetering=%f"
            + "ThreadPriority=%d, ThreadMaximumLimit=%d, ThreadCount=%d, DaemonThreadCount=%d]",
            threads.getSchedulerPolicy(),
            threads.getAllThreadCpuUsage(),
            threads.getTotalCpuUsage(),
            threads.getCpuMetering(),
            threads.getThreadPriority(),
            threads.getThreadMaximumLimit(),
            threads.getThreadCount(),
            threads.getDaemonThreadCount()
            );
    }
    public static String getVCThreadDumpString(MBeanServerConnection mbsc, VirtualThreadMXBean threads, ThreadMXBean threadMXBean) throws Exception {
        StringBuilder tmp = new StringBuilder("ThreadDump:\n");
        long[] ids = threads.getAllThreadIds();
        ObjectName name = new ObjectName("java.lang:type=Threading");
        Object[] args = {ids, false, false};
        String[] sig = {"[J", "boolean", "boolean"};
        CompositeData[] dump = (CompositeData[]) mbsc.invoke(name, "getThreadInfo", args, sig);
        if(dump != null) {
        for (CompositeData cd : dump) {
            if(cd == null)
                continue;
            ThreadInfo ti = ThreadInfo.from(cd);
            tmp.append(ti.toString());
            tmp.append('\n');
        }
        } else {
            tmp.append("\tUnavailable\n");
        }

        return tmp.toString();
    }
    public static String getVCThreadDumpString(ThreadMXBean threadMXBean) throws Exception {
        StringBuilder tmp = new StringBuilder("ThreadDump:\n");
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        System.out.printf("TI.count: %d\n", threadInfos.length);
        return tmp.toString();
    }

    public JVCMXBeans(String jvcName) {
        this.jvcName = jvcName;
    }

    public MBeanServerConnection getMbsc() {
        return mbsc;
    }

    public void setMbsc(MBeanServerConnection mbsc) {
        this.mbsc = mbsc;
    }

    public JavaContainerMXBean getJvc() {
        return jvc;
    }

    public void setJvc(JavaContainerMXBean jvc) {
        this.jvc = jvc;
    }

    public VirtualContainerInfoMXBean getVcinfo() {
        return vcinfo;
    }

    public void setVcinfo(VirtualContainerInfoMXBean vcinfo) {
        this.vcinfo = vcinfo;
    }

    public VirtualMemoryMXBean getMemory() {
        return memory;
    }

    public void setMemory(VirtualMemoryMXBean memory) {
        this.memory = memory;
    }

    public VirtualOperatingSystemMXBean getOs() {
        return os;
    }

    public void setOs(VirtualOperatingSystemMXBean os) {
        this.os = os;
    }

    public RuntimeMXBean getRuntime() {
        return runtime;
    }

    public void setRuntime(RuntimeMXBean runtime) {
        this.runtime = runtime;
    }

    public VirtualThreadMXBean getThreads() {
        return threads;
    }

    public void setThreads(VirtualThreadMXBean threads) {
        this.threads = threads;
    }
    public void setThreadBean(ThreadMXBean bean) {
        this.threadMXBean = bean;
    }

    @Override
    public String toString() {
        String tdump = "";
        try {
            tdump = getVCThreadDumpString(mbsc, threads, threadMXBean);
            //tdump = getVCThreadDumpString(threadMXBean);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format("JVCMXBeans for %s {\n%s\n%s\n%s\n%s\n%s\n%s\n};",
            this.jvcName,
            getVCInfoString(vcinfo),
            getVCMemoryString(memory),
            getVCOSString(os),
            getVCRuntimeString(runtime),
            getVCThreadsString(threads),
            tdump);
    }

}
