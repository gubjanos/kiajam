import java.util.*;
import java.util.stream.Collectors;


// TODO: remove api. prefix before class names


public class Player {
  private static Map map;
  private static TheaderIni state; // TODO: this is an initialized state, check when it has to be updated
  private static int[][][] populations; // time, x, y
  private static int[][][] towerPopulations; // time, tower, radius initialized by method calculateTowerPopulations
  private static Short[][] towerDistances; // towerA, towerB, the distance between the two tower

  private static double[] dataNeedInTime; // time, the data need factor in time
  private static int dataTechnology = 1;

  private static int effectiveMaxRadius;

  private static Set<Short> myTowers = new HashSet<>(); // the set of towers owned
//  private static java.util.Map<Integer, TtowerOrderRec> towerOffers = new HashMap<>(); // the offers given for towers
  private static Set<Short> bannedTowers; // set of towers banned from next acquire step

  // otletek: tornyonkent kiszamolni range-ekre az osszlakossagot koronkent: 200 * 365 * RANGE => 0.07MB * range
  private static final int MAX_RADIUS_RANGE = 40;
  private static final int RADIUS_APPROXIMATION = 5; // the stepsize between radiuses

  private static int[][] cloneIntArray (int[][] input){
    int[][] result = new int[input.length][];
    for (int i = 0; i < result.length; i++) {
      result[i] = input[i].clone();
    }
    return result;
  }

  public Tower[] towerInfos;

  /** 
   * This class is responsible for long initialization version of the player
   */
  public static class LongInitializationProcess extends IterativeInitializationProcess{
  	public static void init(TPlayer player) {
  		// initialize characteristics
  		state = player.headerIni;

		player.clubBonuses.rentingCost = 20;
		player.clubBonuses.runningCost = 20;
		player.clubBonuses.taxRelief = 20;
		player.clubBonuses.researchCost = 20;
		player.clubBonuses.reasearchEfficiency = 20;

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

  		calculateTowerPopulations(player, Decl.TIME_MAX);
  		calculateTowerDistances(player);
  	}
  }


	// Same like LongInitializationProcess, but some states are calculated iteratively
	// NOTE! dont forget to call doLookup method
	public static class IterativeInitializationProcess {
		protected static int lastTime;
		
		/**
		 * initializes:
		 * 	-state
		 * 	-populations
		 *  -dataNeedInTime
		 */
		public static void init(TPlayer player, int lookahead) {
            lastTime = 0;
			state = player.headerIni;
			map = player.map;
			populations = new int[Decl.TIME_MAX][][];
			populations[0] = cloneIntArray(map.pop);
			
			calculatePopulationsInAdvance(lookahead);
			calculateDataNeedIncreasement();
			calculateTowerPopulations(player, lookahead);
            calculateTowerDistances(player);
            
			lastTime += lookahead;
		}

		private static void calculateDataNeedIncreasement() {
			long t = System.currentTimeMillis();
			dataNeedInTime = new double[Decl.TIME_MAX];
			dataNeedInTime[0] = state.dataNeed;
			for (int i = 1; i < Decl.TIME_MAX; i++) {
				dataNeedInTime[i] = dataNeedInTime[i-1] * state.dataMulti;
			}
			System.out.println("calculating data need increasement: [" + (System.currentTimeMillis()-t) + "] ms");
		}
		
		static void calculatePopulationsInAdvance(int lookahead){
            long t = System.currentTimeMillis();
            for (int i = lastTime+1; i < Math.min(Decl.TIME_MAX, lastTime+lookahead); i++) {
				map.MapNextTime();
				populations[i] = cloneIntArray(map.pop);
			}
            System.out.println("calculating populations in advance: [" + (System.currentTimeMillis()-t) + "] ms");
		}

		public static void doLookahead(TPlayer player, int lookahead) {
            for (int i = lastTime+1; i < Math.min(Decl.TIME_MAX, lastTime+lookahead+1); i++) {
                map.MapNextTime();
                populations[i] = cloneIntArray(map.pop);
            }
            if (lastTime + lookahead > Decl.TIME_MAX) return; // skipping if there is no lookahead
			lookahead = Math.min(Decl.TIME_MAX - lastTime, lookahead); // checking for upper boundary
			calculateTowerPopulations(player, lookahead);
			lastTime += lookahead;
		}

