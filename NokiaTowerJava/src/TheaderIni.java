public class TheaderIni {
	public int init;            // 1, initialization
	public int timeOut;         // timeout in milisec
	public int seed;            // seed of random generator
	public int mapWidth;        // map size
	public int mapHeight;
	public int timeMax;         // maximum of game time (end of the game)
	public int teamMax;         // number of players in the game
	public double money;        // initial money
	public double rentingMin;   // minimal renting cost
	public double offerMaxIni;  // maximal offer at the start
	public double offerMax;     // maximal offer at any time
	public int caution;         // caution in month
	public int distMin;         // minimal distance of tower
	public int distMax;         // maximal distance of tower
	public double dataNeed;     // data transfer need
	public double dataMulti;    // growing of data transfer need, next month
	public double dataTech;     // data transfer possibility
	public double techMulti;    // growing of data transfer possibility, next level
	public int techLevelMax;    // number of technical levels of data transfer
	public double[] techCosts = new double[Decl.TECH_LEVEL_MAX + 1];
}