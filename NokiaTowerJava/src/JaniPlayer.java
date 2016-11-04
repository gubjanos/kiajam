import java.util.*;

public class JaniPlayer extends Player {
  private static Map map;
  private static TheaderIni state; // TODO: this is an initialized state, check when it has to be updated
  private static int[][][] populations; // time, x, y
  private static int[][][] towerPopulations; // time, tower, radius initialized by method calculateTowerPopulations
  private static short[][] towerDistances; // towerA, towerB, the distance between the two tower

  private static double[][] towerOverlaps; // towerA, towerB, the overlap between the two tower

  private static double[] dataNeedInTime; // time, the data need factor in time
  private static int dataTechnology = 1;

  private static int effectiveMaxRadius;

  private static Set<Short> myTowers; // the set of towers owned
  private static Set<Short> towersUnderOffer; // the towers we are offering to
  private static java.util.Map<Integer, TtowerOrderRec> towerOffers; // the offers given for towers
  private static int[] numberOfTowerOffers; // the number of offers on the towers

  // otletek: tornyonkent kiszamolni range-ekre az osszlakossagot koronkent: 200 * 365 * RANGE => 0.07MB * range
  private static final int MAX_RADIUS_RANGE = 25;

  private static int[][] cloneIntArray (int[][] input){
    int[][] result = new int[input.length][];
    for (int i = 0; i < result.length; i++) {
      result[i] = input[i].clone();
    }
    return result;
  }

  public Tower[] towerInfos;

  public static class Tower {
    public short id;
    public short[] closestTowers;
    public double[] overlaps;
  }

  private static void init(TPlayer player) {
    // initialize characteristics
    state = player.headerIni;

    map = player.map;
    populations = new int[Decl.TIME_MAX][][];
    populations[0] = cloneIntArray(map.pop);

    // calculating populations in advance
    for (int i = 1; i < Decl.TIME_MAX; i++) {
      map.MapNextTime();
      populations[i] = cloneIntArray(map.pop);
    }

    // calculating data need increasement
    dataNeedInTime = new double[Decl.TIME_MAX];
    dataNeedInTime[0] = state.dataNeed;
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
    System.out.println("money: " + player.inputData.header.money);
    if (player.myTime == 0) {
      long t = System.currentTimeMillis();
      init(player);
      System.out.println("Initialization took " + (System.currentTimeMillis() - t) + " ms.");
      player.myTime++;
    } else {
      System.out.println(player.myTime);
      System.out.println("time: " + player.inputData.header.time + " total pop:" + player.map.totalPop);

      stepInGame(player);
    }
  }

  // NOTE overlapping towers not taken into consideration
  private static void calculateTowerPopulations(TPlayer player) {
    // calculating total populations
    towerPopulations = new int[Decl.TIME_MAX][][];
    effectiveMaxRadius = Math.min(state.distMax - state.distMin, MAX_RADIUS_RANGE); // radius counted from distmin

    int squaredMaximumDistance = effectiveMaxRadius * effectiveMaxRadius + state.distMin * state.distMin; // squared maximum distance from a tower
    int maximumDistance = (int)Math.sqrt(squaredMaximumDistance);

    // number of towers not determined
    int numberOfTowers = player.map.towers.length;
    for (int i = 0; i < player.map.towers.length; i++) {
      if (player.map.towers[i][0] == 0 && player.map.towers[i][1] == 0) {
        numberOfTowers = i;
        break;
      }
    }

    for (int time = 0; time < Decl.TIME_MAX; time++) {
      towerPopulations[time] = new int[numberOfTowers][effectiveMaxRadius+1];
    }

    for (int x = 0; x < Decl.MAP_SIZE; x++) {
      for (int y = 0; y < Decl.MAP_SIZE; y++) {
        for (int actualTower = 0; actualTower < numberOfTowers; actualTower++) {
          // if a map point can not be used by a tower, skip
          // y-x switch in the map!
          int squaredDistance = MapUtils.calculateSquaredDistance(x, y, map.towers[actualTower][1], map.towers[actualTower][0]);
          if (squaredDistance > squaredMaximumDistance) continue;

          int trueDistance = (int) Math.sqrt(squaredDistance - state.distMin * state.distMin);
          for (int time = 0; time < Decl.TIME_MAX; time++) {
            towerPopulations[time][actualTower][trueDistance] += populations[time][x][y];
          }
        }
      }
    }

    for (short i = 0; i < numberOfTowers; i++) {
      for (int time = 0; time < Decl.TIME_MAX; time++) {
        calculatePrefixSum(towerPopulations[time][i]);
      }
    }
  }

  private static void calculateTowerDistances(TPlayer player) {
    // number of towers not determined
    int numberOfTowers = player.map.towers.length;
    for (int i = 0; i < player.map.towers.length; i++) {
      if (player.map.towers[i][0] == 0 && player.map.towers[i][1] == 0) {
        numberOfTowers = i;
        break;
      }
    }

    towerDistances = new short[numberOfTowers][numberOfTowers];
    for (short i = 0; i < towerDistances.length; i++) {
      for (short j = 0; j < towerDistances.length; j++) {
        towerDistances[i][j] = (short)Math.sqrt(MapUtils.calculateSquaredDistance(map.towers[i][0],map.towers[i][1],map.towers[j][0],map.towers[j][1]));
      }
    }
  }