		// NOTE overlapping towers not taken into consideration
		protected static void calculateTowerPopulations(TPlayer player, int lookAhead) {
			long t = System.currentTimeMillis();
			// calculating total populations
			if (towerPopulations == null) towerPopulations = new int[Decl.TIME_MAX][][];
			effectiveMaxRadius = Math.min(state.distMax - state.distMin, MAX_RADIUS_RANGE); // radius counted from distmin

			int squaredMaximumDistance = effectiveMaxRadius * effectiveMaxRadius + state.distMin * state.distMin; // squared maximum distance from a tower
//			int maximumDistance = (int)Math.sqrt(squaredMaximumDistance);

			// number of towers not determined
			int numberOfTowers = numberOfTowers(player);

			for (int time = lastTime; time < lastTime + lookAhead; time++) {
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
						for (int time = lastTime; time < lastTime + lookAhead; time++) {
							towerPopulations[time][actualTower][trueDistance] += populations[time][x][y];
						}
					}
				}
			}

			for (short i = 0; i < numberOfTowers; i++) {
				for (int time = lastTime; time < lastTime + lookAhead; time++) {
					calculatePrefixSum(towerPopulations[time][i]);
				}
			}
			System.out.println("calculateTowerPopulations: [" + (System.currentTimeMillis()-t) + "] ms");
		}
		
		static int numberOfTowers(final TPlayer player) {
			return (int)Arrays.stream(player.map.towers).filter(x -> x[0] + x[1] != 0).count();
		}

		/**
		 * initializes {@code towerDistances}
		 */
		protected static void calculateTowerDistances(TPlayer player) {
			long t = System.currentTimeMillis();
			int numberOfTowers = numberOfTowers(player);

			towerDistances = new Short[numberOfTowers][numberOfTowers];
			for (short i = 0; i < towerDistances.length; i++) {
				for (short j = 0; j < towerDistances.length; j++) {
					if (i == j)
						towerDistances[i][j] = 0;
					else
						towerDistances[i][j] = (short) MapUtils.calculateDistance(map.towers[i][0], map.towers[i][1],
								map.towers[j][0], map.towers[j][1]);
				}
			}
			System.out.println("calculateTowerDistances: [" + (System.currentTimeMillis() - t) + "] ms");
		}
	}

	// this class speeds up calculations by skipping data points - thus approximating populations
	public static class ApproximativeLookahead extends IterativeInitializationProcess {
		private static Random r = new Random(42);
		private static float SKIP_VAL = 0.2f;

		public static void doLookahead(TPlayer player, int lookahead) {
            predictFuturePopulations(lookahead);
            if (lastTime + lookahead > Decl.TIME_MAX) return; // skipping if there is no lookahead
			lookahead = Math.min(Decl.TIME_MAX - lastTime, lookahead); // checking for upper boundary
			calculateTowerPopulations(player, lookahead, SKIP_VAL);
			lastTime += lookahead;
		}

		private static void predictFuturePopulations(int lookahead) {
			for (int i = lastTime; i < Math.min(Decl.TIME_MAX, lastTime+lookahead); i++) {
                map.MapNextTime();
                populations[i] = cloneIntArray(map.pop);
            }
		}

		/**
		 * initializes: {@code towerPopulations, effectiveMaxRadius}
		 */
		protected static void calculateTowerPopulations(TPlayer player, int lookAhead, float skipVal) {
			// calculating total populations
			if (towerPopulations == null) towerPopulations = new int[Decl.TIME_MAX][][];
			effectiveMaxRadius = Math.min(state.distMax - state.distMin, MAX_RADIUS_RANGE); // radius counted from distmin

			int squaredMaximumDistance = MapUtils.calculateSquaredDistance(effectiveMaxRadius, state.distMin); // squared maximum distance from a tower

			// number of towers not determined
			int numberOfTowers = numberOfTowers(player);

			for (int time = lastTime; time < lastTime + lookAhead; time++) {
				towerPopulations[time] = new int[numberOfTowers][effectiveMaxRadius+1];
			}

			for (int x = 0; x < Decl.MAP_SIZE; x++) {
				for (int y = 0; y < Decl.MAP_SIZE; y++) {
					if (r.nextFloat() > skipVal) continue; // skip
					for (int actualTower = 0; actualTower < numberOfTowers; actualTower++) {
						// if a map point can not be used by a tower, skip
						// y-x switch in the map!
						int squaredDistance = MapUtils.calculateSquaredDistance(x, y, map.towers[actualTower][1], map.towers[actualTower][0]);
						if (squaredDistance > squaredMaximumDistance) continue;

						int trueDistance = (int) Math.sqrt(squaredDistance - state.distMin * state.distMin);
						for (int time = lastTime; time < lastTime + lookAhead; time++) {
							towerPopulations[time][actualTower][trueDistance] += populations[time][x][y];
						}
					}
				}
			}

			for (short i = 0; i < numberOfTowers; i++) {
				for (int time = lastTime; time < lastTime + lookAhead; time++) {
					calculatePrefixSum(towerPopulations[time][i]);
					multiplyArray(towerPopulations[time][i], (int)(1.0f/skipVal));
				}
			}
		}
	}


