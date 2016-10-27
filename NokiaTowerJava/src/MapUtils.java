public class MapUtils {
  public static int[][] popNextTime(int[][] popActualTime, Map map) {
    // NOTE code cloned from Map.MapNextTime
    int[][] result = new int[Decl.MAP_SIZE][Decl.MAP_SIZE];

      for (int y = 0; y < map.mapHeight; y++)
        for (int x = 0; x < map.mapWidth; x++)
          if (LCGRandom.RndFloat() < 0.1) {
            int k = (int) Math.floor(map.paramGrow[map.mainTime] * popActualTime[y][x]);
            if (k < 20000) result[y][x] = k;
            else result[y][x] = 20000;
          }

      for (int i = 0; i < 2000; i++) {
        int x1 = 1 + LCGRandom.Rnd(map.mapWidth - 2);
        int y1 = 1 + LCGRandom.Rnd(map.mapHeight - 2);
        int x2 = 1 + LCGRandom.Rnd(map.mapWidth - 2);
        int y2 = 1 + LCGRandom.Rnd(map.mapHeight - 2);
        if ((map.towerMap[y1][x1]) && (map.towerMap[y2][x2])) {
          int d1 = map.centDist[y1][x1];
          int d2 = map.centDist[y2][x2];
          int k;
          if ((d1 < d2) && (map.paramUrban[map.mainTime] > 1)) {
            k = (int) Math.floor((map.paramUrban[map.mainTime] - 1) * popActualTime[y2][x2]);
            result[y1][x1] = popActualTime[y1][x1] + k;
            result[y2][x2] = popActualTime[y2][x2] - k;
          } else if ((d1 > d2) && (map.paramUrban[map.mainTime] < 1)) {
            k = (int) Math.floor((1 - map.paramUrban[map.mainTime]) * popActualTime[y2][x2]);
            result[y1][x1] = popActualTime[y1][x1] + k;
            result[y2][x2] = popActualTime[y2][x2] - k;
          } else if ((d1 > d2) && (map.paramUrban[map.mainTime] > 1)) {
            k = (int) Math.floor((map.paramUrban[map.mainTime] - 1) * popActualTime[y1][x1]);
            result[y1][x1] = popActualTime[y1][x1] - k;
            result[y2][x2] = popActualTime[y2][x2] + k;
          } else if ((d1 < d2) && (map.paramUrban[map.mainTime] < 1)) {
            k = (int) Math.floor((1 - map.paramUrban[map.mainTime]) * popActualTime[y1][x1]);
            result[y1][x1] = popActualTime[y1][x1] - k;
            result[y2][x2] = popActualTime[y2][x2] + k;
          }
        }
      }

    return result;
  }

  // Returns if a point (x1, y1) inside a (x2, y2) centered d radius circle
  public static boolean isInsideCircle(int x1, int y1, int x2, int y2, int radius) {
    return calculateSquaredDistance(x1, y1, x2, y2) <= (radius * radius);
  }

  // Returns the squared distance of two points (x1,y1) and (x2,y2)
  public static int calculateSquaredDistance(int x1, int y1, int x2, int y2) {
    return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
  }
}
