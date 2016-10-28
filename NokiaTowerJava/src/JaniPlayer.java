import java.util.*;

public class JaniPlayer extends Player {
  private static Map map;
  private static TheaderIni state;
  private static int[][][] populations; // time, x, y
  private static int[][][] totalPopulations; // time, tower, radius

  private static Set<Integer> myTowers; // the set of towers owned
  private static Set<Integer> towersUnderOffer; // the towers we are offering to
  private static java.util.Map<Integer, TtowerOrderRec> towerOffers; // the offers given for towers
  private static int[] numberOfTowerOffers; // the number of offers on the towers

  // otletek: tornyonkent kiszamolni range-ekre az osszlakossagot koronkent: 200 * 365 * RANGE => 0.07MB * range
  private static final int MAX_RADIUS = 100;

  private static void init(TPlayer player) {
    // initialize characteristics
    state = player.headerIni;

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

    myTowers = new HashSet<>();
    towersUnderOffer = new HashSet<>();
    towerOffers = new HashMap<>();
    numberOfTowerOffers = new int[Decl.TOWER_MAX];
  }

  public static void makeMove(TPlayer player) {
    // note using Tplayer as persistent state
    if (player.myTime == 0) {
      long t = System.currentTimeMillis();
      init(player);
      System.out.println("Initialization took " + (System.currentTimeMillis() - t) + " ms.");
    }

    System.out.println(player.myTime);
    System.out.println("time: " + player.inputData.header.time + " total pop:" + player.map.totalPop);

    stepInGame(player);
  }

  private static void makeOffer(TPlayer player, short towerID, int offer, int distance) {

  }

  // validating and removing not applied orders
  public static void validateOrders(TPlayer player) {
    TtowerOrderRec[] orders = player.outputData.orders;

    // first, validating if we gathered towers
    for (int i = 0; i < orders.length; i++) {
      if (!orders[i].leave) {
      }
    }
  }

  private static void stepInGame(TPlayer player) {
    player.myTime++;
    clearLastState(player);
  }

  private static void clearLastState(TPlayer player) {
    player.outputData.invest = 0;
    player.outputData.numOrders = 0;
  }
}