//	private static final int LOOKAHEAD = 3; // TODO this is a parameter of the script

	public static void makeMove(TPlayer player) {
		System.out.println("money: " + player.inputData.header.money);
		//wtf
		if (player.map == null) {
			player.map = new Map();
			player.map.GenerateMap(player.headerIni.seed);
		}

		long t = System.currentTimeMillis();
		if (player.myTime == 0) {
			IterativeInitializationProcess.init(player, 10);
			ApproximativeLookahead.doLookahead(player, 1);
			System.out.println("Initialization took " + (System.currentTimeMillis() - t) + " ms");
		} else {
			System.out.println(player.myTime);
			System.out.println("time: " + player.inputData.header.time + ", total pop:" + player.map.totalPop);
			ApproximativeLookahead.doLookahead(player, 1);
			System.out.println("Lookahead took " + (System.currentTimeMillis() - t) + " ms");
			stepInGame(player);
		}

		// step time
		player.myTime++;
	}


  private static void calculatePrefixSum(int[] array) {
    for (int i = 1; i < array.length; i++) {
      array[i] = array[i-1] + array[i];
    }
  }

	private static void multiplyArray(int[] array, int multiplier) {
		for (int i = 0; i < array.length; i++) {
			array[i] *= multiplier;
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
      return towerPopulations[time][towerID][distance] * offer / 1_000_000;
    }

    // cost of tower
    // calculated with a given renting offer
    private static final double RUNNINGPRICE = 100.0d / 2500.0d; // running price for max distance
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

    /**
     * returns: revenue - cost
     */
    public static double profitOfTower(short towerID, float rentingCost, float offer, short distance, int time, TPlayer player) {
      if (distance < state.distMin) return -rentingCost;
      double cost = costOfTower(towerID, rentingCost, distance, player);
      // TODO: do something with distances here
      double revenue = revenueOfTower(towerID, state.dataTech * Math.pow(state.techMulti, dataTechnology - 1), (short)(distance-state.distMin), time, offer);
      //System.out.println("For tower: " + towerID + "cost: " + cost + "revenue: " + revenue);
      return revenue - cost;
    }
  }

  public static class EnemyAwareTowerUtils {
    public static List<Float> getNearbyOffers(short towerID, short distance, TPlayer player) {
      ArrayList<Float> offers = new ArrayList<>();
      for (int i = 0; i < player.inputData.header.numTowers; i++) {
        double overlap = MapUtils.getOverlapFraction(map.towers[towerID][1], map.towers[towerID][0], map.towers[i][1], map.towers[i][0], distance, distance);
        if (overlap > 0.05d) offers.add(player.inputData.towerInf[i].offer);
      }
      return offers;
    }

    public static Double[] profitOfTower(short towerID, float rentingCost, float offer, short distance, int time, TPlayer player) {
      if (distance < state.distMin) return new Double[] {(double) 0, (double) -rentingCost}; // TODO the cost is not exact!
      double cost = TowerUtils.costOfTower(towerID, rentingCost, distance, player);
      double revenue = revenueOfTower(towerID, state.dataTech * Math.pow(state.techMulti, dataTechnology - 1), (distance), time, offer, player);
      //System.out.println("For tower: " + towerID + "cost: " + cost + "revenue: " + revenue);
      return new Double[] {revenue, cost};
    }

    // revenue with a given offer level
    // this method tries to take enemy towers into consideration, also could not infer the ownership and offer changes in future
		// TODO: we are not aware of our own bids yet
		public static double revenueOfTower(short towerID, double dataTech, short distance, int time, double offer,
				TPlayer player) {
			double overlapLoss = 0.0d;
			for (short i = 0; i < player.inputData.header.numTowers; i++) {
				if (i == towerID)
					continue;
				TtowerInfRec actualInfo = player.inputData.towerInf[i];
				if (actualInfo.offer > offer)
					continue; // they will take ours
				double overlap;
				if (actualInfo.owner == 0) {
					overlap = 0.0;
					// overlap = getOverlapFraction(map.towers[towerID][1], map.towers[towerID][0], map.towers[i][1], map.towers[i][0], distance, distance);
				} else {
					// checking orders here
					overlap =  MapUtils.getOverlapFraction(map.towers[towerID][1], map.towers[towerID][0], map.towers[i][1],
							map.towers[i][0], distance, actualInfo.distance);
				}
				// overlap, complex calculations happens here
				overlapLoss += overlap;
			}
			return towerPopulations[time][towerID][distance - state.distMin] * offer * (1.0d - overlapLoss) / 1_000_000;
		}

    public static double actualProfitOfTower(short towerID, TPlayer player) {
      Double[] stats = EnemyAwareTowerUtils.profitOfTower(towerID, (float)state.dataTech, player.inputData.towerInf[towerID].offer, player.inputData.towerInf[towerID].distance, player.myTime, player);
    	return stats[1] - stats[0];
		}
  }


	/**
	 * validating orders and checking gathered towers
	 */
	public static void validateTowers(TPlayer player) {
		// first, validating if we gathered towers
		Set<Short> ids = Arrays.stream(player.outputData.orders).filter(o -> !o.leave)
				.filter(o -> player.inputData.towerInf[o.towerID].owner == player.ID).map(o -> o.towerID)
				.collect(Collectors.toSet());
		myTowers.addAll(ids);

		// check if all of our towers are really ours
		myTowers = myTowers.stream().filter(towerID -> player.inputData.towerInf[towerID].owner == player.ID)
				.collect(Collectors.toSet());
	}

	private static void stepInGame(TPlayer player) {
		// set up game state
		validateTowers(player);
		clearLastOrder(player);
		updateDataTechState(player);
		mostBasicStrategy(player);
	}

  private static final int MIN_MONEY = 700;

	private static Random strategyR = new Random(44);
	private static float INVEST_PROB = 0.5f;
	private static float MINIMUM_PROFIT = 10f;

	private static float ATTACK_PROB = 0.2f;
