package com.webgeoservices.woosmapgeofencingcore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.ojalgo.matrix.MatrixR064;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;

/**
 * Regression guard for the ojalgo upgrade (37.1.1 -> 56.2.1).
 *
 * <p>The upgrade removed {@code PrimitiveMatrix} (now {@code MatrixR064}) and the
 * {@code multiplyLeft}/{@code multiplyRight} methods used by
 * {@link FigmmForVisitsCreatorCore}. These tests pin the exact ojalgo semantics the
 * SDK relies on so any future ojalgo behavior change (operand order, element
 * indexing, eigen-decomposition) is caught here rather than silently corrupting
 * the ZOI clustering maths.
 */
public class OjalgoMatrixRegressionTest {

    private static final double EPS = 1e-9;

    /** Matrix * matrix must be the true matrix product (not element-wise). */
    @Test
    public void matrixMultiplyIsMatrixProduct() {
        MatrixR064 x = MatrixR064.FACTORY.rows(new double[][]{{1, 2}, {3, 4}});
        MatrixR064 y = MatrixR064.FACTORY.rows(new double[][]{{5, 6}, {7, 8}});

        MatrixR064 xy = x.multiply(y); // [[19,22],[43,50]]

        assertEquals(19.0, xy.get(0, 0), EPS);
        assertEquals(22.0, xy.get(0, 1), EPS);
        assertEquals(43.0, xy.get(1, 0), EPS);
        assertEquals(50.0, xy.get(1, 1), EPS);
    }

    /**
     * The migration replaced {@code a.multiplyLeft(b)} (== b*a) with
     * {@code b.multiply(a)}. This documents and locks that equivalence.
     */
    @Test
    public void multiplyLeftEquivalentToFlippedMultiply() {
        MatrixR064 a = MatrixR064.FACTORY.rows(new double[][]{{1, 2}, {3, 4}});
        MatrixR064 b = MatrixR064.FACTORY.rows(new double[][]{{5, 6}, {7, 8}});

        // old: a.multiplyLeft(b) == b * a == [[23,34],[31,46]]
        MatrixR064 flipped = b.multiply(a);

        assertEquals(23.0, flipped.get(0, 0), EPS);
        assertEquals(34.0, flipped.get(0, 1), EPS);
        assertEquals(31.0, flipped.get(1, 0), EPS);
        assertEquals(46.0, flipped.get(1, 1), EPS);
    }

    /** {@code get(long)} must remain column-major, as the SDK reads get(0..3). */
    @Test
    public void linearGetIsColumnMajor() {
        MatrixR064 x = MatrixR064.FACTORY.rows(new double[][]{{1, 2}, {3, 4}});
        assertEquals(1.0, x.get(0), EPS); // (0,0)
        assertEquals(3.0, x.get(1), EPS); // (1,0)
        assertEquals(2.0, x.get(2), EPS); // (0,1)
        assertEquals(4.0, x.get(3), EPS); // (1,1)
    }

    /** Scalar multiply and scalar add must stay element-wise scalar ops. */
    @Test
    public void scalarOps() {
        MatrixR064 x = MatrixR064.FACTORY.rows(new double[][]{{1, 2}, {3, 4}});
        assertEquals(2.0, x.multiply(2.0).get(0), EPS);
        assertEquals(2.0, x.add(1.0).get(0), EPS);
    }

    /** invert() must produce a true matrix inverse (M * M^-1 == I). */
    @Test
    public void invertProducesIdentity() {
        MatrixR064 m = MatrixR064.FACTORY.rows(new double[][]{{4, 7}, {2, 6}});
        MatrixR064 id = m.multiply(m.invert());
        assertEquals(1.0, id.get(0, 0), 1e-9);
        assertEquals(0.0, id.get(0, 1), 1e-9);
        assertEquals(0.0, id.get(1, 0), 1e-9);
        assertEquals(1.0, id.get(1, 1), 1e-9);
    }

    /**
     * Eigen-decomposition of a symmetric matrix used by figmm(): the diagonal of D
     * must hold the eigenvalues and V the (orthonormal) eigenvectors, with
     * V*D*V^T reconstructing the original matrix.
     */
    @Test
    public void eigenDecompositionReconstructsMatrix() {
        MatrixR064 c = MatrixR064.FACTORY.rows(new double[][]{{2, 1}, {1, 2}});
        Eigenvalue<Double> evd = Eigenvalue.R064.make(c);
        evd.decompose(c);
        MatrixStore<Double> v = evd.getV();
        PhysicalStore<Double> d = evd.getD().copy();

        // Eigenvalues of [[2,1],[1,2]] are 1 and 3 (in some order).
        double e0 = d.get(0, 0);
        double e1 = d.get(1, 1);
        double min = Math.min(e0, e1);
        double max = Math.max(e0, e1);
        assertEquals(1.0, min, 1e-9);
        assertEquals(3.0, max, 1e-9);

        // V * D * V^T == C
        MatrixR064 vM = MatrixR064.FACTORY.rows(new double[][]{
                {v.get(0, 0), v.get(0, 1)}, {v.get(1, 0), v.get(1, 1)}});
        MatrixR064 dM = MatrixR064.FACTORY.rows(new double[][]{
                {d.get(0, 0), d.get(0, 1)}, {d.get(1, 0), d.get(1, 1)}});
        MatrixR064 recon = vM.multiply(dM).multiply(vM.transpose());
        assertEquals(2.0, recon.get(0, 0), 1e-9);
        assertEquals(1.0, recon.get(0, 1), 1e-9);
        assertEquals(1.0, recon.get(1, 0), 1e-9);
        assertEquals(2.0, recon.get(1, 1), 1e-9);
    }

    /**
     * Mahalanobis-style quadratic form error * covInv * error^T must be a positive
     * scalar; this is the core operation used when scoring visit points.
     */
    @Test
    public void mahalanobisQuadraticFormIsScalar() {
        MatrixR064 covInv = MatrixR064.FACTORY.rows(new double[][]{{0.25, 0.0}, {0.0, 1.0}});
        MatrixR064 error = MatrixR064.FACTORY.rows(new double[][]{{2.0, 3.0}, {0.0, 0.0}});

        // error * covInv * error^T  ->  2^2*0.25 + 3^2*1 = 1 + 9 = 10
        MatrixR064 result = error.multiply(covInv).multiply(error.transpose());
        assertEquals(10.0, result.get(0), EPS);
        assertTrue(result.get(0) > 0);
    }
}
