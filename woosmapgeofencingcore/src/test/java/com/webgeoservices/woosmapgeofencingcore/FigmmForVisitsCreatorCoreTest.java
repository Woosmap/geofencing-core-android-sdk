package com.webgeoservices.woosmapgeofencingcore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.ojalgo.matrix.MatrixR064;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Regression tests for the real {@link FigmmForVisitsCreatorCore} code paths that
 * depend on the upgraded ojalgo dependency. {@code figmm()} drives the full ZOI
 * ellipse computation (matrix invert + eigen-decomposition + multiply chains) that
 * was migrated from the removed {@code PrimitiveMatrix} API, so it is the most
 * valuable guard that the upgrade preserved behaviour.
 */
public class FigmmForVisitsCreatorCoreTest {

    private static final double EPS = 1e-9;

    private final FigmmForVisitsCreatorCore figmm = new FigmmForVisitsCreatorCore();

    /** Pure statistical helper - must keep returning the chi-squared threshold. */
    @Test
    public void chiSquaredValueMatchesFormula() {
        // -2 * ln(1 - p)
        assertEquals(-2 * Math.log(1 - 0.95), figmm.chi_squared_value(0.95), EPS);
        assertEquals(-2 * Math.log(1 - 0.7), figmm.chi_squared_value(0.7), EPS);
        // Known value: chi-squared (2 dof) for p=0.95 is ~5.9914645
        assertEquals(5.9914645471, figmm.chi_squared_value(0.95), 1e-9);
    }

    /** The default constructor pre-computes the update threshold via chi_squared_value. */
    @Test
    public void updateThresholdInitialised() {
        assertEquals(figmm.chi_squared_value(0.95), figmm.chi_squared_value_for_update, EPS);
    }

    @Test
    public void convertArraylistStringToArrayInt() {
        int[] result = figmm.convertArraylistStringToArrayInt(Arrays.asList("1", "2", "30"));
        assertArrayEquals(new int[]{1, 2, 30}, result);
    }

    /**
     * Drives the full ojalgo ellipse pipeline (invert + eigen-decomposition +
     * multiply chains) through the migrated code and checks the output is a
     * well-formed, correctly centred WKT polygon, then pins exact coordinates as a
     * golden value. The golden numbers were produced by the migrated ojalgo-56
     * code; they would change if the matrix operand order or element indexing
     * regressed, so they are the primary correctness guard for the upgrade.
     */
    @Test
    public void figmmProducesWellFormedCenteredPolygon() {
        Map<String, Object> zoi = new HashMap<>();
        zoi.put("covariance_matrix_inverse",
                MatrixR064.FACTORY.rows(new double[][]{{1e-6, 0.0}, {0.0, 4e-6}}));
        zoi.put("mean", new double[]{0.0, 0.0});

        String wkt = figmm.figmm(zoi);

        assertTrue("must be a WKT polygon", wkt.startsWith("POLYGON(("));
        assertTrue("must be closed", wkt.endsWith("))"));

        String body = wkt.substring("POLYGON((".length(), wkt.length() - 2);
        String[] vertices = body.split(",");
        // step = 8 -> floor(2*pi*8)+1 = 51 vertices
        assertEquals(51, vertices.length);

        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        for (String v : vertices) {
            String[] lonLat = v.trim().split(" ");
            assertEquals(2, lonLat.length);
            double lon = Double.parseDouble(lonLat[0]);
            double lat = Double.parseDouble(lonLat[1]);
            assertTrue(Double.isFinite(lon));
            assertTrue(Double.isFinite(lat));
            minLon = Math.min(minLon, lon);
            maxLon = Math.max(maxLon, lon);
            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
        }

        // The ellipse must be centred on the origin: the bounding-box centre is far
        // closer to (0,0) than the ellipse extent (tolerant of the open arc).
        double lonSpread = maxLon - minLon;
        double latSpread = maxLat - minLat;
        assertEquals(SphericalMercator.x2lon(0.0), (minLon + maxLon) / 2.0, 0.05 * lonSpread);
        assertEquals(SphericalMercator.y2lat(0.0), (minLat + maxLat) / 2.0, 0.05 * latSpread);

        // Golden values from the migrated ojalgo-56 pipeline.
        String[] firstVertex = vertices[0].trim().split(" ");
        assertEquals(0.013939658208976326, Double.parseDouble(firstVertex[0]), 1e-12);
        assertEquals(0.0, Double.parseDouble(firstVertex[1]), 1e-12);
        assertEquals(0.02787739755544058, lonSpread, 1e-12);
        assertEquals(0.027848985252955344, latSpread, 1e-12);
    }

    /**
     * Determinism guard: identical input must yield byte-identical WKT. Locks the
     * migrated maths against accidental drift on future dependency bumps.
     */
    @Test
    public void figmmIsDeterministic() {
        Map<String, Object> zoi = new HashMap<>();
        zoi.put("covariance_matrix_inverse",
                MatrixR064.FACTORY.rows(new double[][]{{2e-6, 1e-7}, {1e-7, 3e-6}}));
        zoi.put("mean", new double[]{1000.0, 2000.0});

        assertEquals(figmm.figmm(zoi), figmm.figmm(zoi));
    }
}
