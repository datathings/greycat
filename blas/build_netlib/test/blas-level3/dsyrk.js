var expect = require('expect.js'),
    emlapack = require('../../emlapack');
var dsyrk = emlapack.cwrap('f2c_dsyrk', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);

describe('dsyrk("U", "N", n, k, alpha, A, lda, beta, C, ldc)', function () {
  it('computes C := alpha * A * A^t + beta * C (upper triangular part of C is updated)', function () {
    var n = 2,
        k = 4,
        puplo = emlapack._malloc(1),
        ptrans = emlapack._malloc(1),
        pn = emlapack._malloc(4),
        pk = emlapack._malloc(4),
        palpha = emlapack._malloc(8),
        pa = emlapack._malloc(n * k * 8),
        plda = emlapack._malloc(4),
        pbeta = emlapack._malloc(8),
        pc = emlapack._malloc(n * n * 8),
        pldc = emlapack._malloc(4),
        a = new Float64Array(emlapack.HEAPF64.buffer, pa, n * k),
        c = new Float64Array(emlapack.HEAPF64.buffer, pc, n * n);

    emlapack.setValue(puplo, 'U'.charCodeAt(0), 'i8');
    emlapack.setValue(ptrans, 'N'.charCodeAt(0), 'i8');
    emlapack.setValue(pn, n, 'i32');
    emlapack.setValue(pk, k, 'i32');
    emlapack.setValue(palpha, 1 / k, 'double');
    emlapack.setValue(pbeta, 0, 'double');
    emlapack.setValue(plda, n, 'i32');
    emlapack.setValue(pldc, n, 'i32');

    a.set([0, 1, 2, 1, -2, -1, 0, -1]);

    dsyrk(puplo, ptrans, pn, pk, palpha, pa, plda, pbeta, pc, pldc);

    expect(c).to.be.eql([2, 0, 1, 1]);
  });
});
