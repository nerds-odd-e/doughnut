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
        action(
            body.match(/check_suite_\d+/).shift(),
            body.match(/This workflow run ([^\.]+\.)/).pop(),
            body.match(/aria\-label\=\"Run \d+ of[^\>]+\>(.*)\<\/a\>/).pop()
            );
      }
    });
}

var lastStatus = [""];
var lastBuild = [""];
function soundMonitor() {
    for_build_status("https://github.com/nerds-odd-e/doughnut/actions", (currentBuild, currentStatus, gitLog)=>{
        console.error(gitLog + " ... " + currentStatus);
        var toSay = "The build is ";
        if (lastBuild[0] !== currentBuild) {
            lastBuild[0] = currentBuild;
            lastStatus[0] = "";
            toSay = "A new push: " + gitLog;
        }
        if (lastStatus[0] !== currentStatus) {
            lastStatus[0] = currentStatus;
            toSay += currentStatus;
            say(toSay);
        }
    });
}

setInterval(soundMonitor, 5000);
