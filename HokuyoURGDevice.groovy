
import gnu.io.NRSerialPort;
import javafx.application.Platform
import javafx.scene.control.ChoiceDialog

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.ByteList;
import com.neuronrobotics.sdk.common.DeviceManager
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.common.NonBowlerDevice;
import com.neuronrobotics.sdk.util.ThreadUtil;
import java.util.ArrayList;

import com.neuronrobotics.sdk.common.ByteList;
import java.text.DecimalFormat;

// TODO: Auto-generated Javadoc
/**
 * The Class DataPoint.
 */
public class DataPoint {

	/** The range. */
	private double range;

	/** The angle. */
	private double angle;

	/**
	 * Instantiates a new data point.
	 *
	 * @param range a distance in MM
	 * @param angle angle in degrees
	 */
	public DataPoint(double range, double angle) {
		this.setRange(range);
		this.setAngle(angle);
	}

	/**
	 * Sets the range.
	 *
	 * @param range in MM
	 */
	private void setRange(double range) {
		this.range = range;
	}

	/**
	 * range in MM.
	 *
	 * @return the range
	 */
	public double getRange() {
		return range;
	}

	/**
	 * Sets the angle.
	 *
	 * @param angle current angle in degrees
	 */
	private void setAngle(double angle) {
		this.angle = angle;
	}

	/**
	 * Gets the angle.
	 *
	 * @return current angle in degrees
	 */
	public double getAngle() {
		return angle;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s="A"+new DecimalFormat("000.00 degrees ").format(angle)+":R"+range+"mm";
		return s;
	}
}

public class URG2Packet {

	/** The cmd. */
	private String cmd;

	/** The junk. */
	String junk;

	/** The status. */
	String status;

	/** The timestamp. */
	int timestamp=0;

	/** The data lines. */
	ByteList dataLines=new ByteList();

	/** The center. */
	private final int center = 384;

	/** The degrees per angle unit. */
	private final double degreesPerAngleUnit = 0.352422908;

	/** The start. */
	private int start;

	/** The end. */
	private int end;

	/** The steps per data point. */
	private int stepsPerDataPoint;

	/** The data. */
	private ArrayList<DataPoint> data=new ArrayList<DataPoint>();


	/**
	 * Instantiates a new UR g2 packet.
	 *
	 * @param line the line
	 */
	public URG2Packet(String line){
		String [] sections = line.split("\\n");//This removes the \n from the data
		setCmd(sections[0]);
		if(getCmd().contains("MD")||getCmd().contains("MS")){
			//junk = sections[1];
			status = sections[1];
			start = Integer.parseInt(getCmd().substring(2, 6));
			end   = Integer.parseInt(getCmd().substring(6, 10));
			stepsPerDataPoint = Integer.parseInt(getCmd().substring(10, 12));
			if(sections.length>2){
				String ts = new String(new ByteList(sections[2].getBytes()).getBytes(0,4));
				//timestamp = decodeURG(ts);
				for(int i=3;i<sections.length;i++){
					byte [] sec = sections[i].getBytes();
					ByteList bl = new ByteList(sec);
					int len =  sections[i].length()-1;//Remove the '\r' from the data
					dataLines.add(bl.getBytes(0, len));
				}
				int angleTicks = start;
				//System.out.println("Packet = "+line);
				//System.out.println("Data = "+dataLines.toString());
				while(dataLines.size()>2){
					int range = decodeURG(dataLines.popList(3));
					double angle = ((double)(angleTicks-center))*degreesPerAngleUnit;
					getData().add(new DataPoint(range, -1*angle));
					angleTicks+=stepsPerDataPoint;
				}
			}else {
				throw new RuntimeException("Unknown packet: "+line+" Command="+getCmd());
			}

		}else if(getCmd().contains("QT")) {
			//do nothing
		}else{
			throw new RuntimeException("Unknown packet: "+line);
		}
	}

	/**
	 * Decode urg.
	 *
	 * @param bs the bs
	 * @return the int
	 */
	public static int decodeURG(byte[] bs){
		if(bs.length!=3){
			System.err.println("URG fail: "+bs.length);
			throw new IndexOutOfBoundsException("URG decode expected 3 bytes, got: "+bs.length );
		}
		int back =0;
		byte [] d = bs;
		for(int i=0;i<d.length;i++){
			d[i]-=0x30;
			int tmp = rawByteToInt(d[i]);
			int power =(int) ((d.length-i-1)*6);
			long val = (long) (tmp * Math.pow(2, power));
			back+=val;
		}
		return back;
	}

