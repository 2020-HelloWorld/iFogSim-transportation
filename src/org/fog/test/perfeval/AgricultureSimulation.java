package org.fog.test.perfeval;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.entities.MicroserviceFogDevice;
import org.fog.entities.PlacementRequest;
import org.fog.mobilitydata.DataParser;
import org.fog.mobilitydata.RandomMobilityGenerator;
import org.fog.mobilitydata.References;
import org.fog.placement.*;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.*;

public class AgricultureSimulation {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static LocationHandler locator;

    static boolean CLOUD = false;

    static double SENSOR_TRANSMISSION_TIME = 10;

    static String Cloud_Node_Name = "cloud";

    static String Proxy_Server_Base_Name = "proxyServer";

    public static void main(String[] args) {
        try {
            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "rachana_agriculture";
            FogBroker broker = new FogBroker("broker");
            Log.printLine("Execution Complete");

            Application application =   createApplication(appId, broker.getId());
            application.setUserId(broker.getId());


        }
        catch (Exception err) {
            Log.printLine("error occurred : " + err.getLocalizedMessage());
        }


    }

    private static FogDevice createFogDevice(String nodeName, long mips,
                                             int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {

        List<Pe> peList = new ArrayList<Pe>();

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000; // host storage
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this
        // resource
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
        // devices by now

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
        return fogdevice;
    }


    public static FogDevice createCloud() {
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
        cloud.setParentId(-1);
        return cloud;
    }

    public static FogDevice createProxyServer(FogDevice cloud,String suffix) {

        if(null==suffix) {
            suffix = "0";
        }
        FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
        proxy.setParentId(cloud.getId());
        return proxy;
    }


    private static Application createApplication(String appId, int userId) {

        Application application = Application.createApplication(appId, userId);

        /*
         * Adding modules (vertices) to the application model (directed graph)
         */
        application.addAppModule("clientModule", 128, 150, 100);
        application.addAppModule("mService1", 512, 250, 200);
        application.addAppModule("mService2", 512, 350, 200);
        application.addAppModule("mService3", 2048, 450, 1000);

        /*
         * Connecting the application modules (vertices) in the application model (directed graph) with edges
         */

        application.addAppEdge("SENSOR", "clientModule", 1000, 500, "SENSOR", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("clientModule", "mService1", 2000, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mService1", "mService2", 2500, 500, "FILTERED_DATA1", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mService1", "mService3", 4000, 500, "FILTERED_DATA2", Tuple.UP, AppEdge.MODULE);

        application.addAppEdge("mService2", "clientModule", 14, 500, "RESULT1", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("mService3", "clientModule", 28, 500, "RESULT2", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("clientModule", "DISPLAY", 14, 500, "RESULT1_DISPLAY", Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge("clientModule", "DISPLAY", 14, 500, "RESULT2_DISPLAY", Tuple.DOWN, AppEdge.ACTUATOR);


        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping("clientModule", "SENSOR", "RAW_DATA", new FractionalSelectivity(0.9));
        application.addTupleMapping("mService1", "RAW_DATA", "FILTERED_DATA1", new FractionalSelectivity(1.0));
        application.addTupleMapping("mService1", "RAW_DATA", "FILTERED_DATA2", new FractionalSelectivity(1.0));
        application.addTupleMapping("mService2", "FILTERED_DATA1", "RESULT1", new FractionalSelectivity(1.0));
        application.addTupleMapping("mService3", "FILTERED_DATA2", "RESULT2", new FractionalSelectivity(1.0));
        application.addTupleMapping("clientModule", "RESULT1", "RESULT1_DISPLAY", new FractionalSelectivity(1.0));
        application.addTupleMapping("clientModule", "RESULT2", "RESULT2_DISPLAY", new FractionalSelectivity(1.0));

        application.setSpecialPlacementInfo("mService3", "cloud");
        if (CLOUD) {
            application.setSpecialPlacementInfo("mService1", "cloud");
            application.setSpecialPlacementInfo("mService2", "cloud");
        }

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("SENSOR");
            add("clientModule");
            add("mService1");
            add("mService2");
            add("clientModule");
            add("DISPLAY");
        }});

        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
        }};
        application.setLoops(loops);


//        application.createDAG();

        return application;
    }

}
