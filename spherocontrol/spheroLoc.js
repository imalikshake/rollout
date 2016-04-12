'use strict';
var sylvester = require('sylvester');

var Matrix = sylvester.Matrix;


var filters = require('./filters.js');
var KalmanModel = filters.KalmanModel;
var KalmanObservation = filters.KalmanObservation;
var vec2log = filters.vec2log;
var Filter = filters.Filter;

var spheroIds = [
    'boo',
    'ybr',
];

var spheros = {};

function nothing() {
    // console.log('nothing b0ss');
}

var fs = require('fs');
var settings;

process.on('SIGINT', function() {
    console.log('Goodbye!');
});

if (process.argv[2]) {
    var settingsFile = process.argv[2];
    console.log(settingsFile);
    fs.readFile(settingsFile, (err, file) => {
        if (err) {
            console.log('Couldnt open settings file', settingsFile, err);
            return;
        }
        settings = JSON.parse(file.toString());
    });
}

function initSphero(dataOut) {
    // initial state
    var x_0 = $V([0, 0, 0, 0]);

    // initial prediction error (always 1)
    var P_0 = $M([
        [1, 0, 0, 0],
        [0, 1, 0, 0],
        [0, 0, 1, 0],
        [0, 0, 0, 1],
    ]);

    // the input model
    // to add the velocity to position
    var F_k = $M([
        [1, 0, 1 / 200, 0],
        [0, 1, 0, 1 / 200],
        [0, 0, 1, 0],
        [0, 0, 0, 1],
    ]);


    // process noise (wadafaq)
    var Q_k = $M([
        [0.001, 0, 0, 0],
        [0, 0.001, 0, 0],
        [0, 0, 0.001, 0],
        [0, 0, 0, 0.001],
    ]);
    var KM = new KalmanModel(x_0, P_0, F_k, Q_k);


    // observation value
    var z_k = $V([0, 0, 0, 0]);

    // observation model
    var H_k = $M([
        [1, 0, 0, 0],
        [0, 1, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
    ]);

    // noise
    var R_k = $M([
        [1, 0, 0, 0],
        [0, 1, 0, 0],
        [0, 0, 0.1, 0],
        [0, 0, 0, 0.1],
    ]);

    var Kimage = new KalmanObservation(z_k, H_k, R_k);

    H_k = $M([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 1, 0],
        [0, 0, 0, 1],
    ]);

    var Ksphero = new KalmanObservation(z_k, H_k, R_k);

    var ret = {
        ipPos: vec2log(4),
        spheroVel: vec2log(10),
        batteryVoltage: -1,
        force: nothing,
        driftAngle: 0,
        absSpheroVel: {
            x: 0,
            y: 0
        },
        pos: {
            x: 0,
            y: 0
        },
        dx: 0,
        dy: 0,
        kalmanModel: KM,
        kSpheroObservation: Ksphero,
        kIPObservation: Kimage,
        angleLog: Filter(500)
    }
    setInterval(() => {
        KM.update(Ksphero);
        KM.update(Kimage);
        ret.pos = {
            x: KM.x_k.elements[0],
            y: KM.x_k.elements[1],
        }
        dataOut(ret.pos);
    }, 1000 / 60);

    return ret;
}


var transformCorners;
// var cv = require("opencv");

var imageSize = {
    x: 1280.0,
    y: 720.0,
};

var spheroScale = 22;
var outputScale = 20;

var debugLog = console.log;
var offset = {
    x: 0,
    y: 0
};

module.exports = function(fn) {
    var dataOut = fn.dataOut;


    spheros = {
        "ybr": initSphero(pos => dataOut({
            name: 'ybr',
            data: {
                kalmanPos: pos
            }
        })),
        "boo": initSphero(pos => dataOut({
            name: 'boo',
            data: {
                kalmanPos: pos
            }
        })),
    };

    // called by web client to test forces
    fn.forceCallback(function(data) {
        spheros.ybr.force(data.direction, data.force);
        spheros.boo.force(data.direction, data.force);
    });

    fn.transformCallback(function(data) {
        offset = data.points[0];
    });

    fn.spheroScaleCallback(function(data) {
        console.log('spheroScale', data);
        if (data.spheroScale)
            spheroScale = data.spheroScale;
        if (data.outputScale)
            outputScale = data.outputScale;
    });

    setupSpheroManager(dataOut);
    setupIp(dataOut);

    return spheros;
}

