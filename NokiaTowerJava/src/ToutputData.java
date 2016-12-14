public class ToutputData {
	public double invest;
	public int numOrders;
	public TtowerOrderRec[] orders = new TtowerOrderRec[Decl.ORDER_MAX];
	
	public ToutputData() {
		for (int i = 0; i < orders.length; i++) {
			orders[i] = new TtowerOrderRec();
		}
	}
}
