package games.stendhal.server.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import games.stendhal.common.Outfits;

import org.junit.Test;

public class OutfitTest {

	@Test
	public void testOutfit() {
		final Outfit ou = new Outfit();
		assertEquals(Integer.valueOf(0), ou.getHair());
		assertEquals(Integer.valueOf(0), ou.getHead());
		assertEquals(Integer.valueOf(0), ou.getDress());
		assertEquals(Integer.valueOf(0), ou.getBase());

	}

	@Test
	public void testOutfitIntegerIntegerIntegerInteger() {
		final Outfit ou = new Outfit(1, 2, 3, 4);
		assertEquals(Integer.valueOf(1), ou.getHair());
		assertEquals(Integer.valueOf(2), ou.getHead());
		assertEquals(Integer.valueOf(3), ou.getDress());
		assertEquals(Integer.valueOf(4), ou.getBase());
		
		final Outfit outfit2 = new Outfit(-1, -2, -3, -4);
		assertEquals(Integer.valueOf(-1), outfit2.getHair());
		assertEquals(Integer.valueOf(-2), outfit2.getHead());
		assertEquals(Integer.valueOf(-3), outfit2.getDress());
		assertEquals(Integer.valueOf(-4), outfit2.getBase());
	}

	@Test
	public void testOutfitInt() {
		Outfit ou = new Outfit(0);
		assertEquals(Integer.valueOf(0), ou.getHair());
		assertEquals(Integer.valueOf(0), ou.getHead());
		assertEquals(Integer.valueOf(0), ou.getDress());
		assertEquals(Integer.valueOf(0), ou.getBase());
		ou = new Outfit(1020304);
		assertEquals(Integer.valueOf(1), ou.getHair());
		assertEquals(Integer.valueOf(2), ou.getHead());
		assertEquals(Integer.valueOf(3), ou.getDress());
		assertEquals(Integer.valueOf(4), ou.getBase());

		final String outfitnumber = "01020304";
		ou = new Outfit(Integer.parseInt(outfitnumber));
		assertEquals(Integer.valueOf(1), ou.getHair());
		assertEquals(Integer.valueOf(2), ou.getHead());
		assertEquals(Integer.valueOf(3), ou.getDress());
		assertEquals(Integer.valueOf(4), ou.getBase());
		// coded octal
		ou = new Outfit(01020304); 
		assertEquals(Integer.valueOf(0), ou.getHair());
		assertEquals(Integer.valueOf(27), ou.getHead());
		assertEquals(Integer.valueOf(5), ou.getDress());
		assertEquals(Integer.valueOf(32), ou.getBase());

	}

	@Test
	public void testGetCode() {
		assertEquals(12345678, new Outfit(12345678).getCode());
	}

	@Test
	public void testPutOver() {
		Outfit orig = new Outfit(12345678);
		final Outfit pullover = new Outfit();
		assertEquals(12345678, orig.getCode());

		Outfit result = orig.putOver(pullover);
		assertEquals(12345678, result.getCode());
		
		
		orig = new Outfit(12, 34, 56, null);
		result = orig.putOver(pullover);
		assertEquals(Integer.valueOf(12), result.getHair());
		assertEquals(Integer.valueOf(34), result.getHead());
		assertEquals(Integer.valueOf(56), result.getDress());
		assertEquals(Integer.valueOf(0), result.getBase());
		orig = new Outfit(12, 34, null, null);
		result = orig.putOver(pullover);
		assertEquals(Integer.valueOf(12), result.getHair());
		assertEquals(Integer.valueOf(34), result.getHead());
		assertEquals(Integer.valueOf(0), result.getDress());
		assertEquals(Integer.valueOf(0), result.getBase());
		orig = new Outfit(12, null, null, null);
		result = orig.putOver(pullover);
		assertEquals(Integer.valueOf(12), result.getHair());
		assertEquals(Integer.valueOf(0), result.getHead());
		assertEquals(Integer.valueOf(0), result.getDress());
		assertEquals(Integer.valueOf(0), result.getBase());
		orig = new Outfit(null, null, null, null);
		result = orig.putOver(pullover);
		assertEquals(Integer.valueOf(0), result.getHair());
		assertEquals(Integer.valueOf(0), result.getHead());
		assertEquals(Integer.valueOf(0), result.getDress());
		assertEquals(Integer.valueOf(0), result.getBase());

	}

	@Test
	public void testIsPartOf() {
		final Outfit of = new Outfit();
		assertTrue(of.isPartOf(of));
		Outfit part = new Outfit(null, null, null, null);
		assertTrue(part.isPartOf(of));
		part = new Outfit(0, null, null, null);
		assertTrue(part.isPartOf(of));
		part = new Outfit(null, 0, null, null);
		assertTrue(part.isPartOf(of));
		part = new Outfit(null, null, 0, null);
		assertTrue(part.isPartOf(of));
		part = new Outfit(null, null, null, 0);
		assertTrue(part.isPartOf(of));
		part = new Outfit(0, null, null, 0);
		assertTrue(part.isPartOf(of));
		part = new Outfit(1, null, null, 0);
		assertFalse(part.isPartOf(of));
		part = new Outfit(1, 5, 5, 0);
		assertFalse(part.isPartOf(of));

	}

	@Test
	public void testIsChoosableByPlayers() {
		Outfit of = new Outfit();
		assertTrue(of.isChoosableByPlayers());
		of = new Outfit(Outfits.HAIR_OUTFITS, 0, 0, 0);
		assertFalse(of.isChoosableByPlayers());
		of = new Outfit(0, Outfits.HEAD_OUTFITS, 0, 0);
		
		assertFalse(of.isChoosableByPlayers());
		of = new Outfit(0, 0, Outfits.CLOTHES_OUTFITS, 0);
		
		assertFalse(of.isChoosableByPlayers());
		of = new Outfit(0, 0, 0, Outfits.BODY_OUTFITS);
		assertFalse(of.isChoosableByPlayers());
		// fail("wont work for any part of outfit == null");

	}

	@Test
	public void testIsNaked() {
		Outfit of = new Outfit();
		assertTrue(of.isNaked());
		 of = new Outfit(0, 0, null, 0);
		assertTrue(of.isNaked());
		 of = new Outfit(0, 0, 1, 0);
		assertFalse(of.isNaked());
	}

}