private static int LOOKAHEAD = 3;
	// TODO: it would be great to check the status of enemy players too!
  private static void mostBasicStrategy(TPlayer player) {
		// TODO: do it better
	  
	  state.money = player.inputData.header.money;
    
    if (player.myTime > state.timeMax - LOOKAHEAD) return; // panic!

		// invest
		if (state.dataTech < state.techLevelMax-1 && state.money > state.techCosts[(int)(state.dataTech+1)] && strategyR.nextFloat() > INVEST_PROB) {
			float inv = (float) (strategyR.nextFloat() * state.money / 10);
			player.outputData.invest = inv;
			state.money -= inv;

		}

    Set<Short> secureTowers = new HashSet<>();
    Set<Short> notWorthItTowers = new HashSet<>();
      bannedTowers = new HashSet<>();

		// check if the owned towers still worth it
		for (Short towerID : myTowers) {
			double[] stats = getProfitNextSteps(player, towerID, player.inputData.towerInf[towerID].rentingCost,
					player.inputData.towerInf[towerID].offer, player.inputData.towerInf[towerID].distance, LOOKAHEAD);
			if (stats[0] - stats[1] < MINIMUM_PROFIT)
				secureTowers.add(towerID);
			else
				notWorthItTowers.add(towerID);
		}

		ArrayList<TowerInfo> towers = new ArrayList<>();

		System.out.println("number of towers:" + player.inputData.header.numTowers + "offermax: " + player.inputData.header.offerMax);

		// DEFEND
		for (short i = 0; i < player.inputData.towerInf.length; i++) {
			if (player.inputData.towerInf[i].owner == player.ID && player.inputData.towerInf[i].licit > 0.0f
					&& player.inputData.towerInf[i].licitDelay == 1) {
				// do something!
				float licit = player.inputData.towerInf[i].licit;
				float offer = player.inputData.towerInf[i].offer;
				short distance = player.inputData.towerInf[i].distance;
				float rentingCost = player.inputData.towerInf[i].rentingCost;
				double[] stats = getProfitNextSteps(player, i, licit, offer, distance, LOOKAHEAD);

				if (stats[0] - stats[1] > MINIMUM_PROFIT) {
					towers.add(new TowerInfo(i, stats[0] - stats[1], player.inputData.towerInf[i].distance, TowerInfo.Type.DEFEND, offer, licit, (float)stats[1],licit - rentingCost));
				}
			}
		}


    // check if we have towers to acquire
    for (short i = 0; i < player.inputData.header.numTowers; i++) {
      TtowerInfRec actualTowerInf = player.inputData.towerInf[i];
      //System.out.println(actualTowerInf.owner);
      if (actualTowerInf.owner == player.ID) continue; // for now skip our towers
      if (actualTowerInf.owner != 0) continue; // for now skip attacks


      short maxDistance = TowerUtils.maximumDistance(i, state.dataTech, player.myTime);
      double maximumProfit = 0.0d;
      TowerInfo maximumInfo = null;

      for (short distance = (short)state.distMin; distance < maxDistance; distance+= RADIUS_APPROXIMATION) {
        // checking for playing out overlaps
        /*for (Float offer : EnemyAwareTowerUtils.getNearbyOffers(i, distance, player)) {
          double profitNextSteps = getProfitNextSteps(player, i, (float) state.rentingMin,
              (float) offer - 1.0f, distance, LOOKAHEAD);
          if (profitNextSteps > maximumProfit) {
            maximumProfit = profitNextSteps;
            maximumInfo = new TowerInfo(i, profitNextSteps, distance);
          }
        }*/

        // going for high order
        double[] stats = getProfitNextSteps(player, i, (float) state.rentingMin,
            (float) player.inputData.header.offerMax, distance, LOOKAHEAD);
        if (stats[0] - stats[1] > maximumProfit) {
          maximumProfit = stats[0] - stats[1];
          maximumInfo = new TowerInfo(i, stats[0] - stats[1], distance, TowerInfo.Type.ACQUIRE, (float)player.inputData.header.offerMax, (float)state.rentingMin, (float)stats[1], (float)state.rentingMin * 4);
        }
      }

      if (maximumProfit < MINIMUM_PROFIT) continue; // does not worth it!
      towers.add(maximumInfo);
    }

    // TODO: attack
		for (short i = 0; i < player.inputData.header.numTowers; i++) {
			TtowerInfRec actualTowerInf = player.inputData.towerInf[i];
			if (actualTowerInf.owner == player.ID) continue; // skip our towers
			if (actualTowerInf.owner == 0) continue; // skip acquire towers
			if (actualTowerInf.licitID == player.ID) // we already did it! ;)
			if (strategyR.nextFloat() > ATTACK_PROB) continue; // thats it

			int ourOverlicit = 10 + strategyR.nextInt(10);
			float percentage = 1.0f + ourOverlicit / 100.0f;
			double maximumProfit = 0.0d;
			TowerInfo maximumInfo = null;
			short maxDistance = TowerUtils.maximumDistance(i, state.dataTech, player.myTime);

			for (short distance = (short)state.distMin; distance < maxDistance; distance+= RADIUS_APPROXIMATION) {
				double[] stats = getProfitNextSteps(player, i, percentage * player.inputData.towerInf[i].rentingCost, (float)player.inputData.header.offerMax, distance, LOOKAHEAD);
				if (stats[0] - stats[1] > maximumProfit) {
					maximumProfit = stats[0] - stats[1];
					maximumInfo = new TowerInfo(i, stats[0] - stats[1], distance, TowerInfo.Type.ATTACK, (float)player.inputData.header.offerMax, percentage * player.inputData.towerInf[i].rentingCost, (float)stats[1], percentage * player.inputData.towerInf[i].rentingCost * 4);
				}
			}

			if (maximumProfit < MINIMUM_PROFIT) continue; // does not worth it!
			towers.add(maximumInfo);
		}


    Collections.sort(towers, new TowerInfo.TowerInfoComparator());
		for (TowerInfo t : towers) {
			// System.out.println("TowerID: " + i + "profit: " + profitNextSteps);
			// buy as long as we can
			// banned logic
			if (t.type == TowerInfo.Type.ACQUIRE && bannedTowers.contains(t.id))
				continue;
			if (strategyR.nextFloat() > 0.5f)
				continue; // be full retard!
			if (state.money - MIN_MONEY > t.actionCost) {
				player.rentTower(t.id, (float) state.rentingMin, t.distance, (float) player.inputData.header.offerMax);
				state.money -= t.actionCost; // cost of caution
				if (t.type == TowerInfo.Type.ACQUIRE) {
					addBannedTowers(bannedTowers, t, player);
				}
			}
			if (player.outputData.numOrders > 5)
				break; // sanity stuff
		}

    // leave not worth it towers
    for (Short towerID : notWorthItTowers) {
      // TODO: checking modifications on distance/offer for each tower as improvement
      /*short maxDistance = TowerUtils.maximumDistance(towerID, state.dataTech, player.myTime);
      double maximumProfit = 0.0d;
      TowerInfo maximumInfo = null;

      for (short distance = (short)state.distMin; distance < maxDistance; distance++) {
        // TODO: checking different offer levels
        double profitNextSteps = getProfitNextSteps(player, towerID, (float) state.rentingMin,
            (float) player.inputData.header.offerMax, distance, LOOKAHEAD);
        if (profitNextSteps > maximumProfit) {
          maximumProfit = profitNextSteps;
          maximumInfo = new TowerInfo(towerID, profitNextSteps, distance);
        }
      }
      if (maximumProfit > 1.0) {
        player.rentTower(towerID, player.inputData.towerInf[towerID].rentingCost, maximumInfo.distance, player.inputData.towerInf[towerID].offer);
        continue; // does worth it!
      }*/

      // does not worth it!
      myTowers.remove(towerID);
      player.leaveTower(towerID);
    }
  }

  private static void addBannedTowers(Set<Short> bannedTowers, TowerInfo t, TPlayer player) {
      for (short i = 0; i < player.inputData.header.numTowers; i++) {
          if (bannedTowers.contains(i)) continue; // no need to explain
          if (towerDistances[t.id][i] < (2 * t.distance)) bannedTowers.add(i); // bann
      }
  }

	/**
	 * returns [revenue, cost]
	 */
	private static double[] getProfitNextSteps(TPlayer player, Short towerID, float rentingCost, float offer,
											   short distance, int aheadSteps) {
		double revenue = 0.0d;
		double cost = 0.0d;
		for (int i = 0; i < aheadSteps; i++) {
			Double stats[] = EnemyAwareTowerUtils.profitOfTower(towerID, rentingCost, offer, distance, player.myTime
					+ i, player);
			revenue += stats[0];
			cost += stats[1];
		}
		return new double[] { revenue, cost };
	}

	// ez mit csinal ??
	private static void updateDataTechState(TPlayer player) {
		for (int i = 0; i < state.techCosts.length; i++) {
			if (player.inputData.header.resPoints > state.techCosts[i]) {
				dataTechnology = i;
			}
		}
//		Arrays.stream(state.techCosts).;
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

	private static class MapUtils {
		// Returns if a point (x1, y1) inside a (x2, y2) centered d radius circle
		public static boolean isInsideCircle(int x1, int y1, int x2, int y2, int radius) {
			return calculateSquaredDistance(x1, y1, x2, y2) <= (radius * radius);
		}

		// Returns the squared distance of two points (x1,y1) and (x2,y2)
		public static int calculateSquaredDistance(int a, int b) {
			return a * a + b * b;
		}

		public static int calculateSquaredDistance(int x1, int y1, int x2, int y2) {
			return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
		}

		public static double calculateDistance2(double x1, double y1, double x2, double y2) {
			return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
		}

		// Returns the squared distance of two points (x1,y1) and (x2,y2)
		public static int calculateDistance(int x1, int y1, int x2, int y2) {
			return (int)Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
		}

		public static double areaOfCircle(double radius) {
			return Math.PI * radius * radius;
		}

		// This method is an approximation only. it returns the overlap between circles c1 and c2, wrt area of c1
		public static double getOverlapFraction(int x1, int y1, int x2, int y2, short r1, short r2) {
			double distance = Math.sqrt(calculateSquaredDistance(x1, y1, x2, y2));
			if (distance > r1 + r2)
				return 0.0d;
			// double biggerRadius = Math.max(r1,r2);
			// calculation of overlap here
			double s = (r1 + r2 + distance) / 2;
			double param1 = ((r2 * r2) - (r1 * r1) - (distance * distance)) / (-2.0 * r1 * distance);
			double alpha = 2 * Math.acos(param1);
			if (Double.isNaN(alpha)) {
				if (r1 > r2)
					return ((r2 * r2) / (r1 * r1));
				else
					return 1.0f;
			}
			double param2 = ((r1 * r1) - (r2 * r2) - (distance * distance)) / (-2.0 * r2 * distance);
			double beta = 2 * Math.acos(param2);
			double t1 = (r1 * r1) * alpha / 2;
			double t2 = (r2 * r2) * beta / 2;
			double t3 = Math.sqrt(s * (s - r1) * (s - r2) * (s - distance));
			double tCommon = t1 + t2 - 2 * t3;
			return tCommon / (t1 / alpha * 2 * 3.14f);
		}

		public static double getOverlapArea(double x1, double y1, double x2, double y2, double r1, double r2) {
			double d = calculateDistance2(x1, y1, x2, y2);
			if (d > r1 + r2) return 0.0d;
			double A = (r2*r2 * Math.acos((d*d + r2*r2 - r1*r1)/(2*d*r2))) +
					(r1*r1 * Math.acos((d*d + r1*r1 - r2*r2)/(2*d*r1))) -
					(0.5*Math.sqrt((-d+r2+r1)*(d+r2-r1)*(d-r2+r1)*(d+r2+r1)));
			if (Double.isNaN(A)) {
				double radius = Math.min(r1,r2); // radius of the smaller
				return areaOfCircle(radius); // r^2 PI = area of the smaller
			}
			return A;
		}

		public static double getOverlapFraction2(double x1, double y1, double x2, double y2, double r1, double r2) {
			double area = getOverlapArea(x1, y1, x2, y2, r1, r2);
			double fraction = area / areaOfCircle(r1);
			return fraction;
		}
	}

	private static class TowerInfo {
		public static enum Type {
			DEFEND, ATTACK, ACQUIRE
		}

		public Type type;
		public short id;
		public double profit;
		public short distance;
		public float offer;
		public float rentingCost;
		public float actionCost; // the cost of the current action
		public float cost; // the total cost in the next n rounds

		public TowerInfo(short id, double profit, short distance, Type type, float offer, float rentingCost, float cost,
						 float actionCost) {
			this.id = id;
			this.profit = profit;
			this.distance = distance;
			this.type = type;
			this.offer = offer;
			this.rentingCost = rentingCost;
			this.cost = cost;
			this.actionCost = actionCost;
		}

		public static class TowerInfoComparator implements Comparator<TowerInfo> {

			@Override
			public int compare(TowerInfo o1, TowerInfo o2) {
				return (int) (o2.profit - o2.cost - (o1.profit - o1.cost));
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof TowerInfoComparator))
					return false;
				return true;
			}
		}
	}

	public class Tower {
		public short id;
		public short[] closestTowers;
		public double[] overlaps;
	}
}
