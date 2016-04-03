import gnu.io.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;

public class adsinterface {
	SerialPort serialPort;
	InputStream input;
	OutputStream output;

	public adsinterface(SerialPort s, InputStream input, OutputStream output) {
		serialPort = s;
		this.input = input;
		this.output = output;
	}

}
