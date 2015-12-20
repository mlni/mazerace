'use strict';

function findPath(maze, start, position) {
    let UP = 1, RIGHT = 2, DOWN = 4, LEFT = 8;

    function str(x, y) {
        return "" + x + "," + y;
    }
    function hasWall(x, y, dir) {
        return maze[y][x] & dir;
    }
    function unvisitedExits(x, y, pathSoFar) {
        let deltas = [[-1, 0, LEFT], [0, -1, UP], [1, 0, RIGHT], [0, 1, DOWN]];
        let result = [];
        for (let i in deltas) {
            let dx = deltas[i][0];
            let dy = deltas[i][1];
            let dir = deltas[i][2];

            let xx = x + dx, yy = y + dy;

            if ((xx >= 0) && (yy >= 0) && (xx < maze[0].length) && (yy < maze.length)
                && !hasWall(x, y, dir) && visited.indexOf(str(xx, yy)) == -1) {
                result.push({x: xx, y: yy, path: pathSoFar.concat([[xx, yy]])});
                visited.push(str(xx, yy));
            }
        }
        return result;
    }

    let target_x = position[0];
    let target_y = position[1];
    let walkers = [{x: start[0], y: start[1], path: [start]}];
    let visited = [str(start)];
    while(walkers.length > 0) {
        let newWalkers = [];
        for (let i in walkers) {
            let walker = walkers[i];
            if ((walker.x == target_x) && (walker.y == target_y)) {
                return walker.path;
            } else {
                let unvisited = unvisitedExits(walker.x, walker.y, walker.path);
                newWalkers = newWalkers.concat(unvisited);
            }
        }
        walkers = newWalkers;
    }
}

module.exports = findPath;