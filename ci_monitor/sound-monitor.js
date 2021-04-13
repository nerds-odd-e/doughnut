const { exec } = require('child_process');
const request = require('request');

const Reset = "\x1b[0m"
const Bright = "\x1b[1m"
const Dim = "\x1b[2m"
const Underscore = "\x1b[4m"
const Blink = "\x1b[5m"
const Reverse = "\x1b[7m"
const Hidden = "\x1b[8m"

const FgBlack = "\x1b[30m"
const FgRed = "\x1b[31m"
const FgGreen = "\x1b[32m"
const FgYellow = "\x1b[33m"
const FgBlue = "\x1b[34m"
const FgMagenta = "\x1b[35m"
const FgCyan = "\x1b[36m"
const FgWhite = "\x1b[37m"

const BgBlack = "\x1b[40m"
const BgRed = "\x1b[41m"
const BgGreen = "\x1b[42m"
const BgYellow = "\x1b[43m"
const BgBlue = "\x1b[44m"
const BgMagenta = "\x1b[45m"
const BgCyan = "\x1b[46m"
const BgWhite = "\x1b[47m"

function now() {
  var currentdate = new Date();
  return currentdate.getDate() + "/"
    + (currentdate.getMonth()+1)  + "/"
    + currentdate.getFullYear() + "@"
    + currentdate.getHours() + ":"
    + currentdate.getMinutes() + ":"
    + currentdate.getSeconds();
}

function say(sentence, colorCode) {
  if(sentence === "") {
    return;
  }
  console.error(colorCode + now() + ": " + sentence + Reset);
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
      return dictionary.translate("new_build") + `'${this.gitLog}'` + dictionary.translate(this.status);
    }
    if (this.status != previousState.status) {
      return dictionary.translate("the_build") + dictionary.translate(this.status);
    }
    return "";
  }

  colorCode() {
    return {
      "has been queued.": BgBlue + FgYellow + Blink,
      "is currently running.": BgBlue + FgYellow + Blink,
      "completed successfully.": BgGreen + FgBlack,
      "faild.": BgRed + FgYellow + Blink
    }[this.status];
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
   say(newState.diffToSentence(lastBuildState, japaneseDictionary), newState.colorCode());
   lastBuildState = newState;
  } ) }, 5000);

module.exports = {
  buildState, BuildState, say, englishDictionary
};
