/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.blas;

import greycat.struct.matrix.TransposeType;

/**
 * @native ts
 * private netlib;
 * private c_dgemm;
 * private c_dgetrs;
 * private c_dgetri;
 * private c_dorgqr;
 * private c_dgesdd;
 * private c_dgeqrf;
 * private c_dgetrf;
 * constructor() {
 * this.netlib = require('./netlib');
 * this.c_dgemm = this.netlib.cwrap('f2c_dgemm', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);
 * this.c_dgetrs = this.netlib.cwrap('dgetrs_', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);
 * this.c_dgetri = this.netlib.cwrap('dgetri_', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number']);
 * this.c_dorgqr = this.netlib.cwrap('dorgqr_', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);
 * this.c_dgesdd = this.netlib.cwrap('dgesdd_', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);
 * this.c_dgeqrf = this.netlib.cwrap('dgeqrf_', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);
 * this.c_dgetrf = this.netlib.cwrap('dgetrf_', null, ['number', 'number', 'number', 'number', 'number', 'number']);
 * }
 * dgemm(transA: greycat.struct.matrix.TransposeType, transB: greycat.struct.matrix.TransposeType, m: number, n: number, k: number, alpha: number, matA: Float64Array, offsetA: number, ldA: number, matB: Float64Array, offsetB: number, ldB: number, beta: number, matC: Float64Array, offsetC: number, ldC: number): void {
 * var ptransA = this.netlib._malloc(1),
 * ptransB = this.netlib._malloc(1),
 * pm = this.netlib._malloc(4),
 * pn = this.netlib._malloc(4),
 * pk = this.netlib._malloc(4),
 * palpha = this.netlib._malloc(8),
 * pmatA = this.netlib._malloc(8 * matA.length),
 * pldA = this.netlib._malloc(4),
 * pmatB = this.netlib._malloc(8 * matB.length),
 * pldB = this.netlib._malloc(4),
 * pbeta = this.netlib._malloc(8),
 * pmatC = this.netlib._malloc(8 * matC.length),
 * pldC = this.netlib._malloc(4);
 * this.netlib.setValue(ptransA, greycat.blas.BlasHelper.transTypeToChar(transA).charCodeAt(0), 'i8');
 * this.netlib.setValue(ptransB, greycat.blas.BlasHelper.transTypeToChar(transB).charCodeAt(0), 'i8');
 * this.netlib.setValue(pm, m, 'i32');
 * this.netlib.setValue(pn, n, 'i32');
 * this.netlib.setValue(pk, k, 'i32');
 * this.netlib.setValue(palpha, alpha, 'double');
 * var ddpmatA = new Float64Array(this.netlib.HEAPF64.buffer, pmatA, matA.length);
 * ddpmatA.set(matA);
 * this.netlib.setValue(pldA, ldA, 'i32');
 * var ddpmatB = new Float64Array(this.netlib.HEAPF64.buffer, pmatB, matB.length);
 * ddpmatB.set(matB);
 * this.netlib.setValue(pldB, ldB, 'i32');
 * this.netlib.setValue(pbeta, beta, 'double');
 * var ddpmatC = new Float64Array(this.netlib.HEAPF64.buffer, pmatC, matC.length);
 * ddpmatC.set(matC);
 * this.netlib.setValue(pldC, ldC, 'i32');
 * this.c_dgemm(ptransA, ptransB, pm, pn, pk, palpha, pmatA, pldA, pmatB, pldB, pbeta, pmatC, pldC);
 * // matA.set(ddpmatA);
 * // matB.set(ddpmatB);
 * matC.set(ddpmatC);
 * this.netlib._free(ptransA);
 * this.netlib._free(ptransB);
 * this.netlib._free(pm);
 * this.netlib._free(pn);
 * this.netlib._free(pk);
 * this.netlib._free(palpha);
 * this.netlib._free(pmatA);
 * this.netlib._free(pldA);
 * this.netlib._free(pmatB);
 * this.netlib._free(pldB);
 * this.netlib._free(pbeta);
 * this.netlib._free(pmatC);
 * this.netlib._free(pldC);
 * }
 * dgetrs(transA: greycat.struct.matrix.TransposeType, dim: number, nrhs: number, matA: Float64Array, offsetA: number, ldA: number, ipiv: Int32Array, offsetIpiv: number, matB: Float64Array, offsetB: number, ldB: number, info: Int32Array): void {
 * var ptransA = this.netlib._malloc(1),
 * pdim = this.netlib._malloc(4),
 * pnrhs = this.netlib._malloc(4),
 * pmatA = this.netlib._malloc(8 * matA.length),
 * pldA = this.netlib._malloc(4),
 * pipiv = this.netlib._malloc(4 * ipiv.length),
 * pmatB = this.netlib._malloc(8 * matB.length),
 * pldB = this.netlib._malloc(4),
 * pinfo = this.netlib._malloc(4 * info.length);
 * this.netlib.setValue(ptransA, greycat.blas.BlasHelper.transTypeToChar(transA).charCodeAt(0), 'i8');
 * this.netlib.setValue(pdim, dim, 'i32');
 * this.netlib.setValue(pnrhs, nrhs, 'i32');
 * var ddpmatA = new Float64Array(this.netlib.HEAPF64.buffer, pmatA, matA.length);
 * ddpmatA.set(matA);
 * this.netlib.setValue(pldA, ldA, 'i32');
 * var iipipiv = new Int32Array(this.netlib.HEAP32.buffer, pipiv, ipiv.length);
 * iipipiv.set(ipiv);
 * var ddpmatB = new Float64Array(this.netlib.HEAPF64.buffer, pmatB, matB.length);
 * ddpmatB.set(matB);
 * this.netlib.setValue(pldB, ldB, 'i32');
 * var iipinfo = new Int32Array(this.netlib.HEAP32.buffer, pinfo, info.length);
 * iipinfo.set(info);
 * this.c_dgetrs(ptransA, pdim, pnrhs, pmatA, pldA, pipiv, pmatB, pldB, pinfo);
 * //  matA.set(ddpmatA);
 * // ipiv.set(iipipiv);
 * matB.set(ddpmatB);
 * info.set(iipinfo);
 * this.netlib._free(ptransA);
 * this.netlib._free(pdim);
 * this.netlib._free(pnrhs);
 * this.netlib._free(pmatA);
 * this.netlib._free(pldA);
 * this.netlib._free(pipiv);
 * this.netlib._free(pmatB);
 * this.netlib._free(pldB);
 * this.netlib._free(pinfo);
 * }
 * dgetri(dim: number, matA: Float64Array, offsetA: number, ldA: number, ipiv: Int32Array, offsetIpiv: number, work: Float64Array, offsetWork: number, ldWork: number, info: Int32Array): void {
 * var pdim = this.netlib._malloc(4),
 * pmatA = this.netlib._malloc(8 * matA.length),
 * pldA = this.netlib._malloc(4),
 * pipiv = this.netlib._malloc(4 * ipiv.length),
 * pwork = this.netlib._malloc(8 * work.length),
 * pldWork = this.netlib._malloc(4),
 * pinfo = this.netlib._malloc(4 * info.length);
 * this.netlib.setValue(pdim, dim, 'i32');
 * var ddpmatA = new Float64Array(this.netlib.HEAPF64.buffer, pmatA, matA.length);
 * ddpmatA.set(matA);
 * this.netlib.setValue(pldA, ldA, 'i32');
 * var iipipiv = new Int32Array(this.netlib.HEAP32.buffer, pipiv, ipiv.length);
 * iipipiv.set(ipiv);
 * var ddpwork = new Float64Array(this.netlib.HEAPF64.buffer, pwork, work.length);
 * ddpwork.set(work);
 * this.netlib.setValue(pldWork, ldWork, 'i32');
 * var iipinfo = new Int32Array(this.netlib.HEAP32.buffer, pinfo, info.length);
 * iipinfo.set(info);
 * this.c_dgetri(pdim, pmatA, pldA, pipiv, pwork, pldWork, pinfo);
 * matA.set(ddpmatA);
 * //ipiv.set(iipipiv);
 * work.set(ddpwork);
 * info.set(iipinfo);
 * this.netlib._free(pdim);
 * this.netlib._free(pmatA);
 * this.netlib._free(pldA);
 * this.netlib._free(pipiv);
 * this.netlib._free(pwork);
 * this.netlib._free(pldWork);
 * this.netlib._free(pinfo);
 * }
 * dgetrf(rows: number, columns: number, matA: Float64Array, offsetA: number, ldA: number, ipiv: Int32Array, offsetIpiv: number, info: Int32Array): void {
 * var prows = this.netlib._malloc(4),
 * pcolumns = this.netlib._malloc(4),
 * pmatA = this.netlib._malloc(8 * matA.length),
 * pldA = this.netlib._malloc(4),
 * pipiv = this.netlib._malloc(4 * ipiv.length),
 * pinfo = this.netlib._malloc(4 * info.length);
 * this.netlib.setValue(prows, rows, 'i32');
 * this.netlib.setValue(pcolumns, columns, 'i32');
 * var ddpmatA = new Float64Array(this.netlib.HEAPF64.buffer, pmatA, matA.length);
 * ddpmatA.set(matA);
 * this.netlib.setValue(pldA, ldA, 'i32');
 * var iipipiv = new Int32Array(this.netlib.HEAP32.buffer, pipiv, ipiv.length);
 * iipipiv.set(ipiv);
 * var iipinfo = new Int32Array(this.netlib.HEAP32.buffer, pinfo, info.length);
 * iipinfo.set(info);
 * this.c_dgetrf(prows, pcolumns, pmatA, pldA, pipiv, pinfo);
 * matA.set(ddpmatA);
 * ipiv.set(iipipiv);
 * info.set(iipinfo);
 * this.netlib._free(prows);
 * this.netlib._free(pcolumns);
 * this.netlib._free(pmatA);
 * this.netlib._free(pldA);
 * this.netlib._free(pipiv);
 * this.netlib._free(pinfo);
 * }
 * dorgqr(m: number, n: number, k: number, matA: Float64Array, offsetA: number, ldA: number, taw: Float64Array, offsetTaw: number, work: Float64Array, offsetWork: number, lWork: number, info: Int32Array): void {
 * var pm = this.netlib._malloc(4),
 * pn = this.netlib._malloc(4),
 * pk = this.netlib._malloc(4),
 * pmatA = this.netlib._malloc(8 * matA.length),
 * pldA = this.netlib._malloc(4),
 * ptaw = this.netlib._malloc(8 * taw.length),
 * pwork = this.netlib._malloc(8 * work.length),
 * plWork = this.netlib._malloc(4),
 * pinfo = this.netlib._malloc(4 * info.length);
 * this.netlib.setValue(pm, m, 'i32');
 * this.netlib.setValue(pn, n, 'i32');
 * this.netlib.setValue(pk, k, 'i32');
 * var ddpmatA = new Float64Array(this.netlib.HEAPF64.buffer, pmatA, matA.length);
 * ddpmatA.set(matA);
 * this.netlib.setValue(pldA, ldA, 'i32');
 * var ddptaw = new Float64Array(this.netlib.HEAPF64.buffer, ptaw, taw.length);
 * ddptaw.set(taw);
 * var ddpwork = new Float64Array(this.netlib.HEAPF64.buffer, pwork, work.length);
 * ddpwork.set(work);
 * this.netlib.setValue(plWork, lWork, 'i32');
 * var iipinfo = new Int32Array(this.netlib.HEAP32.buffer, pinfo, info.length);
 * iipinfo.set(info);
 * this.c_dorgqr(pm, pn, pk, pmatA, pldA, ptaw, pwork, plWork, pinfo);
 * matA.set(ddpmatA);
 * //taw.set(ddptaw);
 * work.set(ddpwork);
 * info.set(iipinfo);
 * this.netlib._free(pm);
 * this.netlib._free(pn);
 * this.netlib._free(pk);
 * this.netlib._free(pmatA);
 * this.netlib._free(pldA);
 * this.netlib._free(ptaw);
 * this.netlib._free(pwork);
 * this.netlib._free(plWork);
 * this.netlib._free(pinfo);
 * }
 * dgeqrf(m: number, n: number, matA: Float64Array, offsetA: number, ldA: number, taw: Float64Array, offsetTaw: number, work: Float64Array, offsetwork: number, lWork: number, info: Int32Array): void {
 * var pm = this.netlib._malloc(4),
 * pn = this.netlib._malloc(4),
 * pmatA = this.netlib._malloc(8 * matA.length),
 * pldA = this.netlib._malloc(4),
 * ptaw = this.netlib._malloc(8 * taw.length),
 * pwork = this.netlib._malloc(8 * work.length),
 * plWork = this.netlib._malloc(4),
 * pinfo = this.netlib._malloc(4 * info.length);
 * this.netlib.setValue(pm, m, 'i32');
 * this.netlib.setValue(pn, n, 'i32');
 * var ddpmatA = new Float64Array(this.netlib.HEAPF64.buffer, pmatA, matA.length);
 * ddpmatA.set(matA);
 * this.netlib.setValue(pldA, ldA, 'i32');
 * var ddptaw = new Float64Array(this.netlib.HEAPF64.buffer, ptaw, taw.length);
 * ddptaw.set(taw);
 * var ddpwork = new Float64Array(this.netlib.HEAPF64.buffer, pwork, work.length);
 * ddpwork.set(work);
 * this.netlib.setValue(plWork, lWork, 'i32');
 * var iipinfo = new Int32Array(this.netlib.HEAP32.buffer, pinfo, info.length);
 * iipinfo.set(info);
 * this.c_dgeqrf(pm, pn, pmatA, pldA, ptaw, pwork, plWork, pinfo);
 * matA.set(ddpmatA);
 * taw.set(ddptaw);
 * work.set(ddpwork);
 * info.set(iipinfo);
 * this.netlib._free(pm);
 * this.netlib._free(pn);
 * this.netlib._free(pmatA);
 * this.netlib._free(pldA);
 * this.netlib._free(ptaw);
 * this.netlib._free(pwork);
 * this.netlib._free(plWork);
 * this.netlib._free(pinfo);
 * }
 * dgesdd(jobz: string, m: number, n: number, data: Float64Array, lda: number, s: Float64Array, u: Float64Array, ldu: number, vt: Float64Array, ldvt: number, work: Float64Array, length: number, iwork: Int32Array, info: Int32Array): void {
 * var pjobz = this.netlib._malloc(1),
 * pm = this.netlib._malloc(4),
 * pn = this.netlib._malloc(4),
 * pdata = this.netlib._malloc(8 * data.length),
 * plda = this.netlib._malloc(4),
 * ps = this.netlib._malloc(8 * s.length),
 * pu = this.netlib._malloc(8 * u.length),
 * pldu = this.netlib._malloc(4),
 * pvt = this.netlib._malloc(8 * vt.length),
 * pldvt = this.netlib._malloc(4),
 * pwork = this.netlib._malloc(8 * work.length),
 * plength = this.netlib._malloc(4),
 * piwork = this.netlib._malloc(4 * iwork.length),
 * pinfo = this.netlib._malloc(4 * info.length);
 * this.netlib.setValue(pjobz, jobz.charCodeAt(0), 'i8');
 * this.netlib.setValue(pm, m, 'i32');
 * this.netlib.setValue(pn, n, 'i32');
 * var ddpdata = new Float64Array(this.netlib.HEAPF64.buffer, pdata, data.length);
 * ddpdata.set(data);
 * this.netlib.setValue(plda, lda, 'i32');
 * var ddps = new Float64Array(this.netlib.HEAPF64.buffer, ps, s.length);
 * ddps.set(s);
 * var ddpu = new Float64Array(this.netlib.HEAPF64.buffer, pu, u.length);
 * ddpu.set(u);
 * this.netlib.setValue(pldu, ldu, 'i32');
 * var ddpvt = new Float64Array(this.netlib.HEAPF64.buffer, pvt, vt.length);
 * ddpvt.set(vt);
 * this.netlib.setValue(pldvt, ldvt, 'i32');
 * var ddpwork = new Float64Array(this.netlib.HEAPF64.buffer, pwork, work.length);
 * ddpwork.set(work);
 * this.netlib.setValue(plength, length, 'i32');
 * var iipiwork = new Int32Array(this.netlib.HEAP32.buffer, piwork, iwork.length);
 * iipiwork.set(iwork);
 * var iipinfo = new Int32Array(this.netlib.HEAP32.buffer, pinfo, info.length);
 * iipinfo.set(info);
 * this.c_dgesdd(pjobz, pm, pn, pdata, plda, ps, pu, pldu, pvt, pldvt, pwork, plength, piwork, pinfo);
 * data.set(ddpdata);
 * s.set(ddps);
 * u.set(ddpu);
 * vt.set(ddpvt);
 * work.set(ddpwork);
 * iwork.set(iipiwork);
 * info.set(iipinfo);
 * this.netlib._free(pjobz);
 * this.netlib._free(pm);
 * this.netlib._free(pn);
 * this.netlib._free(pdata);
 * this.netlib._free(plda);
 * this.netlib._free(ps);
 * this.netlib._free(pu);
 * this.netlib._free(pldu);
 * this.netlib._free(pvt);
 * this.netlib._free(pldvt);
 * this.netlib._free(pwork);
 * this.netlib._free(plength);
 * this.netlib._free(piwork);
 * this.netlib._free(pinfo);
 * }
 * connect(): void {}
 * disconnect(): void {}
 */
