public class ExtendedMap extends Map {
  public int[][] popNextTime(int[][] popActualTime) {
    // NOTE code cloned from Map.MapNextTime
    int[][] result = new int[Decl.MAP_SIZE][Decl.MAP_SIZE];

      for (int y = 0; y < mapHeight; y++)
        for (int x = 0; x < mapWidth; x++)
          if (LCGRandom.RndFloat() < 0.1) {
            int k = (int) Math.floor(paramGrow[mainTime] * popActualTime[y][x]);
            if (k < 20000) result[y][x] = k;
            else result[y][x] = 20000;
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
            k = (int) Math.floor((paramUrban[mainTime] - 1) * popActualTime[y2][x2]);
            result[y1][x1] = popActualTime[y1][x1] + k;
            result[y2][x2] = popActualTime[y2][x2] - k;
          } else if ((d1 > d2) && (paramUrban[mainTime] < 1)) {
            k = (int) Math.floor((1 - paramUrban[mainTime]) * popActualTime[y2][x2]);
            result[y1][x1] = popActualTime[y1][x1] + k;
            result[y2][x2] = popActualTime[y2][x2] - k;
          } else if ((d1 > d2) && (paramUrban[mainTime] > 1)) {
            k = (int) Math.floor((paramUrban[mainTime] - 1) * popActualTime[y1][x1]);
            result[y1][x1] = popActualTime[y1][x1] - k;
            result[y2][x2] = popActualTime[y2][x2] + k;
          } else if ((d1 < d2) && (paramUrban[mainTime] < 1)) {
            k = (int) Math.floor((1 - paramUrban[mainTime]) * popActualTime[y1][x1]);
            result[y1][x1] = popActualTime[y1][x1] - k;
            result[y2][x2] = popActualTime[y2][x2] + k;
          }
        }
      }

      totalPop = 0;
      for (int y = 0; y < mapHeight; y++)
        for (int x = 0; x < mapWidth; x++)
          totalPop = totalPop + result[y][x];

    return result;
  }
}
