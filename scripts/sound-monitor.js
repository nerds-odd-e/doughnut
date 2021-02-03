const { exec } = require('child_process');
const request = require('request');

function say(sentence) {
    exec("say \""+sentence+"\"", (err, stdout, stderr) => {
      if (err) {
        console.error(err)
      }
    });
}

function for_build_status(url, action) {
    request(url, function(err, res, body) {
      if (err) {
        console.error(err)
      }
      else {
        action(body.match(/check_suite_\d+/).shift(), body.match(/This workflow run[^\.]+\./).shift());
      }
    });
}

var lastStatus = [""];
var lastBuild = [""];
function soundMonitor() {
    for_build_status("https://github.com/nerds-odd-e/doughnut/actions?query=workflow%3A%22Run+e2e+test+suite+on+every+push+to+any+branch%22", (currentBuild, currentStatus)=>{
        console.error(currentStatus);
        if (lastBuild[0] !== currentBuild) {
            say("Starting a new build.");
            lastBuild[0] = currentBuild;
            lastStatus[0] = "";
        }
        if (lastStatus[0] !== currentStatus) {
            say(currentStatus);
            lastStatus[0] = currentStatus;
        }
    });
}

setInterval(soundMonitor, 5000);
