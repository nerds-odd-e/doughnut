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

class BuildState {
  constructor(buildName, status) {
    this.buildName = buildName;
    this.status = status;
  }
}
var buildState = [new BuildState("", "")];
function soundMonitor(sayCallBack) {
    for_build_status("https://github.com/nerds-odd-e/doughnut/actions", (currentBuild, currentStatus, gitLog)=>{
        console.error(gitLog + " ... " + currentStatus);
        var toSay = "The build ";
        var lastBuild = buildState[0].buildName;
        var lastStatus = buildState[0].status;
        if (buildState[0].buildName !== currentBuild) {
            lastBuild = currentBuild;
            lastStatus = "";
            toSay = "A new push: " + gitLog;
        }
        if (lastStatus !== currentStatus) {
            lastStatus = currentStatus;
            toSay += currentStatus;
            sayCallBack(toSay);
        }
        buildState[0] = new BuildState(lastBuild, lastStatus);
    });
}

setInterval(()=>soundMonitor(say), 5000);

module.exports = {
  soundMonitor, say
};
