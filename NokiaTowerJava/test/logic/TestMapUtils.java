package logic;

import junit.framework.Assert;

import org.junit.Test;

public class TestMapUtils {
	@Test
	public void t1() {
		double d2 = MapUtils.getOverlapArea(0, 0, 0.8079455, 0, 1, 1);
		Assert.assertEquals(Math.PI/2, d2, 0.0001);
	}
	
	@Test
	public void compare1() {
		System.out.println("compare1");
		short r1 = 1, r2 = 3;
		double fra1 = MapUtils.getOverlapFraction(0, 0, 3, 0, r1, r2);
		double fra2 = MapUtils.getOverlapFraction2(0, 0, 3, 0, 1, 3);
		System.out.println("fra1=" + fra1);
		System.out.println("fra2=" + fra2);
		Assert.assertEquals(fra1, fra2, 0.001);
	}
	
	@Test
	public void compare2() {
		System.out.println("compare2");
		short r1 = 3, r2 = 1;
		double fra1 = MapUtils.getOverlapFraction(0, 0, 3, 0, r1, r2);
		double fra2 = MapUtils.getOverlapFraction2(0, 0, 3, 0, 3, 1);
		System.out.println("fra1=" + fra1);
		System.out.println("fra2=" + fra2);
		Assert.assertEquals(fra1, fra2, 0.0001);
	}
	
	@Test
	public void t2() {
		// edge case 1: 1 db kozos pont
		double d2 = MapUtils.getOverlapArea(0, 0, 2, 0, 1, 1);
		Assert.assertEquals(0.0, d2, 0.0001);
	}
	
	@Test
	public void t3() {
		// edge case 1: nincs kozos pont
		double d2 = MapUtils.getOverlapArea(0, 0, 3, 3, 1, 1);
		Assert.assertEquals(0.0, d2, 0.0001);
	}
	
	@Test
	public void t4() {
		// edge case 2: nagy kor fedi a kisebbet
		double d2 = MapUtils.getOverlapArea(0, 0, 0, 0, 1, 2);
		Assert.assertEquals(Math.PI, d2, 0.0001);
	}
	
	@Test
	public void t5() {
		// edge case 2: a 2 kor fedi egymast teljesen
		double d2 = MapUtils.getOverlapArea(0, 0, 0, 0, 1, 1);
		Assert.assertEquals(Math.PI, d2, 0.0001);
	}
}
