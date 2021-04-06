const { exec } = require('child_process');
const request = require('request');

function say(sentence) {
  if(sentence === "") {
    return;
  }
  console.error(sentence);
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
            body.match(/check_suite_\d+/)?.shift(),
            body.match(/This workflow run ([^\.]+\.)/)?.pop(),
            body.match(/aria\-label\=\"Run \d+ of[^\>]+\>(.*)\<\/a\>/)?.pop()
        );
      }
    });
}

class BuildState {
  constructor(buildName, status) {
    this.buildName = buildName;
    this.status = status;
  }

  nextState() {
    return new Promise((resolve, reject) => {
      for_build_status("https://github.com/nerds-odd-e/doughnut/actions", (currentBuild, currentStatus, gitLog)=>{
          var toSay = "";
          var nextBuildName = this.buildName;
          var nextStatus = this.status;
          if (this.buildName !== currentBuild) {
              nextBuildName = currentBuild;
              nextStatus = "";
              toSay = "A new push: " + gitLog;
          }
          if (nextStatus !== currentStatus) {
              nextStatus = currentStatus;
              if (toSay === "") {
                toSay = "The build ";
              }
              toSay += currentStatus;
          }
          resolve({toSay, newState: new BuildState(nextBuildName, nextStatus)});
      });
    })
  }
}

var buildState = new BuildState("", "");

setInterval(()=>{ buildState.nextState().then(({toSay, newState}) => {
   say(toSay);
    buildState = newState;
  } ) }, 5000);

module.exports = {
  BuildState, say
};
