public class Player {

	// -------------------------------------------------------------------
	// Write your own makeMove function.
	// -------------------------------------------------------------------

	public static void makeMove(TPlayer player) {

		player.map.MapNextTime(); // next population state
		System.out.println("time: " + player.inputData.header.time + " total pop:" + player.map.totalPop);

		player.myTime++;
		player.outputData.invest = 0;
		player.outputData.numOrders = 0;

		if (player.scriptName.length() > 0) {
			player.moveFromScript();
		} else {
			if (player.inputData.header.time == 1)
				player.rentTower((short) 124, 10, (short) 35, 100);
			if (player.inputData.header.time == 5)
				player.changeDistanceAndOffer((short) 124, (short) 20, 110);
			if (player.inputData.header.time > 10 && player.inputData.header.time < 120)
				player.outputData.invest = 100;
			if (player.inputData.header.time == 140)
				player.leaveTower((short) 124);
		}
	}

}