function setupSpheroManager(dataOut) {
    var ipc = require('node-ipc');

    ipc.config.id = 'hello';
    ipc.config.silent = true;

    var id = 'world';

    ipc.connectTo(id, _ => {
        ipc.of[id].on('connect', _ => {
            ipc.log('Connected to sphero manager'.rainbow);
            ipc.of[id].emit('init', '');
        });

        ipc.of[id].on('disconnect', () => {
            ipc.log('disconnected from'.notice);
        });

        ipc.of[id].on('newSpheroData', (data) => {
            var val = JSON.parse(data);
            var name = deviceNameTofriendly(val.name);
            if (val.data.type === 'velocity') {
                dataOut({
                    name,
                    data: newSpheroData(val.data, spheros[name])
                });
            } else {
                ipc.log('Unknown data type'.debug);
            }
        });

        ipc.of[id].on('spheroList', (data) => {
            // console.log('got sphero list', JSON.parse(data));
            var val = JSON.parse(data);
            for (let deviceName of val) {
                var name = deviceNameTofriendly(deviceName);
                var sphero = spheros[name];
                sphero.force = (direction, force) => {
                    let offsetDirection = direction - sphero.driftAngle;
                    // console.log(offsetDirection)
                    let send = {
                        name: deviceName,
                        direction: offsetDirection,
                        force
                    };
                    ipc.of[id].emit('force', JSON.stringify(send));
                };
            }
        });
    });
}

function deviceNameTofriendly(name) {
    // ew
    return name.toLowerCase().indexOf("ybr") !== -1 ? "ybr" : "boo";
}


function setupIp(dataOut) {
    var PORT = 1337;
    var HOST = '127.0.0.1';

    var dgram = require('dgram');
    var server = dgram.createSocket('udp4');

    server.on('listening', function() {
        var address = server.address();
        debugLog('Listening for image processing data on', address.address + ":" + address.port);
    });
    var i = 0;
    server.on('message', function(message, remote) {
        var data = {};
        var parts = message.toString().split(',');
        data.id = parts[0];
        // absolute position update
        data.x = parseInt(parts[1]);
        data.y = parseInt(parts[2]);

        var sphName = spheroIds[data.id];
        var spheroState = spheros[sphName];
        if (spheroState) {
            dataOut({
                name: sphName,
                data: newIpData(spheroState, data)
            });
        }
    });

    server.bind(PORT);
}


var lastPos = [{
    x: 0,
    y: 0
}, {
    x: 0,
    y: 0
}];

var bef = new Date().getTime();


function newSpheroData(data, spheroState) {
    var dx = data.dx;
    var dy = data.dy;

    spheroState.spheroVel.add({
        x: dx / 1000.0,
        y: dy / 1000.0,
    });

    var absVelX = dx * Math.cos(-spheroState.driftAngle) -
        dy * Math.sin(-spheroState.driftAngle);
    var absVelY = dx * Math.sin(-spheroState.driftAngle) +
        dy * Math.cos(-spheroState.driftAngle);

    spheroState.absSpheroVel = {
        x: absVelX,
        y: absVelY,
    };

    spheroState.kSpheroObservation.z_k = $V(
        [0, 0,
            absVelX * spheroScale, // to meters
            absVelY * spheroScale
        ]
    );
    return {
        relSpheroVec: spheroState.spheroVel.average(),
        absSpheroVec: spheroState.absSpheroVel,
    }
}

var ipScale = 40;

