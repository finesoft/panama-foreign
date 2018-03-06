/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have
 * questions.
 */

/*
 * @test
 * @modules jdk.incubator.vector
 * @run testng Int512VectorTests
 *
 */

import jdk.incubator.vector.Shapes;
import jdk.incubator.vector.Vector;

import jdk.incubator.vector.IntVector;

import org.testng.Assert;

@org.testng.annotations.Test
public class Int512VectorTests {
    static final int SIZE = 512 * 1000;
    static final IntVector.IntSpecies<Shapes.S512Bit> species = (IntVector.IntSpecies<Shapes.S512Bit>)
                Vector.speciesInstance(Integer.class, Shapes.S_512_BIT);

    static void init_mask(boolean[] t, int size) {
        for (int i = 0; i < size; i++) {
          t[i] = (i%2 == 0);
        }
    }

    static void init_arr1(int[] t) {
        for (int i = 0; i < t.length; i++) {
            t[i] = (int)(i*5);
        }
    }

    static void init_arr2(int[] t) {
        for (int i = 0; i < t.length; i++) {
            t[i] = (((int)(i+1) == 0) ? 1 : (int)(i+1));
        }
    }

    interface FUnOp {
        int apply(int a);
    }

    static void assertArraysEquals(int[] a, int[] r, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(f.apply(a[i]), r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i]), r[i], "at index #" + i);
        }
    }

    static void assertArraysEquals(int[] a, int[] r, boolean[] mask, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(mask[i % species.length()] ? f.apply(a[i]) : a[i], r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(mask[i % species.length()] ? f.apply(a[i]) : a[i], r[i], "at index #" + i);
        }
    }

    interface FBinOp {
        int apply(int a, int b);
    }

    interface FBinMaskOp {
        int apply(int a, int b, boolean m);

        static FBinMaskOp lift(FBinOp f) {
            return (a, b, m) -> m ? f.apply(a, b) : a;
        }
    }

    static void assertArraysEquals(int[] a, int[] b, int[] r, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(f.apply(a[i], b[i]), r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i], b[i]), r[i], "at index #" + i);
        }
    }

    static void assertArraysEquals(int[] a, int[] b, int[] r, boolean[] mask, FBinOp f) {
        assertArraysEquals(a, b, r, mask, FBinMaskOp.lift(f));
    }

    static void assertArraysEquals(int[] a, int[] b, int[] r, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(f.apply(a[i], b[i], mask[i % species.length()]), r[i]);
            }
        } catch (AssertionError err) {
            Assert.assertEquals(f.apply(a[i], b[i], mask[i % species.length()]), r[i], "at index #" + i + ", a[i] = " + a[i] + ", b[i] = " + b[i] + ", mask = " + mask[i % species.length()]);
        }
    }

    static int add(int a, int b) {
        return (int)(a + b);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void addInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.add(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::add);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void addMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);
        init_arr2(b);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.add(bv, tMask).intoArray(rT, i);
            av.add(bv, fMask).intoArray(rF, i);
            av.add(bv, pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, b, rT, tMask.toArray(), Int512VectorTests::add);
        assertArraysEquals(a, b, rF, fMask.toArray(), Int512VectorTests::add);
        assertArraysEquals(a, b, rM, mask, Int512VectorTests::add);
    }

    static int sub(int a, int b) {
        return (int)(a - b);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void subInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.sub(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::sub);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void subMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);
        init_arr2(b);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.sub(bv, tMask).intoArray(rT, i);
            av.sub(bv, fMask).intoArray(rF, i);
            av.sub(bv, pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, b, rT, tMask.toArray(), Int512VectorTests::sub);
        assertArraysEquals(a, b, rF, fMask.toArray(), Int512VectorTests::sub);
        assertArraysEquals(a, b, rM, mask, Int512VectorTests::sub);
    }

    static int div(int a, int b) {
        return (int)(a / b);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void divInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.div(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::div);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void divMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);
        init_arr2(b);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.div(bv, tMask).intoArray(rT, i);
            av.div(bv, fMask).intoArray(rF, i);
            av.div(bv, pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, b, rT, tMask.toArray(), Int512VectorTests::div);
        assertArraysEquals(a, b, rF, fMask.toArray(), Int512VectorTests::div);
        assertArraysEquals(a, b, rM, mask, Int512VectorTests::div);
    }

    static int mul(int a, int b) {
        return (int)(a * b);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void mulInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.mul(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::mul);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void mulMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);
        init_arr2(b);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.mul(bv, tMask).intoArray(rT, i);
            av.mul(bv, fMask).intoArray(rF, i);
            av.mul(bv, pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, b, rT, tMask.toArray(), Int512VectorTests::mul);
        assertArraysEquals(a, b, rF, fMask.toArray(), Int512VectorTests::mul);
        assertArraysEquals(a, b, rM, mask, Int512VectorTests::mul);
    }


    static int and(int a, int b) {
        return (int)(a & b);
    }



    @org.testng.annotations.Test(invocationCount = 10)
    static void andInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.and(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::and);
    }



    @org.testng.annotations.Test(invocationCount = 10)
    static void andMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);
        init_arr2(b);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.and(bv, tMask).intoArray(rT, i);
            av.and(bv, fMask).intoArray(rF, i);
            av.and(bv, pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, b, rT, tMask.toArray(), Int512VectorTests::and);
        assertArraysEquals(a, b, rF, fMask.toArray(), Int512VectorTests::and);
        assertArraysEquals(a, b, rM, mask, Int512VectorTests::and);
    }



    static int or(int a, int b) {
        return (int)(a | b);
    }



    @org.testng.annotations.Test(invocationCount = 10)
    static void orInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.or(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::or);
    }



    @org.testng.annotations.Test(invocationCount = 10)
    static void orMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);
        init_arr2(b);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.or(bv, tMask).intoArray(rT, i);
            av.or(bv, fMask).intoArray(rF, i);
            av.or(bv, pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, b, rT, tMask.toArray(), Int512VectorTests::or);
        assertArraysEquals(a, b, rF, fMask.toArray(), Int512VectorTests::or);
        assertArraysEquals(a, b, rM, mask, Int512VectorTests::or);
    }



    static int xor(int a, int b) {
        return (int)(a ^ b);
    }



    @org.testng.annotations.Test(invocationCount = 10)
    static void xorInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.xor(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::xor);
    }



    @org.testng.annotations.Test(invocationCount = 10)
    static void xorMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);
        init_arr2(b);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.xor(bv, tMask).intoArray(rT, i);
            av.xor(bv, fMask).intoArray(rF, i);
            av.xor(bv, pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, b, rT, tMask.toArray(), Int512VectorTests::xor);
        assertArraysEquals(a, b, rF, fMask.toArray(), Int512VectorTests::xor);
        assertArraysEquals(a, b, rM, mask, Int512VectorTests::xor);
    }


    static int max(int a, int b) {
        return (int)(Math.max(a, b));
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void maxInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.max(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::max);
    }

    static int min(int a, int b) {
        return (int)(Math.min(a, b));
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void minInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);
        init_arr2(b);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.min(bv).intoArray(r, i);
        }

        assertArraysEquals(a, b, r, Int512VectorTests::min);
    }


    static int blend(int a, int b, boolean mask) {
        return mask ? b : a;
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void blendInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] b = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);
        init_arr2(b);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            IntVector<Shapes.S512Bit> bv = species.fromArray(b, i);
            av.blend(bv, tMask).intoArray(rT, i);
            av.blend(bv, fMask).intoArray(rF, i);
            av.blend(bv, pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, b, rT, tMask.toArray(), Int512VectorTests::blend);
        assertArraysEquals(a, b, rF, fMask.toArray(), Int512VectorTests::blend);
        assertArraysEquals(a, b, rM, mask, Int512VectorTests::blend);
    }
    static int neg(int a) {
        return (int)(-((int)a));
    }

   @org.testng.annotations.Test(invocationCount = 10)
    static void negInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            av.neg().intoArray(r, i);
        }

        assertArraysEquals(a, r, Int512VectorTests::neg);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void negMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            av.neg(tMask).intoArray(rT, i);
            av.neg(fMask).intoArray(rF, i);
            av.neg(pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, rT, tMask.toArray(), Int512VectorTests::neg);
        assertArraysEquals(a, rF, fMask.toArray(), Int512VectorTests::neg);
        assertArraysEquals(a, rM, mask, Int512VectorTests::neg);
    }

    static int abs(int a) {
        return (int)(Math.abs((int)a));
    }

   @org.testng.annotations.Test(invocationCount = 10)
    static void absInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] r = new int[SIZE];

        // Data Initialization.
        init_arr1(a);

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            av.abs().intoArray(r, i);
        }

        assertArraysEquals(a, r, Int512VectorTests::abs);
    }

    @org.testng.annotations.Test(invocationCount = 10)
    static void absMaskedInt512VectorTests() {
        int[] a = new int[SIZE];
        int[] rT = new int[SIZE];
        int[] rF = new int[SIZE];
        int[] rM = new int[SIZE];
        boolean[] mask = new boolean[species.length()];
        
        // Data Initialization.
        init_mask(mask, species.length());
        init_arr1(a);

        Vector.Mask<Integer, Shapes.S512Bit> pMask = species.constantMask(mask);
        Vector.Mask<Integer, Shapes.S512Bit> tMask = species.trueMask();
        Vector.Mask<Integer, Shapes.S512Bit> fMask = species.falseMask();

        // Computation.
        for (int i = 0; i < a.length; i += species.length()) {
            IntVector<Shapes.S512Bit> av = species.fromArray(a, i);
            av.abs(tMask).intoArray(rT, i);
            av.abs(fMask).intoArray(rF, i);
            av.abs(pMask).intoArray(rM, i);
        }

        assertArraysEquals(a, rT, tMask.toArray(), Int512VectorTests::abs);
        assertArraysEquals(a, rF, fMask.toArray(), Int512VectorTests::abs);
        assertArraysEquals(a, rM, mask, Int512VectorTests::abs);
    }

}

