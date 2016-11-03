import java.util.*;

public class JaniPlayer extends Player {
  private static Map map;
  private static TheaderIni state; // TODO: this is an initialized state, check when it has to be updated
  private static int[][][] populations; // time, x, y
  private static int[][][] towerPopulations; // time, tower, radius initialized by method calculateTowerPopulations
  private static short[][] towerDistances; // towerA, towerB, the distance between the two tower

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
    dataNeedInTime = new double[Decl.TIME_MAX];
    dataNeedInTime[0] = 1.0d;
    for (int i = 1; i < Decl.TIME_MAX; i++) {
      dataNeedInTime[i] = dataNeedInTime[i-1] * state.dataMulti;
    }

    calculateTowerPopulations(player);
    calculateTowerDistances(player);

    myTowers = new HashSet<>();
    towersUnderOffer = new HashSet<>();
    towerOffers = new HashMap<>();
    numberOfTowerOffers = new int[player.inputData.header.numTowers];
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
  private static void calculateTowerPopulations(TPlayer player) {
    // calculating total populations
    towerPopulations = new int[Decl.TIME_MAX][][];
    effectiveMaxRadius = Math.min(state.distMax - state.distMin, MAX_RADIUS_RANGE); // radius counted from distmin

    int squaredMaximumDistance = effectiveMaxRadius * effectiveMaxRadius + state.distMin * state.distMin; // squared maximum distance from a tower
    int maximumDistance = (int)Math.sqrt(squaredMaximumDistance);

    for (int time = 0; time < Decl.TIME_MAX; time++) {
      towerPopulations[time] = new int[player.inputData.header.numTowers][effectiveMaxRadius+1];
    }

    for (int x = 0; x < Decl.MAP_SIZE; x++) {
      for (int y = 0; y < Decl.MAP_SIZE; y++) {
        for (int actualTower = 0; actualTower < player.inputData.header.numTowers; actualTower++) {
          // if a map point can not be used by a tower, skip
          int squaredDistance = MapUtils.calculateSquaredDistance(x, y, map.towers[actualTower][0], map.towers[actualTower][1]);
          if (squaredDistance > squaredMaximumDistance) continue;

          int trueDistance = (int) Math.sqrt(squaredDistance);
          for (int actualDistance = trueDistance; actualDistance < maximumDistance; actualDistance++) {
            for (int time = 0; time < Decl.TIME_MAX; time++) {
              towerPopulations[time][actualTower][actualDistance] += populations[time][x][y];
            }
          }
        }
      }
    }
  }

  private static void calculateTowerDistances(TPlayer player) {
    towerDistances = new short[player.inputData.header.numTowers][player.inputData.header.numTowers];
    for (short i = 0; i < towerDistances.length; i++) {
      for (short j = 0; j < towerDistances.length; j++) {
        towerDistances[i][j] = (short)Math.sqrt(MapUtils.calculateSquaredDistance(map.towers[i][0],map.towers[i][1],map.towers[j][0],map.towers[j][1]));
      }
    }
  }


  // NOTE: these methods are now not enemy-aware
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

    // revenue with a given offer level
    public static double revenueOfTower(short towerID, double dataTech, short distance, int time, double offer) {
      return towerPopulations[time][towerID][distance] * offer;
    }

    // cost of tower
    // calculated with a given renting offer
    public static double costOfTower(short towerID, double rentingOffer, short distance, TPlayer player) {
      return player.inputData.towerInf[towerID].runningCost * distance + rentingOffer;
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
    // if tower is not runnable, the negative renting cost will be returned
    public static double profitOfTower(short towerID, float rentingCost, float offer, int time, TPlayer player) {
      short distance = maximumDistance(towerID, state.dataTech, time);
      return profitOfTower(towerID, rentingCost, offer, distance, time, player);
    }

    public static double profitOfTower(short towerID, float rentingCost, float offer, short distance, int time, TPlayer player) {
      if (distance < state.distMin) return -rentingCost;
      double cost = costOfTower(towerID, rentingCost, distance, player);
      // TODO: do something with distances here
      double revenue = revenueOfTower(towerID, state.dataTech * Math.pow(4, dataTechnology - 1), (short)(distance - state.distMin), time, offer);
      System.out.println("For tower: " + towerID + "cost: " + cost + "revenue: " + revenue);
      return revenue - cost;
    }
  }

  public static class EnemyAwareTowerUtils extends TowerUtils {
    public static double profitOfTower(short towerID, float rentingCost, float offer, short distance, int time, TPlayer player) {
      if (distance < state.distMin) return -rentingCost;
      double cost = costOfTower(towerID, rentingCost, distance, player);
      double revenue = revenueOfTower(towerID, state.dataTech * Math.pow(4, dataTechnology - 1), distance, time, offer, player);
      return revenue - cost;
    }

