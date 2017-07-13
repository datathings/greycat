#! /usr/bin/env node
var shell = require("shelljs");
var userArgs = process.argv.slice(2);
var javaArgs = [];
var programArgs = [];
userArgs.forEach(function (arg) {
    if (arg.startsWith('-D') || arg.startsWith('-X')) {
        javaArgs.push(arg);
    } else {
        programArgs.push(arg);
    }
});

var fs = require('fs');
var json = JSON.parse(fs.readFileSync(__dirname + '/package.json', 'utf8'));
javaArgs.push('-Dgreycat.version=' + json.version);

var cmd = 'java';
javaArgs.forEach(function (arg) {
    cmd += ' "' + arg + '"';
});
cmd += ' -jar "' + __dirname + '/generator.jar" ';
programArgs.forEach(function (arg) {
    cmd += ' "' + arg + '"';
});
if (shell.exec(cmd).code !== 0) {
    shell.echo('Error: Generation failed');
    shell.exit(1);
}
