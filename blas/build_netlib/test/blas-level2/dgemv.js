var expect = require('expect.js'),
    emlapack = require('../../emlapack');
var dgemv = emlapack.cwrap('f2c_dgemv', null, ['number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);

describe('dgemv("N", m, n, alpha, A, lda, x, incx, beta, y, incy)', function () {
  it('computes y := alpha*A*x + beta*y', function () {
    var m = 3,
        n = 2,
        ptrans = emlapack._malloc(1),
        pm = emlapack._malloc(4),
        pn = emlapack._malloc(4),
        palpha = emlapack._malloc(8),
        pA = emlapack._malloc(n * m * 8),
        plda = emlapack._malloc(4),
        px = emlapack._malloc(n * 8),
        pincx = emlapack._malloc(4),
        pbeta = emlapack._malloc(8),
        py = emlapack._malloc(m * 8),
        pincy = emlapack._malloc(4),
        A = new Float64Array(emlapack.HEAPF64.buffer, pA, n * m),
        x = new Float64Array(emlapack.HEAPF64.buffer, px, n),
        y = new Float64Array(emlapack.HEAPF64.buffer, py, m);

    emlapack.setValue(ptrans, 'N'.charCodeAt(0), 'i8');
    emlapack.setValue(pm, m, 'i32');
    emlapack.setValue(pn, n, 'i32');
    emlapack.setValue(palpha, 2, 'double');
    emlapack.setValue(pbeta, 3, 'double');
    emlapack.setValue(plda, m, 'i32');
    emlapack.setValue(pincx, 1, 'i32');
    emlapack.setValue(pincy, 1, 'i32');

    A.set([1, 2, 3, 1, 2, 3]);
    x.set([1, 2]);
    y.set([1, 1, 1]);

    dgemv(ptrans, pm, pn, palpha, pA, plda, px, pincx, pbeta, py, pincy);

    expect(y).to.be.eql([9, 15, 21]);
  });
});