    // revenue with a given offer level
    // this method tries to take enemy towers into consideration, also could not infer the ownership and offer changes in future
    public static double revenueOfTower(short towerID, double dataTech, short distance, int time, double offer, TPlayer player) {
      ArrayList<Short> towersToCheck = new ArrayList<>();

      for (short i = 0; i < player.inputData.header.numTowers; i++) {
        if (i == towerID) continue;
        TtowerInfRec actualInfo = player.inputData.towerInf[i];
        if (actualInfo.offer > offer) continue; // they will take ours
        if (towerDistances[towerID][i] > distance
            && towerDistances[towerID][i] > maximumDistance(i, state.dataTech * Math.pow(4, actualInfo.techLevel - 1), time))
          continue; // no overlap

        // overlap, complex calculations happens here
        return 0; // TODO: do the complex calculations
      }
      return towerPopulations[time][towerID][distance] * offer;
    }
  }

  // validating orders and checking gathered towers
  public static void validateTowers(TPlayer player) {
    TtowerOrderRec[] orders = player.outputData.orders;

    // first, validating if we gathered towers
    for (int i = 0; i < orders.length; i++) {
      if (!orders[i].leave) {
        short towerID = orders[i].towerID;
        if (player.inputData.towerInf[towerID].owner == player.ID) {
          // we get this tower!
          myTowers.add(towerID);
        }
      }
    }

    // check if all of our towers are really ours
    ArrayList<Short> towersToRemove = new ArrayList<>();
    for (Short towerID : myTowers) {
      if (player.inputData.towerInf[towerID].owner != player.ID) towersToRemove.add(towerID);
    }
    for (Short towerID : towersToRemove) myTowers.remove(towerID);
  }

  private static void stepInGame(TPlayer player) {
    // set up game state
    player.myTime++;
    validateTowers(player);
    clearLastOrder(player);
    state.money = player.inputData.header.money;

    // do something useful
    mostBasicStrategy(player);
  }


  private static void mostBasicStrategy(TPlayer player) {
    Set<Short> secureTowers = new HashSet<>();
    Set<Short> notWorthItTowers = new HashSet<>();

    // check if the owned towers still worth it
    for (Short towerID : myTowers) {
      if (TowerUtils.actualProfitOfTower(towerID, player) > 0) secureTowers.add(towerID);
      else notWorthItTowers.add(towerID);
    }

    System.out.println("number of towers:" + player.inputData.towerInf.length + "offermax: " + player.inputData.header.offerMax);

    // check if we have towers to acquire
    for (short i = 0; i < player.inputData.towerInf.length; i++) {
      TtowerInfRec actualTowerInf = player.inputData.towerInf[i];
      //System.out.println(actualTowerInf.owner);
      if (actualTowerInf.owner == player.ID) continue; // for now skip our towers
      if (actualTowerInf.owner != 0) continue; // for now skip attacks
      double profitNextSteps = 0.0d;
      // NOTE not checking if all profit is positive
      profitNextSteps += TowerUtils.profitOfTower(i, (float)state.rentingMin, (float)player.inputData.header.offerMax, player.myTime, player);
      profitNextSteps += TowerUtils.profitOfTower(i, (float)state.rentingMin, (float)player.inputData.header.offerMax, player.myTime+1, player);
      profitNextSteps += TowerUtils.profitOfTower(i, (float)state.rentingMin, (float)player.inputData.header.offerMax, player.myTime+2, player);
      //System.out.println("TowerID: " + i + "profit: " + profitNextSteps);
      if (profitNextSteps < 0) continue; // does not worth it!
      System.out.println("TowerID: " + i + "profit: " + profitNextSteps);
      // buy as long as we can
      if (state.money >  state.rentingMin) {
        player.rentTower(i, (float)state.rentingMin, TowerUtils.maximumDistance(i, state.dataTech, player.myTime), (float)state.offerMax);
        state.money -= 4 * state.rentingMin; // cost of caution
      }
    }

    // leave not worth it towers
    for (Short towerID : notWorthItTowers) {
      myTowers.remove(towerID);
      player.leaveTower(towerID);
    }
  }

  // strategies section
  // these section is responsible for generating different strategies, the final player will be a combination of these
  // these agents has to return values on commands how likely we would like to do something
  // aggressive agent - destroy economy of enemy by targeting most successful towers
  // defensive agent - keeping up economy by holding the towers worth mentioning, keeping up quality etc
  // expansive agent - gathering economy by acquiring new towers where we can gather some profit
  // intensive agent - gathering economy by investing in new technology
  // each strategy should return the expected profit gather in the next 1, 5, n rounds
  // each strategy should return the expected cost in the next 1, 5, n rounds
  // each strategy should return the expected profit lost for enemies in the next 1, 5, n rounds

  // metastrategy section
  // metastrategy is responsible for choosing from strategies over time
  // example: in the beginning it would be benefitial to be expansive, in the midgame we should be defensive and intensive
  // while in the end probably we sould just destroy some economy for our enemies

  // alternative: creating a policy evaluation strategy

  private static void clearLastOrder(TPlayer player) {
    player.outputData.invest = 0;
    player.outputData.numOrders = 0;
  }
}
