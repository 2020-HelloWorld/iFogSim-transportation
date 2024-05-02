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
import org.fog.placement.*;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;
public class TransportSimulation {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static LocationHandler locator;

    static boolean CLOUD = false;

    static double SENSOR_TRANSMISSION_TIME = 10;

    static String Cloud_Node_Name = "cloud";

    static String Proxy_Server_Base_Name = "proxyServer";

    static String Biometric_Device_Name = "BIOMETRIC";
    static String Ptz_Sensor_Name  = "PTZ";

    static String Biometric_Sensor_Type = "CameraSensor";

    static String Biometric_Tuple_Type = "BIOMETRIC";
    
    static String Biometric_Analyzer = "Biometric_Analyzer";
    
    static String Biometric_Analyzer_Output = "BIOMETRIC_MEASURES";

    static String Ptz_Actuator_Type = "PTZ_CONTROL";

    static String Router_Device_Name = "Router";
    static int Biometric_Per_Router = 1;

    static int totalTransportationUnits = 1;
    static int ProximitySensor_Per_Router = 1;
    static String Proximity_Device_Name = "PROXIMITY";
    static String Proximity_Sensor_Name = "PROXIMITY_SENSOR";

    static String Proximity_Tuple_Type = "PROXIMITY";

    //
    static int Accelerometer_Per_Router = 1;
    static String Accelerometer_Device_Name = "ACCELEROMETER";
    static String Accelerometer_Sensor_Name = "ACCELEROMETER_SENSOR";

    static String High_Acceleration_Detector = "ACCELERATION_Analyzer";

    static String High_Acceleration_Detector_Output = "Acceleration_Analyzer_Output";

    static String Acceleration_Tuple_Type = "ACCELEROMETER";

    //
    static String Proximity_Detector = "proximity_detector";
    static  String Proximity_Detector_Output = "PROXIMITY_DETECTED";

    static String Forwarder_Temp = "Forwarder";

    static String Motion_Tracker = "MOTION_TRACKER";

    static String Motion_Tracker_Output = "TRACKER_OUTPUT";
    static String Motion_Analyzer_Output = "ANALYSIS";
    static String Motion_Analyzer = "Motion_Analyzer";

    //
    static int Smoke_Per_Router = 1;
    static String SmokeDetector_Device_Name = "SMOKE";
    static String Smoke_Sensor_Name = "SMOKE_SENSOR";

    static String FIRE_ANALYZER = "Fire_Analyzer";

    static String FIRE_ANALYZER_OUTPUT = "Fire_Alert";

    static String Smoke_Tuple_Type = "SMOKE";

    static String Forwarder_Smoke = "Forwarder_Smoke";
    //


    //
    static int Cam_Per_Router = 1;
    static String Camera_Device_Name = "CAMERA";
    static String Camera_Sensor_Name = "CAMERA_SENSOR";

    static String Image_Processor = "Image_Processor";

    static String Object_Detetion_Output = "Identified_Object";

    static String Camera_Tuple_Type = "CAMERA";