  private static void calculatePrefixSum(int[] array) {
    for (int i = 1; i < array.length; i++) {
      array[i] = array[i-1] + array[i];
    }
  }

  // This method is an approximation only. it returns the overlap between circles c1 and c2, wrt area of c1
  public static double getOverlapFraction(int x1, int y1, int x2, int y2, short r1, short r2) {
    double distance = Math.sqrt(MapUtils.calculateSquaredDistance(x1, y1, x2, y2));
    if (distance > r1 + r2) return 0.0d;
    double biggerRadius = Math.max(r1,r2);
    if (biggerRadius >= distance) return 1.0d; // approximation here!
    // calculation of overlap here
    double s = (r1 + r2 + distance) / 2;
    double param1 = ((r2 * r2) - (r1 * r1) - (distance * distance)) / (-2.0 * r1 * distance);
    double alpha = 2 * Math.acos(param1);
    double param2 = ((r1 * r1) - (r2 * r2) - (distance * distance)) / (-2.0 * r2 * distance);
    double beta = 2 * Math.acos(param2);
    double t1 = (r1 * r1) * alpha / 2;
    double t2 = (r2 * r2) * alpha / 2;
    double t3 = Math.sqrt(s * (s - r1) * (s - r2) * (s-distance));
    double tCommon = t1 + t2 - 2 * t3;
    return tCommon / t1;
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
      return towerPopulations[time][towerID][distance] * offer / 1_000_000;
    }

