package logic;

import junit.framework.Assert;

import org.junit.Test;

public class TestMapUtils {
	@Test
	public void t() {
		double d2 = MapUtils.getOverlapFraction2(0, 0, 0.8079455, 0, 1, 1);
		Assert.assertEquals(Math.PI/2, d2, 0.0001);
	}
	
	@Test
	public void t2() {
		short r2, r1 = r2 = 1;
		double d = MapUtils.getOverlapFraction(0, 0, 1, 0, r1, r2);
		double d2 = MapUtils.getOverlapFraction2(0, 0, 1, 0, 1, 1);
		Assert.assertEquals(d, d2, 0.0001);
	}
}