    static String Forwarder_Cam = "Forwarder_Cam";
    //


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
                moduleMapping.addModuleToDevice("biometric_detector", "cloud"); // placing all instances of Object Detector module in the Cloud
                moduleMapping.addModuleToDevice(Biometric_Analyzer,"cloud");
                moduleMapping.addModuleToDevice(Proximity_Detector,"cloud");
                moduleMapping.addModuleToDevice(Motion_Tracker_Output,"cloud");
                moduleMapping.addModuleToDevice(Proximity_Detector,"cloud");
                moduleMapping.addModuleToDevice(Image_Processor,"cloud");
//                moduleMapping.addModuleToDevice(Forwarder_WA,"cloud");
                moduleMapping.addModuleToDevice(High_Acceleration_Detector,"cloud");// placing all instances of Object Tracker module in the Cloud
//                moduleMapping.addModuleToDevice(Forwarder_Temp,"cloud");
                moduleMapping.addModuleToDevice(FIRE_ANALYZER,"cloud");
//                moduleMapping.addModuleToDevice(Forwarder_Smoke,"cloud");



            }
            else {
                for(FogDevice device : fogDevices) {
                    // adding motion detecting functionality (computations) directly inside camera
                    String deviceName = device.getName();
                    String suffix = deviceName!=null?deviceName.split("_")[0]:deviceName;
                    if(Biometric_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice("biometric_detector", deviceName);
                    }
                    if(Proximity_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice(Proximity_Detector,deviceName);
                    }
                    if(Accelerometer_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice(High_Acceleration_Detector,deviceName);
                    }
                    if(SmokeDetector_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice(FIRE_ANALYZER,deviceName);
                    }
                    if(Camera_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice(Image_Processor,deviceName);
                    }
                    if(Router_Device_Name.equals(suffix)) {
                        moduleMapping.addModuleToDevice(Biometric_Analyzer,deviceName);
                        moduleMapping.addModuleToDevice(Motion_Tracker,deviceName);
                        moduleMapping.addModuleToDevice(Forwarder_Temp,deviceName);
                        moduleMapping.addModuleToDevice(Forwarder_Smoke,deviceName);
                        moduleMapping.addModuleToDevice(Forwarder_Cam,deviceName);
                    }
                    if(Proxy_Server_Base_Name.equals(deviceName)) {
                        moduleMapping.addModuleToDevice(Motion_Analyzer,deviceName);
                        moduleMapping.addModuleToDevice(Forwarder_Temp,deviceName);
                        moduleMapping.addModuleToDevice(Forwarder_Smoke,deviceName);
                        moduleMapping.addModuleToDevice(Forwarder_Cam,deviceName);
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
        application.addAppModule(Biometric_Analyzer, 10);
        application.addAppModule("biometric_detector", 10);

        application.addAppModule("user_interface", 10);
        application.addAppModule("user_interface_pir",10);
        application.addAppModule(Proximity_Detector,10);
        // application.addAppModule(Forwarder,10);
        application.addAppModule(Motion_Tracker,10);
        application.addAppModule(Motion_Analyzer,10);


        /*
         * Connecting the application modules (vertices) in the application model (directed graph) with edges
         */
        application.addAppEdge(Biometric_Device_Name, "biometric_detector", 1000, 20000, Biometric_Tuple_Type, Tuple.UP, AppEdge.SENSOR);
        // application.addAppEdge(Camera_Device_Name,Motion_Detector,1000,2000,"PIR_RAW_DATA",Tuple.UP,AppEdge.SENSOR);
        // adding edge from CAMERA (sensor) to Motion Detector module carrying tuples of type CAMERA
        application.addAppEdge("biometric_detector", Biometric_Analyzer, 2000, 2000, "MEASURES", Tuple.UP, AppEdge.MODULE); // adding edge from Motion Detector to Object Detector module carrying tuples of type MOTION_VIDEO_STREAM
        application.addAppEdge(Biometric_Analyzer, "user_interface", 500, 2000, "BIOMETRIC_DETAILS", Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to User Interface module carrying tuples of type DETECTED_OBJECT





        application = addSecondarySensor(application, Camera_Device_Name, Camera_Tuple_Type, Image_Processor, Object_Detetion_Output, Forwarder_Cam);
        application = addSecondarySensor(application,SmokeDetector_Device_Name,Smoke_Tuple_Type,FIRE_ANALYZER,FIRE_ANALYZER_OUTPUT,Forwarder_Smoke);
        application = addSecondarySensor(application, Accelerometer_Device_Name, Acceleration_Tuple_Type, High_Acceleration_Detector, High_Acceleration_Detector_Output,Forwarder_Temp);




        application.addAppEdge(Proximity_Device_Name, Proximity_Detector,1000,200, Proximity_Tuple_Type,Tuple.UP,AppEdge.SENSOR);
        application.addAppEdge(Proximity_Detector,Motion_Tracker,500,200, Proximity_Detector_Output,Tuple.UP, AppEdge.MODULE);
        application.addAppEdge(Motion_Tracker,Motion_Analyzer,1000,100,Motion_Tracker_Output,Tuple.UP,AppEdge.MODULE);
        application.addAppEdge(Motion_Analyzer,"user_interface_pir",1000,200,Motion_Analyzer_Output,Tuple.UP,AppEdge.MODULE);
        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping("biometric_detector", Biometric_Tuple_Type, "MEASURES", new FractionalSelectivity(1.0)); // 1.0 tuples of type MOTION_VIDEO_STREAM are emitted by Motion Detector module per incoming tuple of type CAMERA
        application.addTupleMapping(Biometric_Analyzer, "MEASURES", "BIOMETRIC_DETAILS", new FractionalSelectivity(0.05)); // 0.05 tuples of type MOTION_VIDEO_STREAM are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM

        application.addTupleMapping(Proximity_Detector, Proximity_Tuple_Type, Proximity_Detector_Output,new FractionalSelectivity(0.05));
        application.addTupleMapping(Motion_Tracker, Proximity_Detector_Output,Motion_Tracker_Output,new FractionalSelectivity(1.0));
        application.addTupleMapping(Motion_Analyzer,Motion_Tracker_Output,Motion_Analyzer_Output,new FractionalSelectivity(1.0));
        // application.addTupleMapping(Forwarder,Motion_Detector_Output,Motion_Detector_Output,new FractionalSelectivity(1.0));



        /*
         * Defining application loops (maybe incomplete loops) to monitor the latency of.
         * Here, we add two loops for monitoring : Motion Detector -> Object Detector -> Object Tracker and Object Tracker -> PTZ Control
         */
        final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("biometric_detector");add(Biometric_Analyzer);add("object_tracker");}});

        final AppLoop loop3 = new AppLoop(new ArrayList<String>(){{add(Proximity_Detector);add(Motion_Tracker);add(Proximity_Detector);}});
        final AppLoop loop4 = new AppLoop(new ArrayList<String>(){{add(High_Acceleration_Detector);add(Forwarder_Temp);}});

        final AppLoop loop5 = new AppLoop(new ArrayList<String>(){{add(FIRE_ANALYZER);add(Forwarder_Smoke);}});

        final AppLoop loop6 = new AppLoop(new ArrayList<String>(){{add(Image_Processor);add(Forwarder_Cam);}});



        List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop3);add(loop4);add(loop5);add(loop6);}};

        application.setLoops(loops);
        return application;
    }

    public static Application addSecondarySensor(Application application,String sensorName,String sensorTupleType,String processor,String processorOutput,String forwarder) {
        // adding module
        application.addAppModule(processor,10);
        application.addAppModule(forwarder,10);

        // adding edge
        application.addAppEdge(sensorName,processor,2000,100,sensorTupleType,Tuple.UP,AppEdge.SENSOR);
        application.addAppEdge(processor,"user_interface",2000,2000,processorOutput,Tuple.UP,AppEdge.MODULE);
        application.addAppEdge(processor,forwarder,2000,2000,processorOutput,Tuple.UP,AppEdge.MODULE);
        application.addAppEdge(forwarder,forwarder,2000,2000,processorOutput,Tuple.UP,AppEdge.MODULE);
        application.addAppEdge(forwarder,"user_interface",2000,200,processorOutput,Tuple.UP,AppEdge.MODULE);



        application.addTupleMapping(processor,sensorTupleType,processorOutput,new FractionalSelectivity(0.05));
        application.addTupleMapping(forwarder,processorOutput,processorOutput,new FractionalSelectivity(1.0));

        return application;

    }