	/**
	 * Decode urg.
	 *
	 * @param data the data
	 * @return the int
	 */
	public static int decodeURG(String data){
		System.out.println("Decoding string="+data);
		return decodeURG(data.getBytes());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		String s="Command: "+getCmd();
		s+="\nStatus: "+status;
		if(getData().size()>0){
			s+="\nStart: "+start;
			s+="\nEnd: "+end;
			s+="\nStep: "+stepsPerDataPoint;
			s+="\nTimestamp: "+timestamp;
			s+="\nData: "+getData();
			s+="\nData Size: "+getData().size();
		}
		return s;
	}


	/**
	 * Raw byte to int.
	 *
	 * @param b the b
	 * @return the int
	 */
	public static int rawByteToInt(byte b){
		int tmp =(int)b;
		if (tmp < 0){
			// This solves the Java signedness problem of "bytes"
			tmp +=256;
		}
		return tmp;
	}

	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public ArrayList<DataPoint> getData() {
		return data;
	}

	/**
	 * Gets the cmd.
	 *
	 * @return the cmd
	 */
	public String getCmd() {
		return cmd;
	}

	/**
	 * Sets the cmd.
	 *
	 * @param cmd the new cmd
	 */
	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

}

// TODO: Auto-generated Javadoc
/**
 * The Class HokuyoURGDevice.
 */
public class HokuyoURGDeviceLocal extends NonBowlerDevice{

	/** The serial. */
	private NRSerialPort serial;

	/** The ins. */
	private DataInputStream ins;

	/** The outs. */
	private DataOutputStream outs;

	/** The receive. */
	private Thread receive;

	/** The center. */
	private final int center = 384;//from datasheet

	/** The degrees per angle unit. */
	private final double degreesPerAngleUnit = 0.352422908;//from datasheet


	/** The packet. */
	private URG2Packet packet=null;

	/** The run. */
	boolean run=true;

	/** The done. */
	protected boolean done=false;

	/**
	 * Instantiates a new hokuyo urg device.
	 *
	 * @param port the port
	 */
	public HokuyoURGDeviceLocal(NRSerialPort port){
		serial=port;
	}

	/**
	 * Clear.
	 */
	public void clear() {
		send("QT\n");
	}

	/**
	 * Start sweep.
	 *
	 * @param startDeg the start deg
	 * @param endDeg the end deg
	 * @param degPerStep the deg per step
	 * @return the UR g2 packet
	 */
	public URG2Packet startSweep(double startDeg, double endDeg, double degPerStep) {
		setPacket(null);
		int tick =(int)(degPerStep/degreesPerAngleUnit);
		if (tick>99)
			tick=99;
		if(tick<1)
			tick=1;
		tick=1;//HACK
		scan(degreeToTicks(startDeg),degreeToTicks(endDeg),tick,0,1);
		ThreadUtil.wait(10);
		long start = System.currentTimeMillis();
		while(getPacket() == null ||!getPacket().getCmd().contains("MD") ){
			if(System.currentTimeMillis()-start>2000)
				break;
			ThreadUtil.wait(10);

		}
		if(getPacket()==null){
			System.err.println("Sweep failed, resetting and trying again");
			clear();
			startSweep(startDeg, endDeg, degPerStep);
		}
		System.out.print("Sweep got packet= "+getPacket());
		return getPacket();
	}

	/**
	 * Degree to ticks.
	 *
	 * @param degrees the degrees
	 * @return the int
	 */
	private int degreeToTicks(double degrees) {
		int tick =(int)(degrees/degreesPerAngleUnit)+center;
		if(tick<0)
			tick=0;
		if(tick > (center*2))
			tick=center*2;
		return tick;
	}

