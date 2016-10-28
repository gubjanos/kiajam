import java.util.*;

public class JaniPlayer extends Player {
  private static Map map;
  private static TheaderIni state; // TODO: this is an initialized state, check when it has to be updated
  private static int[][][] populations; // time, x, y
  private static int[][][] towerPopulations; // time, tower, radius initialized by method calculateTowerStatistics

  private static double[] dataNeedInTime; // time, the data need factor in time
  private static int dataTechnology = 1;

  private static int effectiveMaxRadius;

  private static Set<Short> myTowers; // the set of towers owned
  private static Set<Short> towersUnderOffer; // the towers we are offering to
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

    // calculating data need increasement
    dataNeedInTime[0] = 1.0d;
    for (int i = 1; i < Decl.TIME_MAX; i++) {
      dataNeedInTime[i] = dataNeedInTime[i-1] * state.dataMulti;
    }

    calculateTowerStatistics();

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

  // NOTE overlapping towers not taken into consideration
  private static void calculateTowerStatistics() {
    // calculating total populations
    towerPopulations = new int[Decl.TIME_MAX][][];
    effectiveMaxRadius = Math.min(state.distMax - state.distMin, MAX_RADIUS_RANGE); // radius counted from distmin
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

  public static class TowerUtils {
    // calculate
    // maximum distance for a tower wrt data limit
    // if a tower could not do any production distMin-1 is returned
    public static short maximumDistance(short towerID, double dataTech, int time) {
      for (int i = 0; i < effectiveMaxRadius; i++) {
        double dataNeed = towerPopulations[time][towerID][i] * dataNeedInTime[time];
        if (dataNeed > dataTech) return (short)(state.distMin + i - 1);
      }

      return (short)(effectiveMaxRadius + state.distMin);
    }

    // maximum revenue
    // if there is no way to generate revenue at a tower with current technology returns -1
    public static double maximumRevenue(short towerID, double dataTech, int time) {
      return revenueOfTower(towerID, dataTech, time, state.offerMax);
    }

    // revenue with a given offer level
    // if there is no way to generate revenue at a tower with current technology returns -1
    // if the offer is greater then the maximum givable, also returns -1
    public static double revenueOfTower(short towerID, double dataTech, int time, double offer) {
      int maximumDistance = maximumDistance(towerID, dataTech, time);
      if (maximumDistance < state.distMin) return -1;
      if (offer > state.offerMax) return -1;
      return towerPopulations[time][towerID][maximumDistance] * offer;
    }

    // cost of tower
    // calculated with a given renting offer
    public static double costOfTower(short towerID, double rentingOffer, TPlayer player) {
      return player.inputData.towerInf[towerID].runningCost + rentingOffer;
    }

    // profit of tower with actual state
    public static double actualProfitOfTower(short towerID, TPlayer player) {
      return profitOfTower(towerID, player.inputData.towerInf[towerID].rentingCost, player.inputData.towerInf[towerID].offer, player);
    }

    // profit of the tower with hypothetical state with actual time
    public static double profitOfTower(short towerID, float rentingCost, float offer, TPlayer player) {
      return profitOfTower(towerID, rentingCost, offer, player.myTime, player);
    }

    // profit of the tower with a hypothetical state
    public static double profitOfTower(short towerID, float rentingCost, float offer, int time, TPlayer player) {
      double cost = costOfTower(towerID, rentingCost, player);
      double revenue = revenueOfTower(towerID, state.dataTech * Math.pow(4, dataTechnology - 1), time, offer);
      return revenue - cost;
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
    validateOrders(player);
    clearLastState(player);
  }

  private static void mostBasicStrategy(TPlayer player) {
    Set<Short> secureTowers = new HashSet<>();
    Set<Short> notWorthItTowers = new HashSet<>();

    // update state of mytowers

    // check if the owned towers still worth it
    for (Short towerID : myTowers) {
      if (TowerUtils.actualProfitOfTower(towerID, player) > 0) secureTowers.add(towerID);
      else notWorthItTowers.add(towerID);
    }

    // check if we have towers to acquire
    for (short i = 0; i < player.inputData.towerInf.length; i++) {
      TtowerInfRec actualTowerInf = player.inputData.towerInf[i];
      if (actualTowerInf.owner != player.ID) continue; // for now skip our towers
      if (actualTowerInf.owner != 0) continue; // for now skip attacks
      int profitNextSteps = 0;
      // NOTE not checking if all profit is positive
      profitNextSteps += TowerUtils.profitOfTower(i, (float)state.rentingMin, (float)state.offerMax, player.myTime, player);
      profitNextSteps += TowerUtils.profitOfTower(i, (float)state.rentingMin, (float)state.offerMax, player.myTime+1, player);
      profitNextSteps += TowerUtils.profitOfTower(i, (float)state.rentingMin, (float)state.offerMax, player.myTime+2, player);
      if (profitNextSteps < 0) continue; // does not worth it!

      // buy as long as we can
      if (state.money > 3 * state.rentingMin) {
        player.rentTower(i, (float)state.rentingMin, TowerUtils.maximumDistance(i, state.dataTech, player.myTime), (float)state.offerMax);
        state.money -= 3 * state.rentingMin;
      }
    }

    // leave not worth it towers
    for (Short towerID : notWorthItTowers) {
      myTowers.remove(towerID);
      player.leaveTower(towerID);
    }
    // update money
  }

  private static void clearLastState(TPlayer player) {
    player.outputData.invest = 0;
    player.outputData.numOrders = 0;
  }
}
