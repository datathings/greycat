var expect = require('expect.js'),
    emlapack = require('../../emlapack');
var dcopy = emlapack.cwrap('f2c_dcopy', null, ['number', 'number', 'number', 'number', 'number']);

describe('dcopy(n, dx, incx, dy, incy)', function () {
  it('computes dy := dx', function () {
    var n = 4,
        pn = emlapack._malloc(4),
        pdx = emlapack._malloc(n * 8),
        pincx = emlapack._malloc(4),
        pdy = emlapack._malloc(n * 8),
        pincy = emlapack._malloc(4),
        dx = new Float64Array(emlapack.HEAPF64.buffer, pdx, n),
        dy = new Float64Array(emlapack.HEAPF64.buffer, pdy, n);

    emlapack.setValue(pn, n, 'i32');
    emlapack.setValue(pincx, 1, 'i32');
    emlapack.setValue(pincy, 1, 'i32');
    dx.set([1, 2, 3, 4]);

    dcopy(pn, pdx, pincx, pdy, pincy);

    expect(dy).to.be.eql([1, 2, 3, 4]);
  });
});
