package logic;
  public class MapUtils {
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
    
    // Returns the squared distance of two points (x1,y1) and (x2,y2)
    public static int calculateDistance(int x1, int y1, int x2, int y2) {
      return (int)Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }
  }