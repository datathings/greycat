var expect = require('expect.js');
var emlapack = require('../../emlapack');

describe('dgemm(transa, transb, m, n, k, alpha, a, lda, b, ldb, beta, c, ldc)', function () {
  it('computes c := alpha * a * b + beta * c', function () {
    var ptransa = emlapack._malloc(1),
        ptransb = emlapack._malloc(1),
        pm = emlapack._malloc(4),
        pn = emlapack._malloc(4),
        pk = emlapack._malloc(4),
        palpha = emlapack._malloc(8),
        pa = emlapack._malloc(128),
        plda = emlapack._malloc(4),
        pb = emlapack._malloc(128),
        pldb = emlapack._malloc(4),
        pbeta = emlapack._malloc(8),
        pc = emlapack._malloc(128),
        pldc = emlapack._malloc(4);

    emlapack.setValue(ptransa, 'N'.charCodeAt(0), 'i8');
    emlapack.setValue(ptransb, 'N'.charCodeAt(0), 'i8');
    emlapack.setValue(pm, 4, 'i32');
    emlapack.setValue(pn, 4, 'i32');
    emlapack.setValue(pk, 4, 'i32');
    emlapack.setValue(palpha, 1.5, 'double');
    emlapack.setValue(plda, 4, 'i32');
    emlapack.setValue(pldb, 4, 'i32');
    emlapack.setValue(pbeta, 2.5, 'double');
    emlapack.setValue(pldc, 4, 'i32');

    var a = new Float64Array(emlapack.HEAPF64.buffer, pa, 16);
    var b = new Float64Array(emlapack.HEAPF64.buffer, pb, 16);
    var c = new Float64Array(emlapack.HEAPF64.buffer, pc, 16);
    a.set([1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]);
    b.set([1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]);
    c.set([1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]);

    var dgemm = emlapack.cwrap('f2c_dgemm', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);
    dgemm(ptransa, ptransb, pm, pn, pk, palpha, pa, plda, pb, pldb, pbeta, pc, pldc);

    expect(c).to.be.eql([8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5, 8.5]);
  });
});
