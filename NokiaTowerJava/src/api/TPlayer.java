package api;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class TPlayer {
	public int ID;
	public int seed;
	public int myTime;

	public Map map = null;

	public TheaderIni headerIni = new TheaderIni();
	public TinputData inputData = new TinputData();
	public ToutputData outputData = new ToutputData();
	public TclubBonuses clubBonuses = new TclubBonuses();

	public String scriptName = "";

	private int OMax;
	private int OMut;
	private TtowerOrderRec[] OScript = new TtowerOrderRec[Decl.ORDER_MAX + 1]; // order
	private int[] Times = new int[Decl.ORDER_MAX + 1];
	private float[] IScript = new float[Decl.TIME_MAX + 1]; // invest

	public TPlayer() {
		this.ID = 0;
		this.seed = 0;
		this.myTime = 0;
	}

	public void loadScript() throws FileNotFoundException {
		if (scriptName.length() == 0)
			return;
		OMax = 0;
		OMut = 1;
		File file1 = new File("data\\" + scriptName + ".txt");
		if (file1.exists()) {
			Scanner sc = new Scanner(file1);
			while (sc.hasNextLine()) {
				OMax++;
				Times[OMax] = sc.nextInt();
				OScript[OMax].towerID = sc.nextShort();
				OScript[OMax].rentingCost = sc.nextFloat();
				int n = sc.nextInt();
				OScript[OMax].distance = sc.nextShort();
				OScript[OMax].offer = sc.nextFloat();
				OScript[OMax].leave = n > 0;
			}
			sc.close();
		}

		for (int i = 0; i <= Decl.TIME_MAX; i++)
			IScript[i] = 0;

		File file2 = new File("data\\" + scriptName + ".inv");
		if (file2.exists()) {
			int n = 0;
			Scanner sc = new Scanner(file2);
			while (sc.hasNextLine() && n < Decl.TIME_MAX) {
				n++;
				IScript[n] = sc.nextFloat();
			}
			sc.close();
		}
	}

	public void rentTower(short ID, float rentingCost, short dist, float offer) {
		if (outputData.numOrders < Decl.ORDER_MAX) {
			outputData.orders[outputData.numOrders].towerID = ID;
			outputData.orders[outputData.numOrders].rentingCost = rentingCost;
			outputData.orders[outputData.numOrders].distance = dist;
			outputData.orders[outputData.numOrders].offer = offer;
			outputData.orders[outputData.numOrders].leave = false;
			outputData.numOrders++;
		}
	}

	public void leaveTower(short ID) {
		if (outputData.numOrders < Decl.ORDER_MAX) {
			outputData.orders[outputData.numOrders].towerID = ID;
			outputData.orders[outputData.numOrders].leave = true;
			outputData.orders[outputData.numOrders].rentingCost = 0;
			outputData.orders[outputData.numOrders].distance = 0;
			outputData.orders[outputData.numOrders].offer = 0;
			outputData.numOrders++;
		}
	}

	public void changeDistanceAndOffer(short ID, short dist, float offer) {
		if (outputData.numOrders < Decl.ORDER_MAX) {
			outputData.orders[outputData.numOrders].towerID = ID;
			outputData.orders[outputData.numOrders].distance = dist;
			outputData.orders[outputData.numOrders].offer = offer;
			outputData.orders[outputData.numOrders].leave = false;
			outputData.numOrders++;
		}
	}

	public void moveFromScript() {
		outputData.invest = IScript[myTime];
		outputData.numOrders = 0;
		while ((OMut <= OMax) && (myTime >= Times[OMut])) {
			if (myTime == Times[OMut]) {
				outputData.orders[outputData.numOrders].towerID = OScript[OMut].towerID;
				outputData.orders[outputData.numOrders].rentingCost = OScript[OMut].rentingCost;
				outputData.orders[outputData.numOrders].distance = OScript[OMut].distance;
				outputData.orders[outputData.numOrders].offer = OScript[OMut].offer;
				outputData.orders[outputData.numOrders].leave = OScript[OMut].leave;
				outputData.numOrders++;
			}
			OMut++;
		}
	}

}
