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
  if (image == 'ubuntu20') {
    if (mysqlVersion != '8.0') {
      // install
      run(`sudo apt-get install mysql-server-${mysqlVersion}`);
    }
  } else {
    if (mysqlVersion != '5.7') {
      if (mysqlVersion == '5.6') {
        throw `MySQL version not supported yet: ${mysqlVersion}`;
      }

      // install
      useTmpDir();
      run(`wget -q -O mysql-apt-config.deb https://dev.mysql.com/get/mysql-apt-config_0.8.16-1_all.deb`);
      run(`echo mysql-apt-config mysql-apt-config/select-server select mysql-${mysqlVersion} | sudo debconf-set-selections`);
      run(`sudo dpkg -i mysql-apt-config.deb`);
      // TODO only update single list
      run(`sudo apt-get update`);
      run(`sudo apt-get install mysql-server`);
    }
  }

  run(`sudo echo "port		= ${port}" >> /etc/mysql/mysql.conf.d/mysqld.cnf`)`
  // start
  run(`sudo systemctl start mysql`);

  // remove root password
  run(`sudo mysqladmin -proot password ''`);

  // add user
  run(`sudo mysql -e "CREATE USER '$USER'@'localhost' IDENTIFIED BY ''"`);
  run(`sudo mysql -e "GRANT ALL PRIVILEGES ON *.* TO '$USER'@'localhost'"`);
  run(`sudo mysql -e "FLUSH PRIVILEGES"`);

  bin = `/usr/bin`;
}

if (database) {
  runSafe(path.join(bin, 'mysqladmin'), 'create', database);
}
