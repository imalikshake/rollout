"use strict";

var cv = require("opencv");

var width = 5;
var height = 5.0;

//var corners = [0, 0, 4, 1, 3.4, 15, -0.5, 13.7];
var corners = [0, 0, 4, 0, 4, 4, 0, 4];
var target = [0, 0, width, 0, width, height, 0, height];

var transform = new cv.Matrix.Zeros(3, 3).getPerspectiveTransform(corners, target);

console.log(transform.row(0));
console.log(transform.row(1));
console.log(transform.row(2));
console.log();

var points = [3.5, 1.765, 0.34, 2.25];

console.log(warpPerspectiveList(points, transform));

function warpPerspectiveList(p, m) {
    var q = [];
    for (var i = 0; i < p.length; i += 2) {
        q.push(warpPerspective(p, m, i));
    }
    return q;
}

function warpPerspective(p, m, i) {
    if (typeof(i) === "undefined") {
        i = 0;
    }

    var q = [
        p[i + 0] * m.get(0, 0) + p[i + 1] * m.get(0, 1) + m.get(0, 2),
        p[i + 0] * m.get(1, 0) + p[i + 1] * m.get(1, 1) + m.get(1, 2)
    ];

    var f = p[i + 0] * m.get(2, 0) + p[i + 1] * m.get(2, 1) + m.get(2, 2);

    q[i + 0] /= f;
    q[i + 1] /= f;

    return q;
}

// var cv = require('opencv');
//
// cv.readImage("./mpi.png", function(err, im) {
//   if (err) throw err;
//
//   var width = im.width();
//   var height = im.height();
//   if (width < 1 || height < 1) throw new Error('Image has no size');
//
//   var srcArray = [0, 0, width, 0, width, height, 0, height];
//   var dstArray = [0, 0, width * 0.9, height * 0.1, width, height, width * 0.2, height * 0.8];
//   var xfrmMat = im.getPerspectiveTransform(srcArray, dstArray);
//   console.log(xfrmMat.row(0));
//   console.log(xfrmMat.row(1));
//   console.log(xfrmMat.row(2));
//   im.warpPerspective(xfrmMat, width, height, [255, 255, 255]);
//   im.save("./warp-image.png");
//   console.log('Image saved to ./tmp/warp-image.png');
// });
