public class Comm {

	private static byte[] readBuffer = new byte[65500];
	private static byte[] writeBuffer = new byte[4096];
	private static int pos;

	public static void initComm() {
		int res = CommLibrary.INSTANCE.init_proc();
		if (res != 0) {
			System.out.println("ERROR: communication initialization");
			System.exit(res);
		}
	}

	public static int readIniData(TPlayer player) {
		int res = CommLibrary.INSTANCE.read_data_proc(readBuffer);
		ByteArrayToPlayerIni(player);
		return res;
	}

	public static int readData(TPlayer player) {
		int res = CommLibrary.INSTANCE.read_data_proc(readBuffer);
		ByteArrayToPlayer(player);
		return res;
	}

	public static int writeData(TPlayer player) {
		PlayerToByteArray(player);
		int res = CommLibrary.INSTANCE.write_data_proc(writeBuffer);
		return res;
	}

	private static void ByteArrayToPlayerIni(TPlayer player) {
		pos = 0;

		player.headerIni.init = readInt();
		player.headerIni.timeOut = readInt();
		player.headerIni.seed = readInt();
		player.headerIni.mapWidth = readInt();
		player.headerIni.mapHeight = readInt();
		player.headerIni.timeMax = readInt();
		player.headerIni.teamMax = readInt();
		player.headerIni.money = readDouble();
		player.headerIni.rentingMin = readDouble();
		player.headerIni.offerMaxIni = readDouble();
		player.headerIni.offerMax = readDouble();
		player.headerIni.caution = readInt();
		player.headerIni.distMin = readInt();
		player.headerIni.distMax = readInt();
		player.headerIni.dataNeed = readDouble();
		player.headerIni.dataMulti = readDouble();
		player.headerIni.dataTech = readDouble();
		player.headerIni.techMulti = readDouble();
		player.headerIni.techLevelMax = readInt();

		for (int i = 0; i <= Decl.TECH_LEVEL_MAX; i++)
			player.headerIni.techCosts[i] = readDouble();
	}

	private static void ByteArrayToPlayer(TPlayer player) {
		pos = 0;

		player.inputData.header.init = readInt();
		player.inputData.header.timeOut = readInt();
		player.inputData.header.time = readInt();
		player.inputData.header.money = readDouble();
		player.inputData.header.offerMax = readDouble();
		player.inputData.header.resPoints = readDouble();
		player.inputData.header.numTowers = readInt();
		for (int i = 0; i < Decl.TOWER_MAX; i++) {
			player.inputData.towerInf[i].owner = readShort();
			player.inputData.towerInf[i].leave = readShort();
			player.inputData.towerInf[i].licit = readFloat();
			player.inputData.towerInf[i].licitID = readShort();
			player.inputData.towerInf[i].licitDelay = readShort();
			player.inputData.towerInf[i].cust = readFloat();
			player.inputData.towerInf[i].techLevel = readShort();
			player.inputData.towerInf[i].distance = readShort();
			player.inputData.towerInf[i].offer = readFloat();
			player.inputData.towerInf[i].rentingCost = readFloat();
			player.inputData.towerInf[i].runningCost = readFloat();
		}
	}

	private static void PlayerToByteArray(TPlayer player) {
		pos = 0;
		
		writeDouble(player.outputData.invest);
		writeInt(player.outputData.numOrders);
		for (int i = 0; i < Decl.ORDER_MAX; i++) {
			writeShort(player.outputData.orders[i].towerID);
			writeFloat(player.outputData.orders[i].rentingCost);
			writeByte(player.outputData.orders[i].leave ? (byte) 1 : (byte) 0);
			writeShort(player.outputData.orders[i].distance);
			writeFloat(player.outputData.orders[i].offer);
		}
	}

	private static void writeByte(byte b) {
		writeBuffer[pos++] = b;
	}

	private static void writeShort(short s) {
		writeBuffer[pos++] = (byte) (s & 0xFF);
		writeBuffer[pos++] = (byte) ((s >>> 8) & 0xFF);
	}

	private static void writeInt(int i) {
		writeBuffer[pos++] = (byte) (i & 0xFF);
		writeBuffer[pos++] = (byte) ((i >>> 8) & 0xFF);
		writeBuffer[pos++] = (byte) ((i >>> 16) & 0xFF);
		writeBuffer[pos++] = (byte) ((i >>> 24) & 0xFF);
	}

	private static void writeLong(long l) {
		writeBuffer[pos++] = (byte) (l & 0xFF);
		writeBuffer[pos++] = (byte) ((l >>> 8) & 0xFF);
		writeBuffer[pos++] = (byte) ((l >>> 16) & 0xFF);
		writeBuffer[pos++] = (byte) ((l >>> 24) & 0xFF);
		writeBuffer[pos++] = (byte) ((l >>> 32) & 0xFF);
		writeBuffer[pos++] = (byte) ((l >>> 40) & 0xFF);
		writeBuffer[pos++] = (byte) ((l >>> 48) & 0xFF);
		writeBuffer[pos++] = (byte) ((l >>> 56) & 0xFF);
	}

	private static void writeFloat(float f) {
		writeInt(Float.floatToIntBits(f));
	}

	private static void writeDouble(double d) {
		writeLong(Double.doubleToRawLongBits(d));
	}

	private static byte readByte() {
		return readBuffer[pos++];
	}

	private static short readByteToShort() {
		return (short) (readBuffer[pos++] & 0xFF);
	}

	private static short readShort() {
		int byte1 = ((int) readBuffer[pos++]) & 0xFF;
		int byte2 = ((int) readBuffer[pos++]) & 0xFF;
		return (short) ((byte2 << 8) + (byte1));
	}

	private static int readInt() {
		int byte1 = readBuffer[pos++];
		int byte2 = readBuffer[pos++];
		int byte3 = readBuffer[pos++];
		int byte4 = readBuffer[pos++];
		return (byte4 << 24) + ((byte3 << 24) >>> 8) + ((byte2 << 24) >>> 16) + ((byte1 << 24) >>> 24);
	}

	private static long readLong() {
		long byte1 = readBuffer[pos++];
		long byte2 = readBuffer[pos++];
		long byte3 = readBuffer[pos++];
		long byte4 = readBuffer[pos++];
		long byte5 = readBuffer[pos++];
		long byte6 = readBuffer[pos++];
		long byte7 = readBuffer[pos++];
		long byte8 = readBuffer[pos++];
		return (byte8 << 56) 
				+ ((byte7 << 56) >>> 8) 
				+ ((byte6 << 56) >>> 16) 
				+ ((byte5 << 56) >>> 24)
				+ ((byte4 << 56) >>> 32) 
				+ ((byte3 << 56) >>> 40) 
				+ ((byte2 << 56) >>> 48) 
				+ ((byte1 << 56) >>> 56);
	}

	private static float readFloat() {
		return Float.intBitsToFloat(readInt());
	}

	private static double readDouble() {
		return Double.longBitsToDouble(readLong());
	}
}
