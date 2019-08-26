package org.opentripplanner.routing.util;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;

public class TestElevationUtils extends TestCase {

	public void testLengthMultiplier() {

		PackedCoordinateSequenceFactory factory = PackedCoordinateSequenceFactory.DOUBLE_FACTORY;
		CoordinateSequence seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 1) });
		SlopeCosts costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.0, costs.lengthMultiplier);
		
		seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 2) });
                costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.00498756211208902702, costs.lengthMultiplier);
		
		seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 2), new Coordinate(15, 1) });
                costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.00992634231424500668, costs.lengthMultiplier);
	}

	public void testCalculateSlopeWalkEffectiveLengthFactor() {
		// 35% should hit the MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR=3, hence 300m is expected
		assertEquals(300.0, ElevationUtils.calculateEffectiveWalkLength(100, 35), 0.1);

		// 10% incline equals 1.42 penalty
		assertEquals(141.9, ElevationUtils.calculateEffectiveWalkLength(100, 10), 0.1);

		// Flat is flat, no penalty
		assertEquals(120.0, ElevationUtils.calculateEffectiveWalkLength(120, 0));

		// 5% downhill is the fastest to walk and effective distance only 0.83 * flat distance
		assertEquals(83.9, ElevationUtils.calculateEffectiveWalkLength(100, -5), 0.1);

		// 10% downhill is about the same as flat
		assertEquals(150.0, ElevationUtils.calculateEffectiveWalkLength(150, -15));

		// 15% downhill have a penalty of 1.19
		assertEquals(238.2, ElevationUtils.calculateEffectiveWalkLength(200, -30), 0.1);

		// 45% downhill hit the MAX_SLOPE_WALK_EFFECTIVE_LENGTH_FACTOR=3 again
		assertEquals(300.0, ElevationUtils.calculateEffectiveWalkLength(100, -45), 0.1);
	}

	public void testGetPartialElevationProfile() {
		int x_length = 10;
		Coordinate[] c = new Coordinate[6];
		// [(0, 10), (2, 8), (4, 6), (6, 4), (8, 2), (10, 0)]
		for (int i = 0; i <= x_length; i += 2) {
			c[i / 2] = new Coordinate(i, x_length - i);
		}
		PackedCoordinateSequence elevationProfile = new PackedCoordinateSequence.Double(c, 2);

		// Test that partial elevation profile from start to finish matched original profile
		PackedCoordinateSequence fullProfile =
				ElevationUtils.getPartialElevationProfile(elevationProfile, 0, x_length);
		assertEquals(elevationProfile.getDimension(), fullProfile.getDimension());
		assertEquals(elevationProfile.size(), fullProfile.size());
		assertEquals(elevationProfile.getX(0), fullProfile.getX(0));
		assertEquals(elevationProfile.getY(0), fullProfile.getY(0));
		assertEquals(elevationProfile.getX(5), fullProfile.getX(5));
		assertEquals(elevationProfile.getY(5), fullProfile.getY(5));

		// Test that partial elevation profile from start to half way has same start as full profile
		// and that its last elevation value is between the closest values from the full profile
		Double halfWay = 5.0;
		PackedCoordinateSequence firstHalfProfile =
				ElevationUtils.getPartialElevationProfile(elevationProfile, 0, halfWay);
		assertEquals(4, firstHalfProfile.size());
		assertEquals(elevationProfile.getX(0), firstHalfProfile.getX(0));
		assertEquals(elevationProfile.getY(0), firstHalfProfile.getY(0));
		assertEquals(halfWay, firstHalfProfile.getX(3));
		assertEquals(elevationProfile.getY(3) +
				(elevationProfile.getY(3) - elevationProfile.getY(4)) / 2,
				firstHalfProfile.getY(3));

		// Test that partial elevation profile from halfway to end has x values ranging from 0 to
		// (end - halfway), that the first elevation value is between the closest values from
		// the full profile and that the end has the same elevation value as full profile's end
		PackedCoordinateSequence lastHalfProfile =
				ElevationUtils.getPartialElevationProfile(elevationProfile, halfWay, 10);
		assertEquals(4, lastHalfProfile.size());
		assertEquals(0.0, lastHalfProfile.getX(0));
		assertEquals(elevationProfile.getY(3) +
						(elevationProfile.getY(3) - elevationProfile.getY(4)) / 2,
				lastHalfProfile.getY(0));
		assertEquals(halfWay, lastHalfProfile.getX(3));
		assertEquals(fullProfile.getY(5),
				lastHalfProfile.getY(3));
	}
}
