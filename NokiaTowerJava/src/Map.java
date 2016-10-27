
public class Map {

	public int seed = 0;

	public int mainTime = 0; // global time

	public int mapWidth = 0;
	public int mapHeight = 0;

	public int numTowers = 0;
	public int[][] towers = new int[Decl.TOWER_MAX][2];
	public boolean[][] towerMap = new boolean[Decl.MAP_SIZE][Decl.MAP_SIZE];
	public int[][] pop = new int[Decl.MAP_SIZE][Decl.MAP_SIZE];
	public int[][] pop0 = new int[Decl.MAP_SIZE][Decl.MAP_SIZE];

	public int totalPop = 0;

	public int numCenters = 0;
	public int[][] centers = new int[Decl.CENTER_MAX][2];
	public int[] centersScale = new int[Decl.CENTER_MAX];

	public double[] paramUrban = new double[Decl.TIME_MAX];
	public double[] paramGrow = new double[Decl.TIME_MAX];

	protected boolean[][] newMap = new boolean[2 * Decl.MAP_SIZE + 3][2 * Decl.MAP_SIZE + 3];
	protected int[][] centDist = new int[Decl.MAP_SIZE][Decl.MAP_SIZE];

	public int centerDistance(int y, int x) {
		int best = mapWidth + mapHeight;
		for (int i = 0; i < numCenters; i++) {
			int y1 = centers[i][1];
			int x1 = centers[i][0];
			int d1 = (int) Math.rint((double) (Math.abs(y - y1) + Math.abs(x - x1)) / centersScale[i]);
			if (best > d1)
				best = d1;
		}
		if (best < 1)
			best = 1;
		return best;
	}

	// generateLand
	public void generateLand() {
		double BASE_PROB = 0.536 + 0.001 * LCGRandom.Rnd(3);
		double CELL_PROB = 0.26;
		double SURROUND_PROB = 0.2;

		int w = 8;
		int h = 8;

		for (int i = 0; i <= 7; i++)
			for (int j = 0; j <= 7; j++) {
				double prob = LCGRandom.RndFloat();
				towerMap[i][j] = prob < BASE_PROB;
			}

		while ((w <= mapWidth) || (h <= mapHeight)) {
			for (int y = 0; y <= h * 2 - 1; y++) {
				for (int x = 0; x <= w * 2 - 1; x++) {
					double prob;
					if (towerMap[y / 2][x / 2])
						prob = BASE_PROB + CELL_PROB;
					else
						prob = BASE_PROB - CELL_PROB;
					int n = 0;
					if ((y % 2 == 0) && (y > 0))
						if (towerMap[y / 2 - 1][x / 2])
							n++;
						else
							n--;
					if ((y % 2 == 1) && (y < h - 1))
						if (towerMap[y / 2 + 1][x / 2])
							n++;
						else
							n--;
					if ((x % 2 == 0) && (x > 0))
						if (towerMap[y / 2][x / 2 - 1])
							n++;
						else
							n--;
					if ((x % 2 == 1) && (x < w - 1))
						if (towerMap[y / 2][x / 2 + 1])
							n++;
						else
							n--;
					prob = prob + n * SURROUND_PROB;
					newMap[y][x] = LCGRandom.RndFloat() < prob;
				}
			}
			int max = 2 * h - 1;
			if (max > 500)
				max = 500;
			for (int y = 0; y < max; y++)
				for (int x = 0; x < max; x++)
					towerMap[y][x] = newMap[y][x];
			w = 2 * w;
			h = 2 * h;
			if (w == 128) {
				w = 125;
				h = 125;
			}

			BASE_PROB = BASE_PROB + LCGRandom.RndFloat() / 12;
			SURROUND_PROB = SURROUND_PROB + LCGRandom.RndFloat() / 15;
			CELL_PROB = CELL_PROB - LCGRandom.RndFloat() / 25;
		}

		for (int y = 1; y <= mapHeight - 2; y++) {
			for (int x = 1; x <= mapWidth - 2; x++) {
				if ((towerMap[y + 1][x]) && (towerMap[y - 1][x]) && (towerMap[y][x + 1]) && (towerMap[y][x - 1])) {
					towerMap[y][x] = true;
				} else {
					if ((!towerMap[y + 1][x]) && (!towerMap[y - 1][x]) && (!towerMap[y][x + 1])
							&& (!towerMap[y][x - 1])) {
						towerMap[y][x] = false;
					}
				}
			}
		}
	}

	public void generateCenters() {
		numCenters = 8 + LCGRandom.Rnd(13);
		for (int i = 0; i < numCenters; i++) {
			centers[i][0] = 0;
			centers[i][1] = 0;
			centersScale[i] = 0;
		}

		for (int i = 0; i < numCenters; i++) {
			while (centers[i][0] == 0) {
				int x = 15 + LCGRandom.Rnd(mapWidth - 30);
				int y = 15 + LCGRandom.Rnd(mapHeight - 30);
				if (towerMap[y][x]) {
					centers[i][0] = x;
					centers[i][1] = y;
					centersScale[i] = 1 + LCGRandom.Rnd(5);
				}
			}
		}
	}