public class JSBlas implements Blas {
    @Override
    public void dgemm(TransposeType transA, TransposeType transB, int m, int n, int k, double alpha, double[] matA, int offsetA, int ldA, double[] matB, int offsetB, int ldB, double beta, double[] matC, int offsetC, int ldC) {

    }

    @Override
    public void dgetrs(TransposeType transA, int dim, int nrhs, double[] matA, int offsetA, int ldA, int[] ipiv, int offsetIpiv, double[] matB, int offsetB, int ldB, int[] info) {

    }

    @Override
    public void dgetri(int dim, double[] matA, int offsetA, int ldA, int[] ipiv, int offsetIpiv, double[] work, int offsetWork, int ldWork, int[] info) {

    }

    @Override
    public void dgetrf(int rows, int columns, double[] matA, int offsetA, int ldA, int[] ipiv, int offsetIpiv, int[] info) {

    }

    @Override
    public void dorgqr(int m, int n, int k, double[] matA, int offsetA, int ldA, double[] taw, int offsetTaw, double[] work, int offsetWork, int lWork, int[] info) {

    }

    @Override
    public void dgeqrf(int m, int n, double[] matA, int offsetA, int ldA, double[] taw, int offsetTaw, double[] work, int offsetwork, int lWork, int[] info) {

    }

    @Override
    public void dgesdd(String jobz, int m, int n, double[] data, int lda, double[] s, double[] u, int ldu, double[] vt, int ldvt, double[] work, int length, int[] iwork, int[] info) {

    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }
}
