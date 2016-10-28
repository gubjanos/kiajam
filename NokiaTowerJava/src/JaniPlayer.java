import java.util.*;

public class JaniPlayer extends Player {
  private static Map map;
  private static TheaderIni state;
  private static int[][][] populations; // time, x, y
  private static int[][][] towerPopulations; // time, tower, radius initialized by method calculateTowerStatistics

  private static Set<Integer> myTowers; // the set of towers owned
  private static Set<Integer> towersUnderOffer; // the towers we are offering to
  private static java.util.Map<Integer, TtowerOrderRec> towerOffers; // the offers given for towers
  private static int[] numberOfTowerOffers; // the number of offers on the towers

  // otletek: tornyonkent kiszamolni range-ekre az osszlakossagot koronkent: 200 * 365 * RANGE => 0.07MB * range
  private static final int MAX_RADIUS_RANGE = 100;

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

    calculateTowerStatistics(player);

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

  private static void calculateTowerStatistics(TPlayer player) {
    // calculating total populations
    towerPopulations = new int[Decl.TIME_MAX][][];
    int effectiveMaxRadius = Math.min(state.distMax - state.distMin, MAX_RADIUS_RANGE); // radius counted from distmin
    int squaredMaximumDistance = effectiveMaxRadius * effectiveMaxRadius + state.distMin * state.distMin; // squared maximum distance from a tower

    for (int time = 0; time < Decl.TIME_MAX; time++) {
      towerPopulations[time] = new int[Decl.TOWER_MAX][effectiveMaxRadius];
    }

    for (int x = 0; x < Decl.MAP_SIZE; x++) {
      for (int y = 0; y < Decl.MAP_SIZE; y++) {
        for (int actualTower = 0; actualTower < Decl.TOWER_MAX; actualTower++) {
          // if a map point can not be used by a tower, skip
          int squaredDistance = MapUtils.calculateSquaredDistance(x, y, map.towers[actualTower][0], map.towers[actualTower][1]);
          if (squaredDistance < squaredMaximumDistance) continue;

          int trueDistance = (int) Math.sqrt(squaredDistance);
          for (int actualDistance = trueDistance; actualDistance < squaredMaximumDistance; actualDistance++) {
            for (int time = 0; time < Decl.TIME_MAX; time++) {
              towerPopulations[time][actualTower][actualDistance] += populations[time][x][y];
            }
          }
        }
      }
    }

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