	public void generateTowers() {
		int towerNeed = 200 + LCGRandom.Rnd(200);
		numTowers = 0;

		for (int i = 0; i < numCenters; i++) {
			int jMax = 2 + 2 * centersScale[i];
			for (int j = 0; j <= jMax; j++) {
				int m = 10 + 3 * centersScale[i];
				int x = centers[i][0] + LCGRandom.Rnd(m) - m / 2;
				int y = centers[i][1] + LCGRandom.Rnd(m) - m / 2;
				if (towerMap[y][x]) {
					towers[numTowers][0] = x;
					towers[numTowers][1] = y;
					numTowers++;
				}
			}
		}
		while (numTowers < towerNeed) {
			int y = 15 + LCGRandom.Rnd(mapHeight - 30);
			int x = 15 + LCGRandom.Rnd(mapWidth - 30);
			if (towerMap[y][x]) {
				towers[numTowers][0] = x;
				towers[numTowers][1] = y;
				numTowers++;
			}
		}
	}

	public void generatePop() {
		totalPop = 0;
		for (int y = 0; y < mapHeight; y++) {
			for (int x = 0; x < mapWidth; x++) {
				if (towerMap[y][x]) {
					int maxPop = (int) Math.rint((double) Decl.POP_MAX / centerDistance(y, x));
					pop[y][x] = LCGRandom.Rnd(maxPop + 1);
					if (pop[y][x] <= 1)
						pop[y][x] = 1;
					totalPop = totalPop + pop[y][x];
				} else
					pop[y][x] = 0;
			}
		}
	}

	public void GenerateMap(int _seed) {
		seed = _seed;
		LCGRandom.RndIni(seed);

		mapWidth = Decl.MAP_SIZE;
		mapHeight = Decl.MAP_SIZE;

		generateLand();
		generateCenters();
		generateTowers();
		generatePop();

		pop0 = pop;

		paramUrban[0] = 0.9 + 0.25 * LCGRandom.RndFloat();
		paramGrow[0] = 0.95 + 0.2 * LCGRandom.RndFloat();

		mainTime = 0;
		for (int i = 1; i < Decl.TIME_MAX; i++) {
			paramUrban[i] = (1 + 49 * paramUrban[i - 1]) / 50 + 0.02 * LCGRandom.RndFloat() - 0.01;
			paramGrow[i] = (1.01 + 49 * paramGrow[i - 1]) / 50 + 0.02 * LCGRandom.RndFloat() - 0.01;
		}

		for (int y = 0; y < mapHeight; y++)
			for (int x = 0; x < mapWidth; x++)
				centDist[y][x] = centerDistance(y, x);
	}

	public void MapNextTime() {
		if (mainTime < Decl.TIME_MAX) {

			for (int y = 0; y < mapHeight; y++)
				for (int x = 0; x < mapWidth; x++)
					if (LCGRandom.RndFloat() < 0.1) {
						int k = (int) Math.floor(paramGrow[mainTime] * pop[y][x]);
						if (k < 20000)
							pop[y][x] = k;
						else
							pop[y][x] = 20000;
					}

			for (int i = 0; i < 2000; i++) {
				int x1 = 1 + LCGRandom.Rnd(mapWidth - 2);
				int y1 = 1 + LCGRandom.Rnd(mapHeight - 2);
				int x2 = 1 + LCGRandom.Rnd(mapWidth - 2);
				int y2 = 1 + LCGRandom.Rnd(mapHeight - 2);
				if ((towerMap[y1][x1]) && (towerMap[y2][x2])) {
					int d1 = centDist[y1][x1];
					int d2 = centDist[y2][x2];
					int k;
					if ((d1 < d2) && (paramUrban[mainTime] > 1)) {
						k = (int) Math.floor((paramUrban[mainTime] - 1) * pop[y2][x2]);
						pop[y1][x1] = pop[y1][x1] + k;
						pop[y2][x2] = pop[y2][x2] - k;
					} else if ((d1 > d2) && (paramUrban[mainTime] < 1)) {
						k = (int) Math.floor((1 - paramUrban[mainTime]) * pop[y2][x2]);
						pop[y1][x1] = pop[y1][x1] + k;
						pop[y2][x2] = pop[y2][x2] - k;
					} else if ((d1 > d2) && (paramUrban[mainTime] > 1)) {
						k = (int) Math.floor((paramUrban[mainTime] - 1) * pop[y1][x1]);
						pop[y1][x1] = pop[y1][x1] - k;
						pop[y2][x2] = pop[y2][x2] + k;
					} else if ((d1 < d2) && (paramUrban[mainTime] < 1)) {
						k = (int) Math.floor((1 - paramUrban[mainTime]) * pop[y1][x1]);
						pop[y1][x1] = pop[y1][x1] - k;
						pop[y2][x2] = pop[y2][x2] + k;
					}
				}
			}

			totalPop = 0;
			for (int y = 0; y < mapHeight; y++)
				for (int x = 0; x < mapWidth; x++)
					totalPop = totalPop + pop[y][x];

			mainTime++;
		}
	}
}
