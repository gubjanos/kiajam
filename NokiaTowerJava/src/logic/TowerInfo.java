package logic;

import java.util.Comparator;

public class TowerInfo {
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