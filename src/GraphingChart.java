import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JPanel;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class GraphingChart extends JPanel implements SerialPortEventListener {
	final int HEADERSIZE = 1;
	final int CHANNELS = 1;
	final int CHANNELSIZE = 3;
	final int MESSAGESIZE = HEADERSIZE + (CHANNELS * CHANNELSIZE);
	final byte HEADER = (byte) 0b11000000;

	SerialPort serialPort;

	final int DataRecordLength = 1024;

	CircularArrayList<Byte> ringBuffer = new CircularArrayList<Byte>(MESSAGESIZE * 10);
	List<Integer> ChannelOneData = new ArrayList<Integer>(DataRecordLength);

	int fftCount = 0;
	int fftThreashold = 200;

	double[] fft = new double[DataRecordLength];
	double[] data = new double[DataRecordLength];
	double[] zeros = new double[DataRecordLength];

	/** The port we're normally going to use. */
	private static final String PORT_NAMES[] = { "/dev/tty.usbserial-A9007UX1", // Mac
			// OS
			// X
			"/dev/ttyACM0", // Raspberry Pi
			"/dev/ttyACM1", // Raspberry Pi
			"/dev/ttyUSB0", // Linux
			"COM3", // Windows
	};
	/**
	 * A BufferedReader which will be fed by a InputStreamReader converting the
	 * bytes into characters making the displayed results codepage independent
	 */
	private InputStream input;
	/** The output stream to the port */
	private OutputStream output;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 115200;

	public GraphingChart() {
		for (int i = 0; i < 1000; i++) {
			zeros[i] = 0;
			data[i] = 0;
			fft[i] = 0;
		}
	}

	public void initialize() {
		// the next line is for Raspberry Pi and
		// gets us into the while loop and was suggested here was suggested
		// http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186

		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
		System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");

		// // First, Find an instance of serial port as set in PORT_NAMES.
		// while (portEnum.hasMoreElements()) {
		// CommPortIdentifier currPortId = (CommPortIdentifier) portEnum
		// .nextElement();
		// System.out.println(currPortId.getName());
		// for (String portName : PORT_NAMES) {
		// // if (currPortId.getName().contains("ACM")) {
		// if (currPortId.getName().equals(portName)) {
		// portId = currPortId;
		// break;
		// }
		// }
		// }

		try {
			portId = CommPortIdentifier.getPortIdentifier("/dev/ttyACM0");
		} catch (NoSuchPortException e1) {
			e1.printStackTrace();
			System.out.println("Could not find COM port.");
			return;
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = serialPort.getInputStream();
			output = serialPort.getOutputStream();

			// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * This should be called when you stop using the port. This will prevent
	 * port locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	@Override
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {

				byte[] inputByte = new byte[1];
				while (input.available() > 0) {
					input.read(inputByte, 0, 1);
					ringBuffer.add(inputByte[0]);
					if ((ringBuffer.get(0) >> 4) == (HEADER >> 4)) {
						if (ringBuffer.size() >= MESSAGESIZE) {
							System.out.print("Message Header :");
							System.out.println(ringBuffer.remove(0));
							for (int i = 0; i < CHANNELS; i++) {
								System.out.print("Channel #");
								System.out.println(i);
								int value = ThreeByteInt.convertToInt(ringBuffer.remove(0), ringBuffer.remove(0),
										ringBuffer.remove(0));
								if (DataRecordLength == ChannelOneData.size()) {
									ChannelOneData.remove(0);
								}
								ChannelOneData.add(value);
								System.out.println(value);
							}
						}
					} else {
						// Must remove head element here
						System.out.print("Missed header got :");
						System.out.println(Integer.toBinaryString(ringBuffer.remove(0) & 0xFF));
					}
				}

			} catch (Exception e) {
				System.err.println(e.toString());
			}

		}
		// Ignore all the other eventTypes, but you should consider the other
		// ones.

		this.updateUI();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(500, 500);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.GREEN);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setColor(Color.BLACK);

		if (++fftCount >= fftThreashold) {
			fftCount = 0;
			calculateFFT();
		}

		int TopGraphSize = 300;

		/*
		 * if (ChannelOneData.size() > 0) { int value = ChannelOneData.get(0);
		 * g.fillRect(100, 100, value + 3, value + 3); }
		 */
		for (int i = 0; i + 1 < ChannelOneData.size(); i++) {
			int place = i;
			double value = ChannelOneData.get(i) / 1048575.0 * TopGraphSize;
			double valueNext = ChannelOneData.get(i + 1) / 1048575.0 * TopGraphSize;
			g.drawLine(place, (int) value, place + 1, (int) valueNext);
		}

		for (int i = 0; i < DataRecordLength - 1; i++) {
			int place = i;
			double value = fft[i];
			double valueNext = fft[i + 1];
			g.drawLine(place, (int) value + TopGraphSize, place + 1, (int) valueNext + TopGraphSize);
		}

	}

	private void calculateFFT() {
		for (int i = 0; i < DataRecordLength; i++) {
			if (i < ChannelOneData.size()) {
				data[i] = ChannelOneData.get(i);
			} else {
				data[i] = 0;
			}

		}
		double[] ffttemp = FFTbase.fft(data, zeros, true);
		fft = Arrays.copyOf(ffttemp, ffttemp.length);
		double maxvalue = 0, minvalue = 0;
		for (int i = 0; i < DataRecordLength; i++) {
			if (fft[i] > maxvalue)
				maxvalue = fft[i];
			if (fft[i] < minvalue)
				minvalue = fft[i];
		}
		for (int i = 0; i < DataRecordLength; i++) {
			double test1 = fft[i] - minvalue;
			double data = fft[i];
			double test = ((fft[i] - minvalue) / (maxvalue - minvalue)) * 300.0;
			fft[i] = ((fft[i] - minvalue) / (maxvalue - minvalue)) * 300.0;
		}
	}
}
