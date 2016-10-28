public class JaniPlayer extends Player {
  private static Map map;
  private static TheaderIni defaults;
  private static int[][][] populations; // time, x, y
  private static int[][][] totalPopulations; // time, tower, radius

  // otletek: tornyonkent kiszamolni range-ekre az osszlakossagot koronkent: 200 * 365 * RANGE => 0.07MB * range
  private static final int MAX_RADIUS = 100;

  public static void makeMove(TPlayer player) {
    if (player.myTime == 0) {
      long t = System.currentTimeMillis();
      init(player);
      System.out.println("Initialization took " + (System.currentTimeMillis() - t) + " ms.");
    }

    System.out.println(player.myTime);
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
    // initialize characteristics
    defaults = player.headerIni;

    map = player.map;
    populations = new int[Decl.TIME_MAX][][];
    populations[0] = map.pop;

    // calculating populations in advance
    for (int i = 1; i < Decl.TIME_MAX; i++) {
      populations[i] = MapUtils.popNextTime(populations[i-1], map);
    }

    // calculating total populations
    totalPopulations = new int[Decl.TIME_MAX][][];
    for (int i = 0; i < Decl.TIME_MAX; i++) {
      // radius
      for (int j = 0; j < MAX_RADIUS; j++) {

      }
    }
  }
}