	/**
	 * Scan.
	 *
	 * @param startStep 	tick to start at
	 * @param endStep 		tick to end at
	 * 						Starting step and End Step can be any points between 0 and maximum step (see section 4). End Step
	 * 							should be always greater than Starting step.
	 * @param clusterCount 	Cluster Count is the number of adjacent steps that can be merged into single data and has a range 0 to
	 * 							99. When cluster count is more than 1, step having minimum measurement value (excluding error) in the
	 * 							cluster will be the output data. 
	 * @param scanInterval 	Scan Interval and
	 * 							Skipping the number of scans when obtaining multiple scan data can be set in Scan Interval. The value
	 * 							should be in decimal.
	 * @param numberOfScans User can request number of scan data by supplying the count in Number of Scan. If Number of Scan is
	 * 							set to 00 the data is supplied indefinitely unless canceled using [QT-Command] or [RS-Command].
	 * 							The value should be in decimal.
	 */
	public void scan(int startStep,int endStep,int clusterCount,int scanInterval,int numberOfScans){
		clear();
		String cmd = "MD";
		cmd+=new DecimalFormat("0000").format(startStep);
		cmd+=new DecimalFormat("0000").format(endStep);
		cmd+=new DecimalFormat("00").format(clusterCount);
		cmd+=new DecimalFormat("0").format(scanInterval);
		cmd+=new DecimalFormat("00").format(numberOfScans);
		cmd+="\n\r";
		send(cmd);
	}

	/**
	 * Send.
	 *
	 * @param data the data
	 */
	private void send(String data){
		try {
			//System.out.println("\nSending: "+data);
			outs.write(data.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Gets the packet.
	 *
	 * @return the packet
	 */
	public URG2Packet getPacket() {
		return packet;
	}

	/**
	 * Sets the packet.
	 *
	 * @param packet the new packet
	 */
	public void setPacket(URG2Packet packet) {
		this.packet = packet;
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.common.NonBowlerDevice#disconnectDeviceImp()
	 */
	@Override
	public void disconnectDeviceImp() {
		run=false;
		if(receive!=null){
			receive.interrupt();
			while(!done && receive.isAlive());
				receive=null;
		}
		try{
			if(serial.isConnected())
				serial.disconnect();
		}catch(Exception ex){}

	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.common.NonBowlerDevice#connectDeviceImp()
	 */
	@Override
	public boolean connectDeviceImp() {
		serial.connect();


		ins = new DataInputStream(serial.getInputStream());

		outs = new DataOutputStream(serial.getOutputStream());

		receive = new Thread(){
					public void run(){
						setName("HokuyoURGDevice updater");
						ByteList bl = new ByteList();
						//System.out.println("Starting listener");
						while(run && !Thread.interrupted()){
							try {
								if(ins.available()>0){
									while(ins.available()>0 && run && !Thread.interrupted()){
										int b = ins.read();
										if(b==10 && bl.get(bl.size()-1)==10){
											if(bl.size()>0){
												try{
													URG2Packet p =new URG2Packet(new String(bl.getBytes()));
													Log.debug("New Packet: \n"+p);
													setPacket(p);
													bl = new ByteList();
												}catch(Exception ex){
													setPacket(null);
													//System.out.println("Unknown packet");
													//ex.printStackTrace();
												}

											}
										}else{
											bl.add(b);
										}
										ThreadUtil.wait(1);
									}
								}else{

								}
							} catch (Exception e) {

								//e.printStackTrace();
								run=false;

							}
							try {Thread.sleep(1);} catch (InterruptedException e) {run=false;}
						}
						done=true;
					}
				};
		clear();
		receive.start();
		return serial.isConnected();
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.common.NonBowlerDevice#getNamespacesImp()
	 */
	@Override
	public ArrayList<String> getNamespacesImp() {
		// TODO Auto-generated method stub
		return new ArrayList<String>();
	}
}

Set<String> ports = NRSerialPort.getAvailableSerialPorts();
List<String> choices = new ArrayList<>();
if(ports.isEmpty()) {
	println "No device found!"
	return;
}
for (String s: ports){
	choices.add(s);
}


ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
dialog.setTitle("LIDAR Serial Port Chooser");
dialog.setHeaderText("Supports URG-04LX-UG01");
dialog.setContentText("Lidar Port:");
Platform.runLater({
	// Traditional way to get the response value.
	Optional<String> result = dialog.showAndWait();
	
	// The Java 8 way to get the response value (with lambda expression).
	result.ifPresent({letter ->
		HokuyoURGDevice p = new HokuyoURGDeviceLocal(new NRSerialPort(letter, 115200));
		p.connect();
		String name = "lidar";
		DeviceManager.addConnection(p, name);
	});
})
