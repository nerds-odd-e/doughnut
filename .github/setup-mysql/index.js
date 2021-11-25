const execSync = require("child_process").execSync;
const fs = require('fs');
const os = require('os');
const path = require('path');
const process = require('process');
const spawnSync = require('child_process').spawnSync;

function run(command) {
  console.log(command);
  let env = Object.assign({}, process.env);
  delete env.CI; // for Homebrew on macos-11.0
  execSync(command, {stdio: 'inherit', env: env});
}

function runSafe() {
  const args = Array.from(arguments);
  console.log(args.join(' '));
  const command = args.shift();
  // spawn is safer and more lightweight than exec
  const ret = spawnSync(command, args, {stdio: 'inherit'});
  if (ret.status !== 0) {
    throw ret.error;
  }
}

function addToPath(newPath) {
  fs.appendFileSync(process.env.GITHUB_PATH, `${newPath}\n`);
}

const image = process.env['ImageOS'];
const defaultVersion = (image == 'ubuntu16' || image == 'ubuntu18') ? '5.7' : '8.0';
const mysqlVersion = parseFloat(process.env['INPUT_MYSQL-VERSION'] || defaultVersion).toFixed(1);

// TODO make OS-specific
if (!['8.0', '5.7', '5.6'].includes(mysqlVersion)) {
  throw `MySQL version not supported: ${mysqlVersion}`;
}

const database = process.env['INPUT_DATABASE'];
const port = process.env['INPUT_PORT'];


let bin;

function useTmpDir() {
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'mysql-'));
  process.chdir(tmpDir);
}

if (process.platform == 'darwin') {
  // install
  run(`brew install mysql@${mysqlVersion}`);

  // start
  bin = `/usr/local/opt/mysql@${mysqlVersion}/bin`;
  const myconf = `/usr/local/etc/my.cnf`;
  run(`sudo echo "port		= ${port}" >> ${myconf}`);
  run(`${bin}/mysql.server start`);

  // add user
  run(`${bin}/mysql -e "CREATE USER '$USER'@'localhost' IDENTIFIED BY ''"`);
  run(`${bin}/mysql -e "GRANT ALL PRIVILEGES ON *.* TO '$USER'@'localhost'"`);
  run(`${bin}/mysql -e "FLUSH PRIVILEGES"`);

  // set path
  addToPath(bin);
} else if (process.platform == 'win32') {
  // install
  const versionMap = {
    '8.0': '8.0.27',
    '5.7': '5.7.36',
    '5.6': '5.6.51'
  };
  const fullVersion = versionMap[mysqlVersion];
  useTmpDir();
  run(`curl -Ls -o mysql.zip https://dev.mysql.com/get/Downloads/MySQL-${mysqlVersion}/mysql-${fullVersion}-winx64.zip`)
  run(`unzip -q mysql.zip`);
  fs.mkdirSync(`C:\\Program Files\\MySQL`);
  fs.renameSync(`mysql-${fullVersion}-winx64`, `C:\\Program Files\\MySQL\\MySQL Server ${mysqlVersion}`);

  // start
  bin = `C:\\Program Files\\MySQL\\MySQL Server ${mysqlVersion}\\bin`;
  if (mysqlVersion != '5.6') {
    run(`"${bin}\\mysqld" --initialize-insecure`);
  }
  run(`"${bin}\\mysqld" --install`);
  run(`net start MySQL`);

  addToPath(bin);

  run(`"${bin}\\mysql" -u root -e "SELECT VERSION()"`);

  // add user
  run(`"${bin}\\mysql" -u root -e "CREATE USER 'ODBC'@'localhost' IDENTIFIED BY ''"`);
  run(`"${bin}\\mysql" -u root -e "GRANT ALL PRIVILEGES ON *.* TO 'ODBC'@'localhost'"`);
  run(`"${bin}\\mysql" -u root -e "FLUSH PRIVILEGES"`);
} else {
  run(`sudo cat /etc/mysql/mysql.conf.d/mysqld.cnf`)
  run(`sudo sed -i 's/3306/3309/g' /etc/mysql/mysql.conf.d/mysqld.cnf`)

  // start
  run(`sudo mysqld &`);

  // remove root password
  run(`sudo mysqladmin -proot password ''`);

  // add user
  run(`sudo mysql  -e "CREATE USER '$USER'@'localhost' IDENTIFIED BY ''"`);
  run(`sudo mysql  -e "GRANT ALL PRIVILEGES ON *.* TO '$USER'@'localhost'"`);
  run(`sudo mysql  -e "FLUSH PRIVILEGES"`);

  bin = `/usr/bin`;
}

if (database) {
  runSafe(path.join(bin, 'mysqladmin'), 'create', database);
}
