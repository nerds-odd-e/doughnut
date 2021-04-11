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
  constructor(buildName, status, gitLog) {
    this.buildName = buildName;
    this.status = status;
    this.gitLog = gitLog;
  }

  nextState() {
    return new Promise((resolve, reject) => {
      for_build_status("https://github.com/nerds-odd-e/doughnut/actions", (currentBuild, currentStatus, gitLog)=>{
          var toSay = "";
          var nextStatus = this.status;
          var newBuild = false;
          if (this.buildName !== currentBuild) {
            newBuild = true;
              nextStatus = "";
              toSay = "A new push: " + gitLog;
          }
          if (nextStatus !== currentStatus) {
              if (! newBuild) {
                toSay = "The build ";
              }
              toSay += currentStatus;
          }
          resolve(new BuildState(currentBuild, currentStatus, gitLog));
      });
    })
  }

  diffToSentence(previousState, dictionary) {
    var toSay = "";
    if (this.buildName != previousState.buildName) {
      toSay = dictionary.translate("new_build") + `"${this.gitLog}"` + dictionary.translate(this.status);
    }
    return toSay;
  }
}

var buildState = new BuildState("", "");
const englishDictionary = {
  translate: function(phrase) {
    return {
      new_build: "A new build "
    }[phrase] || ` ${phrase}`;
  }
};

setInterval(()=>{ buildState.nextState().then((newState) => {
   say(newState.diffToSentence(buildState, englishDictionary));
   buildState = newState;
  } ) }, 5000);

module.exports = {
  BuildState, say, englishDictionary
};
