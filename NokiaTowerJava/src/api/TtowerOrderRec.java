package api;
public class TtowerOrderRec {
	public short towerID;      // towerID
	public float rentingCost;  // licit, owner must be 0
	public boolean leave;      // finish renting (offect in 3 month), owner must be me
	public short distance;     // running cost depend on distance, maxDistance
	public float offer;        // offers for nearest customers
}