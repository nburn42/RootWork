@SuppressWarnings("serial")
public class ThreeByteInt extends Number {

	public static int convertToInt(Byte b1, Byte b2, Byte b3) {
		return (b3 & 0xFF) | ((b2 & 0xFF) << 8) | ((b1 & 0x0F) << 16);
	}

	byte b1; // MSB
	byte b2;
	byte b3;

	public ThreeByteInt() {
		b1 = 0;
		b2 = 0;
		b3 = 0;
	}

	public ThreeByteInt(int input) {
		b1 = (byte) ((input << 16) & 0xFF);
		b2 = (byte) ((input << 8) & 0xFF);
		b3 = (byte) (input & 0xFF);
	}

	@Override
	public int intValue() {
		return convertToInt(b1, b2, b3);
	}

	@Override
	public long longValue() {
		return intValue();
	}

	@Override
	public float floatValue() {
		return intValue();
	}

	@Override
	public double doubleValue() {
		return intValue();
	}

}
