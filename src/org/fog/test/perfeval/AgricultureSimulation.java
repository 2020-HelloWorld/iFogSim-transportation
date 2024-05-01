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
import org.fog.gui.dialog.AddAppEdge;
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

    static String Camera_Device_Name = "CAMERA";
    static String Ptz_Sensor_Name  = "PTZ";

    static String Camera_Sensor_Name = "CameraSensor";

    static String Camera_Tuple_Type = "CAMERA";

    static String Ptz_Actuator_Type = "PTZ_CONTROL";

    static String Router_Device_Name = "Router";
    static int Cam_Per_Router = 2;

    static int totalArea = 1;
    static int Pir_Per_Router = 1;
    static String Pir_Device_Name = "PIR";
    static String Pir_Sensor_Name = "PIR_SENSOR";

    static String Pir_Tuple_Type = "PIR";

    //
    static int Temp_Per_Router = 1;
    static String Temp_Device_Name = "TEMP";
    static String Temp_Sensor_Name = "Temperature_SENSOR";

    static String High_Temperature_Detector = "Temperature_Analyzer";

    static String High_Temperature_Detector_Output = "Temperature_Analyzer_Output";

    static String Temp_Tuple_Type = "TEMP";

    //
    static String Motion_Detector = "motion_detector";
    static  String Motion_Detector_Output = "MOTION_DETECTED";

    static String Forwarder_Temp = "Forwarder";

    static String Motion_Tracker = "MOTION_TRACKER";

    static String Motion_Tracker_Output = "TRACKER_OUTPUT";
    static String Motion_Analyzer_Output = "ANALYSIS";
    static String Motion_Analyzer = "Motion_Analyzer";


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
            setUpIotDevices(appId, broker.getId());

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice("user_interface", "cloud");
            moduleMapping.addModuleToDevice("user_interface_pir", "cloud");

            if(CLOUD) {
                moduleMapping.addModuleToDevice("image_processor", "cloud"); // placing all instances of Object Detector module in the Cloud
                moduleMapping.addModuleToDevice("object_detector","cloud");
                moduleMapping.addModuleToDevice(Motion_Detector,"cloud");
                moduleMapping.addModuleToDevice(Motion_Tracker_Output,"cloud");
                moduleMapping.addModuleToDevice(Motion_Detector,"cloud");
                moduleMapping.addModuleToDevice("object_tracker", "cloud");
                moduleMapping.addModuleToDevice(High_Temperature_Detector,"cloud");// placing all instances of Object Tracker module in the Cloud
                moduleMapping.addModuleToDevice(Forwarder_Temp,"cloud");
            }
            else {
                for(FogDevice device : fogDevices) {
                    // adding motion detecting functionality (computations) directly inside camera
                    String deviceName = device.getName();
                    String suffix = deviceName!=null?deviceName.split("_")[0]:deviceName;
                    if(Camera_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice("image_processor", deviceName);
                    }
                    if(Pir_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice(Motion_Detector,deviceName);
                    }
                    if(Temp_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice(High_Temperature_Detector,deviceName);
                    }
                    if(Router_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice("object_detector",deviceName);
                        moduleMapping.addModuleToDevice(Motion_Tracker,deviceName);
                        moduleMapping.addModuleToDevice(Forwarder_Temp,deviceName);
                    }
                    if(Proxy_Server_Base_Name.equals(deviceName)) {
                        moduleMapping.addModuleToDevice("object_tracker",deviceName);
                        moduleMapping.addModuleToDevice(Motion_Analyzer,deviceName);
                        moduleMapping.addModuleToDevice(Forwarder_Temp,deviceName);
                    }

                }
            }
            Controller controller = null;
            controller = new Controller("master-controller", fogDevices, sensors,
                    actuators);

            controller.submitApplication(application,
                    (CLOUD)?(new ModulePlacementMapping(fogDevices, application, moduleMapping))
                            :(new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            CloudSim.stopSimulation();
            Log.printLine("Comepleted");


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

    public static FogDevice createProxyServer(int parentId) {

        FogDevice proxy = createFogDevice(Proxy_Server_Base_Name, 2800, 4000, 10000, 10000, 1, 0.01, 107.339, 83.4333);
        proxy.setParentId(parentId);
        return proxy;
    }


    /**
     * Function to create the Intelligent Surveillance application in the DDF model.
     * @param appId unique identifier of the application
     * @param userId identifier of the user of the application
     * @return
     */
    @SuppressWarnings({"serial" })
    private static Application createApplication(String appId, int userId){

        Application application = Application.createApplication(appId, userId);
        /*
         * Adding modules (vertices) to the application model (directed graph)
         */
        application.addAppModule("object_detector", 10);
        application.addAppModule("image_processor", 10);
        application.addAppModule("object_tracker", 10);
        application.addAppModule("user_interface", 10);
        application.addAppModule("user_interface_pir",10);
        application.addAppModule(Motion_Detector,10);
        // application.addAppModule(Forwarder,10);
        application.addAppModule(Motion_Tracker,10);
        application.addAppModule(Motion_Analyzer,10);
        application.addAppModule(Forwarder_Temp,10);
        application.addAppModule(High_Temperature_Detector,10);


        /*
         * Connecting the application modules (vertices) in the application model (directed graph) with edges
         */
        application.addAppEdge(Camera_Device_Name, "image_processor", 1000, 20000, "CAMERA", Tuple.UP, AppEdge.SENSOR);
        // application.addAppEdge(Camera_Device_Name,Motion_Detector,1000,2000,"PIR_RAW_DATA",Tuple.UP,AppEdge.SENSOR);
        // adding edge from CAMERA (sensor) to Motion Detector module carrying tuples of type CAMERA
        application.addAppEdge("image_processor", "object_detector", 2000, 2000, "MOTION_VIDEO_STREAM", Tuple.UP, AppEdge.MODULE); // adding edge from Motion Detector to Object Detector module carrying tuples of type MOTION_VIDEO_STREAM
        application.addAppEdge("object_detector", "user_interface", 500, 2000, "DETECTED_OBJECT", Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to User Interface module carrying tuples of type DETECTED_OBJECT
        application.addAppEdge("object_detector", "object_tracker", 1000, 100, "OBJECT_LOCATION", Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to Object Tracker module carrying tuples of type OBJECT_LOCATION
        application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", Tuple.DOWN, AppEdge.ACTUATOR); // adding edge from Object Tracker to PTZ CONTROL (actuator) carrying tuples of type PTZ_PARAMS

        application.addAppEdge(Temp_Device_Name,High_Temperature_Detector,2000,100,Temp_Tuple_Type,Tuple.UP,AppEdge.SENSOR);
        application.addAppEdge(High_Temperature_Detector,Forwarder_Temp,2000,2000,High_Temperature_Detector_Output,Tuple.UP,AppEdge.MODULE);
        application.addAppEdge(Forwarder_Temp,Forwarder_Temp,2000,2000,High_Temperature_Detector_Output,Tuple.UP,AppEdge.MODULE);
        application.addAppEdge(Forwarder_Temp,"user_interface",2000,200,High_Temperature_Detector_Output,Tuple.UP,AppEdge.MODULE);

        application.addAppEdge(Pir_Device_Name,Motion_Detector,1000,200,Pir_Tuple_Type,Tuple.UP,AppEdge.SENSOR);
        application.addAppEdge(Motion_Detector,Motion_Tracker,500,200,Motion_Detector_Output,Tuple.UP, AppEdge.MODULE);
        application.addAppEdge(Motion_Tracker,Motion_Analyzer,1000,100,Motion_Tracker_Output,Tuple.UP,AppEdge.MODULE);
        application.addAppEdge(Motion_Analyzer,"user_interface_pir",1000,200,Motion_Analyzer_Output,Tuple.UP,AppEdge.MODULE);
        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping("image_processor", "CAMERA", "MOTION_VIDEO_STREAM", new FractionalSelectivity(1.0)); // 1.0 tuples of type MOTION_VIDEO_STREAM are emitted by Motion Detector module per incoming tuple of type CAMERA
        application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "OBJECT_LOCATION", new FractionalSelectivity(1.0)); // 1.0 tuples of type OBJECT_LOCATION are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM
        application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECT", new FractionalSelectivity(0.05)); // 0.05 tuples of type MOTION_VIDEO_STREAM are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM

        application.addTupleMapping(Motion_Detector,Pir_Tuple_Type,Motion_Detector_Output,new FractionalSelectivity(0.05));
        application.addTupleMapping(Motion_Tracker,Motion_Detector_Output,Motion_Tracker_Output,new FractionalSelectivity(1.0));
        application.addTupleMapping(Motion_Analyzer,Motion_Tracker_Output,Motion_Analyzer_Output,new FractionalSelectivity(1.0));
        // application.addTupleMapping(Forwarder,Motion_Detector_Output,Motion_Detector_Output,new FractionalSelectivity(1.0));

        application.addTupleMapping(High_Temperature_Detector,Temp_Tuple_Type,High_Temperature_Detector_Output,new FractionalSelectivity(0.05));
        application.addTupleMapping(Forwarder_Temp,High_Temperature_Detector,High_Temperature_Detector_Output,new FractionalSelectivity(1.0));

        /*
         * Defining application loops (maybe incomplete loops) to monitor the latency of.
         * Here, we add two loops for monitoring : Motion Detector -> Object Detector -> Object Tracker and Object Tracker -> PTZ Control
         */
        final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("image_processor");add("object_detector");add("object_tracker");}});
        final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("object_tracker");add("PTZ_CONTROL");}});

       final AppLoop loop3 = new AppLoop(new ArrayList<String>(){{add(Motion_Detector);add(Motion_Tracker);add(Motion_Detector);}});
        final AppLoop loop4 = new AppLoop(new ArrayList<String>(){{add(High_Temperature_Detector);add(Forwarder_Temp);}});


        List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);add(loop3);add(loop4);}};

        application.setLoops(loops);
        return application;
    }
   public static FogDevice createCamera(int deviceNum,String appId,int userId,int parentId) {
       FogDevice camera = createFogDevice(Camera_Device_Name + "_" + deviceNum, 500, 1000, 10000, 10000, 3, 0.005, 87.53, 82.44);
       camera.setParentId(parentId);
       Sensor sensor = new Sensor(Camera_Sensor_Name + "_"+deviceNum, Camera_Tuple_Type, userId, appId, new DeterministicDistribution(5));
       // Sensor Pir_sensor = new Sensor(Pir_Sensor_Name+"_"+deviceNum,"PIR_RAW_DATA",userId,appId,new DeterministicDistribution(5));
       // inter-transmission time of camera (sensor) follows a deterministic distribution
       sensors.add(sensor);


       Actuator ptz = new Actuator(Ptz_Sensor_Name+"_" + deviceNum, userId, appId, Ptz_Actuator_Type);
       actuators.add(ptz);
       sensor.setGatewayDeviceId(camera.getId());
       sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms

       ptz.setGatewayDeviceId(camera.getId());
       ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms
       return camera;
   }

    public static FogDevice createPir(int deviceNum,String appId,int userId,int parentId) {
        FogDevice Pir = createFogDevice(Pir_Device_Name + "_" + deviceNum, 500, 1000, 10000, 10000, 3, 0.005, 87.53, 82.44);
        Pir.setParentId(parentId);
        Sensor sensor = new Sensor(Pir_Sensor_Name + "_"+deviceNum,Pir_Tuple_Type, userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
        sensors.add(sensor);
        sensor.setGatewayDeviceId(Pir.getId());
        sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
        return Pir;
    }


    public static FogDevice createTemperatureSensor(int deviceNum,String appId,int userId,int parentId) {
        FogDevice temp = createFogDevice(Temp_Device_Name + "_" + deviceNum, 500, 1000, 10000, 10000, 3, 0.005, 87.53, 82.44);
        temp.setParentId(parentId);
        Sensor sensor = new Sensor(Temp_Sensor_Name + "_"+deviceNum,Temp_Tuple_Type, userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
        sensors.add(sensor);
        sensor.setGatewayDeviceId(temp.getId());
        sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
        return temp;
    }
   public static void createAndPopulateRouter(int deviceNum,String appId,int userId,int parentId) {
       FogDevice router = createFogDevice(Router_Device_Name+"_"+deviceNum, 2800, 4000, 10000, 10000, 2, 0.005, 107.339, 83.4333);
       router.setParentId(parentId);
       fogDevices.add(router);
       for (int i=0;i<Cam_Per_Router;i++) {
           FogDevice cam = createCamera(i,appId,userId,router.getId());

           cam.setUplinkLatency(2);
           fogDevices.add(cam);
       }
       for (int j=0;j<Pir_Per_Router;j++) {
           FogDevice pir = createPir(j,appId,userId, router.getId());
           pir.setUplinkLatency(2);
           fogDevices.add(pir);
       }
       for (int k=0;k<Temp_Per_Router;k++) {
           FogDevice temp = createTemperatureSensor(k,appId,userId, router.getId());
           temp.setUplinkLatency(2);
           fogDevices.add(temp);
       }
   }

   public static void setUpIotDevices(String appId,int userId) {
        FogDevice cloudServer = createCloud();
        fogDevices.add(cloudServer);
        FogDevice proxyServer = createProxyServer(cloudServer.getId());
        fogDevices.add(proxyServer);
        for (int i=0;i<totalArea;i++) {
            createAndPopulateRouter(i,appId,userId, proxyServer.getId());
        }
   }

}
