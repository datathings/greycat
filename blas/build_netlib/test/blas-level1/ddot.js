var expect = require('expect.js'),
    emlapack = require('../../emlapack');
var ddot = emlapack.cwrap('f2c_ddot', null, ['number', 'number', 'number', 'number', 'number']);

describe('ddot(n, dx, incx, dy, incy)', function () {
  it('returns dx^t dy', function () {
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
    dy.set([2, 3, 4, 5]);

    expect(ddot(pn, pdx, pincx, pdy, pincy)).to.be(40);
  });
});
