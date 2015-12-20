'use strict';

let WebSocket = require('ws');
let solve = require('./solver');

function handleConnection(ws, id, count) {
    let state = {};
    let path = null;
    let solver = null;
    let expectClose = false;

    currentWorkers += 1;

    ws.on('open', function() {
        // console.log(id + ' connected');
    });

    ws.on('error', function(err) {
        console.log(id + ' WS error: ' + err);
    });

    ws.on('close', function() {
        if (!expectClose) {
            console.log(id + ' unexpectedly disconnected');
        }
        currentWorkers -= 1;
    });

    ws.on('message', function(str) {
        let data = JSON.parse(str);
        // console.log(id + ' got', data);

        if ('maze' in data) {
            state = data;
            startSolving();
        } else if ('position' in data) {
            // console.log(id + ' jumped to ' + data.position);
            state.position = data.position;
            startSolving();
        } else if ('result' in data) {
            console.log(id + ' finished with: ' + data.result);
            stopSolving();
            expectClose = true;
            ws.close();

            if (count > 0) {
                startWorker(id, count - 1);
            }
        }
    });

    function move() {
        if (path.length > 0) {
            let nextMove = path.pop();
            ws.send(JSON.stringify({move: nextMove}));
            solver = setTimeout(move, 100 + Math.floor(Math.random() * 500));
        }
    }

    function stopSolving() {
        if (solver) {
            clearTimeout(solver);
            solver = null;
        }
    }

    function startSolving() {
        stopSolving();
        path = solve(state.maze, state.target, state.position);
        // console.log('start solving path');
        solver = setTimeout(move, 1500);
    }
}

function startWorker(id, counter) {
    console.log('Starting worker ' + id);
    let ws = new WebSocket(url);
    handleConnection(ws, id, counter);
}

let url = process.argv[2] || "ws://localhost:3000/ws";
let numWorkers = process.argv[3] || 4;
let numGames = process.argv[4] || 1;
let currentWorkers = 0;

console.log('Starting ' + numWorkers + ' workers against ' + url + ' with ' + numGames + ' games each');

for (let i = 0; i < numWorkers; i++) {
    setTimeout(function(i) {
        startWorker("worker-" + i, numGames);
    }, i * 250, i)
}

setInterval(function() {
    console.log('Workers running: ' + currentWorkers + " / " + numWorkers);
}, 1000);