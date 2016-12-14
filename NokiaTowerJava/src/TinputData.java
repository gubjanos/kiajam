public class TinputData {
	public Theader header = new Theader();
	public TtowerInfRec[] towerInf = new TtowerInfRec[Decl.TOWER_MAX];

	public TinputData() {
		for (int i = 0; i < towerInf.length; i++) {
			towerInf[i] = new TtowerInfRec();
		}
	}
}
