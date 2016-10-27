public class JaniPlayer extends Player {
  private static int time;
  private static ExtendedMap map;
  private static int[][][] populations; // x, y, time

  public static void makeMove(TPlayer player) {
    if (player.myTime == 0) init(player);

    System.out.println(player.myTime);
    //if (player.headerIni != null && player.myTime == 1)
    System.out.println("time: " + player.inputData.header.time + " total pop:" + player.map.totalPop);

    stepInGame(player);
  }

  private static void stepInGame(TPlayer player) {
    player.myTime++;
    clearLastState(player);
  }

  private static void clearLastState(TPlayer player) {
    player.outputData.invest = 0;
    player.outputData.numOrders = 0;
  }

  private static void init(TPlayer player) {
    map = (ExtendedMap) player.map;
    populations = new int[Decl.MAP_SIZE][Decl.MAP_SIZE][Decl.TIME_MAX];
    populations[0] = map.pop;
    // calculating populations in advance
    for (int i = 1; i < Decl.TIME_MAX; i++) {
      populations[i] = map.popNextTime(populations[i-1]);
    }
  }
}
