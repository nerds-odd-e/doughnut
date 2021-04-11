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

function buildState(url) {
  return new Promise((resolve, reject) => {
    request(url, function(err, res, body) {
      if (err) {
        console.error(err)
      }
      else {
        const currentBuild = body.match(/check_suite_\d+/)?.shift();
        const currentStatus = body.match(/This workflow run ([^\.]+\.)/)?.pop();
        const gitLog = body.match(/aria\-label\=\"Run \d+ of[^\>]+\>(.*)\<\/a\>/)?.pop();
        resolve(new BuildState(currentBuild, currentStatus, gitLog));
      }
    });
  });
}

class BuildState {
  constructor(buildName, status, gitLog) {
    this.buildName = buildName;
    this.status = status;
    this.gitLog = gitLog;
  }

  diffToSentence(previousState, dictionary) {
    if (this.buildName != previousState.buildName) {
      return dictionary.translate("new_build") + `"${this.gitLog}"` + dictionary.translate(this.status);
    }
    if (this.status != previousState.status) {
      return dictionary.translate("the_build") + dictionary.translate(this.status);
    }
    return "";
  }
}

var lastBuildState = new BuildState("", "");

const englishDictionary = {
  translate: function(phrase) {
    return {
      new_build: "A new build ",
      the_build: "The build",
    }[phrase] || ` ${phrase}`;
  }
};

const japaneseDictionary = {
  translate: function(phrase) {
    return {
      new_build: "新規プッシュがありました：",
      the_build: "現プッシュが",
      "has been queued.": "準備中。",
      "is currently running.": "運転中。",
      "completed successfully.": "成功しました！",
      "faild.": "失敗しました！直してください",
    }[phrase] || ` ${phrase}`;
  }
};

setInterval(()=>{ buildState("https://github.com/nerds-odd-e/doughnut/actions").then((newState) => {
   say(newState.diffToSentence(lastBuildState, japaneseDictionary));
   lastBuildState = newState;
  } ) }, 5000);

module.exports = {
  buildState, BuildState, say, englishDictionary
};