//    public static Application addFogDeviceToApplication(Application application) {
//
//    }
    public static FogDevice createBiometric(int deviceNum, String appId, int userId, int parentId) {
        FogDevice camera = createFogDevice(Biometric_Device_Name + "_" + deviceNum, 500, 1000, 10000, 10000, 3, 0.005, 87.53, 82.44);
        camera.setParentId(parentId);
        Sensor sensor = new Sensor(Biometric_Sensor_Type + "_"+deviceNum, Biometric_Tuple_Type, userId, appId, new DeterministicDistribution(5));

        sensors.add(sensor);


        sensor.setGatewayDeviceId(camera.getId());
        sensor.setLatency(1.0);
        return camera;
    }

    public static FogDevice createProximityDevice(int deviceNum, String appId, int userId, int parentId) {
        FogDevice Pir = createFogDevice(Proximity_Device_Name + "_" + deviceNum, 500, 1000, 10000, 10000, 3, 0.005, 87.53, 82.44);
        Pir.setParentId(parentId);
        Sensor sensor = new Sensor(Proximity_Sensor_Name + "_"+deviceNum, Proximity_Tuple_Type, userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
        sensors.add(sensor);
        sensor.setGatewayDeviceId(Pir.getId());
        sensor.setLatency(1.0);  
        return Pir;
    }


    public static FogDevice createAccelerometer(int deviceNum, String appId, int userId, int parentId) {
        FogDevice temp = createFogDevice(Accelerometer_Device_Name + "_" + deviceNum, 500, 1000, 10000, 10000, 3, 0.005, 87.53, 82.44);
        temp.setParentId(parentId);
        Sensor sensor = new Sensor(Accelerometer_Sensor_Name + "_"+deviceNum, Acceleration_Tuple_Type, userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
        sensors.add(sensor);
        sensor.setGatewayDeviceId(temp.getId());
        sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
        return temp;
    }

    public static FogDevice createSmokeSensor(int deviceNum,String appId,int userId,int parentId) {
        FogDevice smokedetector = createFogDevice(SmokeDetector_Device_Name + "_" + deviceNum, 500, 100, 10000, 10000, 3, 0.005, 87.53, 82.44);
        smokedetector.setParentId(parentId);
        Sensor sensor = new Sensor(Smoke_Sensor_Name + "_"+deviceNum,Smoke_Tuple_Type, userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
        sensors.add(sensor);
        sensor.setGatewayDeviceId(smokedetector.getId());
        sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
        return smokedetector;
    }

    public static FogDevice createCamera(int deviceNum, String appId, int userId, int parentId) {
        FogDevice WaterAndAirQualityDetector = createFogDevice(Camera_Device_Name + "_" + deviceNum, 500, 1000, 10000, 10000, 3, 0.005, 87.53, 82.44);
        WaterAndAirQualityDetector.setParentId(parentId);
        Sensor sensor = new Sensor(Camera_Sensor_Name + "_"+deviceNum, Camera_Tuple_Type, userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
        sensors.add(sensor);
        sensor.setGatewayDeviceId(WaterAndAirQualityDetector.getId());
        sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
        return WaterAndAirQualityDetector;
    }
    public static void createAndPopulateRouter(int deviceNum,String appId,int userId,int parentId) {
        FogDevice router = createFogDevice(Router_Device_Name+"_"+deviceNum, 2800, 4000, 10000, 10000, 2, 0.005, 107.339, 83.4333);
        router.setParentId(parentId);
        fogDevices.add(router);
        for (int i = 0; i< Biometric_Per_Router; i++) {
            FogDevice cam = createBiometric(i,appId,userId,router.getId());

            cam.setUplinkLatency(2);
            fogDevices.add(cam);
        }
        for (int j = 0; j< ProximitySensor_Per_Router; j++) {
            FogDevice pir = createProximityDevice(j,appId,userId, router.getId());
            pir.setUplinkLatency(2);
            fogDevices.add(pir);
        }
        for (int k = 0; k< Accelerometer_Per_Router; k++) {
            FogDevice temp = createAccelerometer(k,appId,userId, router.getId());
            temp.setUplinkLatency(2);
            fogDevices.add(temp);
        }
        for (int m=0;m<Smoke_Per_Router;m++) {
            FogDevice smoke_detector = createSmokeSensor(m,appId,userId, router.getId());
            smoke_detector.setUplinkLatency(2);
            fogDevices.add(smoke_detector);
        }
        for (int n = 0; n< Cam_Per_Router; n++) {
            FogDevice smoke_detector = createCamera(n,appId,userId, router.getId());
            smoke_detector.setUplinkLatency(2);
            fogDevices.add(smoke_detector);
        }
    }

    public static void setUpIotDevices(String appId,int userId) {
        FogDevice cloudServer = createCloud();
        fogDevices.add(cloudServer);
        FogDevice proxyServer = createProxyServer(cloudServer.getId());
        fogDevices.add(proxyServer);
        for (int i = 0; i< totalTransportationUnits; i++) {
            createAndPopulateRouter(i,appId,userId, proxyServer.getId());
        }
    }
}

class deviceSetting {
    private String deviceType;
    private int numOfDevice;

    // Getter for deviceType
    public String getDeviceType() {
        return deviceType;
    }

    // Setter for deviceType
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    // Getter for numOfDevice
    public int getNumOfDevice() {
        return numOfDevice;
    }

    // Setter for numOfDevice
    public void setNumOfDevice(int numOfDevice) {
        this.numOfDevice = numOfDevice;
    }
}