    // cost of tower
    // calculated with a given renting offer
    private static final double RUNNINGPRICE = 100 / 2500; // running price for max distance
    public static double costOfTower(short towerID, double rentingOffer, short distance, TPlayer player) {
      return rentingOffer + RUNNINGPRICE * (distance * distance); //player.inputData.towerInf[towerID].runningCost * distance + rentingOffer;
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
      double revenue = revenueOfTower(towerID, state.dataTech * Math.pow(4, dataTechnology - 1), (short)(distance-state.distMin), time, offer);
      //System.out.println("For tower: " + towerID + "cost: " + cost + "revenue: " + revenue);
      return revenue - cost;
    }
  }

  public static class EnemyAwareTowerUtils {
    public static double profitOfTower(short towerID, float rentingCost, float offer, short distance, int time, TPlayer player) {
      if (distance < state.distMin) return -rentingCost;
      double cost = TowerUtils.costOfTower(towerID, rentingCost, distance, player);
      double revenue = revenueOfTower(towerID, state.dataTech * Math.pow(4, dataTechnology - 1), (distance), time, offer, player);
      //System.out.println("For tower: " + towerID + "cost: " + cost + "revenue: " + revenue);
      return revenue - cost;
    }

    // revenue with a given offer level
    // this method tries to take enemy towers into consideration, also could not infer the ownership and offer changes in future
    public static double revenueOfTower(short towerID, double dataTech, short distance, int time, double offer, TPlayer player) {
      double overlapLoss = 0.0d;
      for (short i = 0; i < player.inputData.header.numTowers; i++) {
        if (i == towerID) continue;
        TtowerInfRec actualInfo = player.inputData.towerInf[i];
        if (actualInfo.offer > offer) continue; // they will take ours
        //if (actualInfo.owner == 0) continue; // nobody uses this
        // checking orders here
        double overlap = getOverlapFraction(map.towers[towerID][1], map.towers[towerID][0], map.towers[i][1], map.towers[i][0], distance, actualInfo.distance);
        if (overlap > 0.05d) {
          int x = 1;
        }
        // overlap, complex calculations happens here
        overlapLoss += overlap;
      }
      return towerPopulations[time][towerID][distance - state.distMin] * offer * (1.0d - overlapLoss) / 1_000_000;
    }

    public static double actualProfitOfTower(short towerID, TPlayer player) {
      return EnemyAwareTowerUtils.profitOfTower(towerID, (float)state.dataTech, player.inputData.towerInf[towerID].offer, player.inputData.towerInf[towerID].distance, player.myTime, player);
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
    validateTowers(player);
    clearLastOrder(player);
    state.money = player.inputData.header.money;

    // do something useful
    mostBasicStrategy(player);

    // step time
    player.myTime++;
  }

  private static final int MIN_MONEY = 900;

  public static class TowerInfo {
    public short id;
    public double profit;
    public short distance;
    public TowerInfo(short id, double profit, short distance) {
      this.id = id;
      this.profit = profit;
      this.distance = distance;
    }
    public static class TowerInfoComparator implements Comparator<TowerInfo> {

      @Override
      public int compare(TowerInfo o1, TowerInfo o2) {
        return (int)(o2.profit - o1.profit);
      }

      @Override
      public boolean equals(Object obj) {
        if (! (obj instanceof TowerInfoComparator)) return false;
        return true;
      }
    }
  }

  private static void mostBasicStrategy(TPlayer player) {
    // TODO: defend secure towers
    Set<Short> secureTowers = new HashSet<>();
    Set<Short> notWorthItTowers = new HashSet<>();

    // check if the owned towers still worth it
    for (Short towerID : myTowers) {
      double profitNextSteps = 0.0d;
      profitNextSteps += EnemyAwareTowerUtils.profitOfTower(towerID, (float)state.dataTech, player.inputData.towerInf[towerID].offer, player.inputData.towerInf[towerID].distance, player.myTime, player);
      profitNextSteps += EnemyAwareTowerUtils.profitOfTower(towerID, (float)state.dataTech, player.inputData.towerInf[towerID].offer, player.inputData.towerInf[towerID].distance, player.myTime+1, player);
      profitNextSteps += EnemyAwareTowerUtils.profitOfTower(towerID, (float)state.dataTech, player.inputData.towerInf[towerID].offer, player.inputData.towerInf[towerID].distance, player.myTime+2, player);
      if (profitNextSteps < 1.0) secureTowers.add(towerID);
      else notWorthItTowers.add(towerID);
    }

    System.out.println("number of towers:" + player.inputData.header.numTowers + "offermax: " + player.inputData.header.offerMax);

    ArrayList<TowerInfo> towers = new ArrayList<>();
    // check if we have towers to acquire
    for (short i = 0; i < player.inputData.header.numTowers; i++) {
      TtowerInfRec actualTowerInf = player.inputData.towerInf[i];
      //System.out.println(actualTowerInf.owner);
      if (actualTowerInf.owner == player.ID) continue; // for now skip our towers
      if (actualTowerInf.owner != 0) continue; // for now skip attacks


      short maxDistance = TowerUtils.maximumDistance(i, state.dataTech, player.myTime);
      double maximumProfit = 0.0d;
      TowerInfo maximumInfo = null;

      for (short distance = (short)state.distMin; distance < maxDistance; distance++) {
        // TODO: checking different offer levels
        double profitNextSteps = 0.0d;
        // NOTE not checking if all profit is positive
        profitNextSteps += EnemyAwareTowerUtils.profitOfTower(i, (float) state.rentingMin, (float) player.inputData.header.offerMax, distance,
            player.myTime, player);
        profitNextSteps += EnemyAwareTowerUtils.profitOfTower(i, (float) state.rentingMin, (float) player.inputData.header.offerMax, distance,
            player.myTime + 1, player);
        profitNextSteps += EnemyAwareTowerUtils.profitOfTower(i, (float) state.rentingMin, (float) player.inputData.header.offerMax, distance,
            player.myTime + 2, player);
        if (profitNextSteps > maximumProfit) {
          maximumProfit = profitNextSteps;
          maximumInfo = new TowerInfo(i, profitNextSteps, distance);
        }
      }

      if (maximumProfit < 1.0) continue; // does not worth it!
      towers.add(maximumInfo);
    }

    Collections.sort(towers, new TowerInfo.TowerInfoComparator());
    for (TowerInfo t : towers) {
      //System.out.println("TowerID: " + i + "profit: " + profitNextSteps);
      // buy as long as we can
      if (state.money - MIN_MONEY >  state.rentingMin * 3 + TowerUtils.costOfTower(t.id, state.rentingMin, t.distance, player)) {
        player.rentTower(t.id, (float)state.rentingMin, t.distance, (float)player.inputData.header.offerMax);
        state.money -= state.rentingMin * 3 + TowerUtils.costOfTower(t.id, state.rentingMin, t.distance, player); // cost of caution
      }
    }

    // leave not worth it towers
    for (Short towerID : notWorthItTowers) {
      // TODO: checking modifications on distance/offer for each tower as improvement
      short maxDistance = TowerUtils.maximumDistance(towerID, state.dataTech, player.myTime);
      double maximumProfit = 0.0d;
      TowerInfo maximumInfo = null;

      for (short distance = (short)state.distMin; distance < maxDistance; distance++) {
        // TODO: checking different offer levels
        double profitNextSteps = 0.0d;
        // NOTE not checking if all profit is positive
        profitNextSteps += EnemyAwareTowerUtils.profitOfTower(towerID, (float) state.rentingMin, (float) player.inputData.header.offerMax, distance,
            player.myTime, player);
        profitNextSteps += EnemyAwareTowerUtils.profitOfTower(towerID, (float) state.rentingMin, (float) player.inputData.header.offerMax, distance,
            player.myTime + 1, player);
        profitNextSteps += EnemyAwareTowerUtils.profitOfTower(towerID, (float) state.rentingMin, (float) player.inputData.header.offerMax, distance,
            player.myTime + 2, player);
        if (profitNextSteps > maximumProfit) {
          maximumProfit = profitNextSteps;
          maximumInfo = new TowerInfo(towerID, profitNextSteps, distance);
        }
      }
      if (maximumProfit > 1.0) {
        player.rentTower(towerID, player.inputData.towerInf[towerID].rentingCost, maximumInfo.distance, player.inputData.towerInf[towerID].offer);
        continue; // does worth it!
      }

      // does not worth it!
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
