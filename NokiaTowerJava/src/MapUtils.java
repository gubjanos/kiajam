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