function newIpData(sphero, data) {
    // data.x, data.y, data.id
    var angle, dx, dy;

    // lost sphero
    if (data.x === -1 || data.y === -1) {
        return {};
    }

    var ipData = {
        x: data.x,
        y: data.y,
    }

    var transformedPosition = pixelToPosition(ipData);
    // console.log(transformedPosition);

    sphero.kIPObservation.z_k = $V([transformedPosition.x, transformedPosition.y, 0, 0]);

    sphero.ipPos.add(transformedPosition);

    var filteredPos = sphero.ipPos.average();

    var ipDx = filteredPos.x - lastPos[data.id].x;
    var ipDy = filteredPos.y - lastPos[data.id].y;
    lastPos[data.id] = filteredPos;

    var ipDirVec = {
        x: ipDx,
        y: ipDy
    };

    var avgSpheroData = sphero.spheroVel.average();

    var spheroDirVec = {
        x: avgSpheroData.x,
        y: avgSpheroData.y
    };

    var ipDirMag = Math.sqrt(
        ipDirVec.x * ipDirVec.x +
        ipDirVec.y * ipDirVec.y
    );
    var spheroDirMag = Math.sqrt(
        spheroDirVec.x * spheroDirVec.x +
        spheroDirVec.y * spheroDirVec.y
    );

    var ipDirAngle = Math.atan2(ipDirVec.y, ipDirVec.x);
    var spheroDirAngle = Math.atan2(spheroDirVec.y, spheroDirVec.x);
    angle = spheroDirAngle - ipDirAngle;

    angle = angle > Math.PI ? angle - 2 * Math.PI : angle;
    angle = angle < -Math.PI ? angle + 2 * Math.PI : angle;

    if (!isNaN(angle) &&
        spheroDirMag > 0.05) {
        console.log('spheroDirMag', spheroDirMag);
        sphero.angleLog.add(angle);
        var filteredAngle = sphero.angleLog.value();

        sphero.driftAngle = filteredAngle;
    }

    var bottomLeft = pixelToPosition({
            x: 0,
            y: 0
        }),
        topRight = pixelToPosition({
            x: 1280,
            y: 720
        }),
        topLeft = pixelToPosition({
            x: 0,
            y: 720
        }),
        bottomRight = pixelToPosition({
            x: 1280,
            y: 0
        });

    return {
        // kalmanPos: pos,
        driftAngle: sphero.driftAngle ? sphero.driftAngle : 0.0,
        ipAngle: ipDirAngle,
        ipPosTransformed: transformedPosition,
        spheroAngle: spheroDirAngle,
        cameraBounds: {
            bottomLeft,
            topRight,
            topLeft,
            bottomRight
        }
    }
}

var angleOffset = 0 / 360 * Math.PI * 2; // camera angle offset
var h = 2; // meters high
var verticalFov = 50 / 360 * Math.PI * 2; // 60 degrees in radiuns
var radiunsPerYPixel = verticalFov / imageSize.y;

var focalLength = 200; // 10 mm?

var resetAngle = angleOffset + verticalFov / 2;

var rotatu = $M([
    [1, 0, 0],
    [0, Math.cos(resetAngle), -Math.sin(resetAngle)],
    [0, Math.sin(resetAngle), Math.cos(resetAngle)]
]);



function pixelToPosition(pos) {
    // make bottom left (0,0)
    var y = imageSize.y - pos.y;

    var x = pos.x - imageSize.x / 2;
    // console.log(1 / Math.cos(radiunsPerYPixel * y))
    var Z = h / Math.cos(angleOffset + radiunsPerYPixel * y);
    var X = x / (focalLength / Z);
    var Y = y / (focalLength / Z);
    var res = rotatu.x($V([X, Y, Z]));
    // return {
    //     x: pos.x / (imageSize.x * 0.5) - 1.0,
    //     y: (imageSize.y - pos.y) / (imageSize.y * 0.5) - 1.0,
    // }
    return {
        x: res.elements[0] + offset.x,
        y: res.elements[1] + offset.y,
        z: res.elements[2]
    }
}
