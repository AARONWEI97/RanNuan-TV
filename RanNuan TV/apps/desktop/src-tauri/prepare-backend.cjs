const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const serverDir = path.resolve(__dirname, '../../../server');
const packageFile = path.join(serverDir, 'package.json');
const modulesDir = path.join(serverDir, 'node_modules');
const runtimeDir = path.join(serverDir, 'node-runtime');
const runtimeName = process.platform === 'win32' ? 'node.exe' : 'node';
const runtimeTarget = path.join(runtimeDir, runtimeName);

function fail(message) {
  console.error(`[Desktop Backend] ${message}`);
  process.exit(1);
}

if (!fs.existsSync(packageFile)) {
  fail(`Missing server package.json: ${packageFile}`);
}

const nodeMajor = Number(process.versions.node.split('.')[0]);
if (!Number.isFinite(nodeMajor) || nodeMajor < 18) {
  fail(`Node.js 18 or newer is required to build (current: ${process.version})`);
}

const packageJson = JSON.parse(fs.readFileSync(packageFile, 'utf8'));
const dependencies = Object.keys(packageJson.dependencies || {});
const needsCleanInstall = dependencies.some((name) => {
  const dependencyPath = path.join(modulesDir, name);
  if (!fs.existsSync(dependencyPath)) return true;
  return fs.lstatSync(dependencyPath).isSymbolicLink();
});

if (needsCleanInstall) {
  console.log('[Desktop Backend] Installing real production dependencies with npm ci...');
  const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
  const install = spawnSync(
    npmCommand,
    ['ci', '--omit=dev', '--ignore-scripts', '--workspaces=false'],
    {
      cwd: serverDir,
      stdio: 'inherit',
      shell: process.platform === 'win32',
    }
  );
  if (install.status !== 0) {
    fail(`npm ci failed with exit code ${install.status ?? 'unknown'}`);
  }
}

for (const name of dependencies) {
  const dependencyPath = path.join(modulesDir, name);
  if (!fs.existsSync(dependencyPath) || fs.lstatSync(dependencyPath).isSymbolicLink()) {
    fail(`Dependency is not packageable: ${name}`);
  }
}

fs.mkdirSync(runtimeDir, { recursive: true });
fs.copyFileSync(process.execPath, runtimeTarget);
if (process.platform !== 'win32') fs.chmodSync(runtimeTarget, 0o755);

const requireCheck = spawnSync(
  runtimeTarget,
  ['-e', `for (const name of ${JSON.stringify(dependencies)}) require.resolve(name)`],
  { cwd: serverDir, stdio: 'inherit' }
);
if (requireCheck.status !== 0) {
  fail('Bundled Node runtime could not resolve server dependencies');
}

console.log(`[Desktop Backend] Runtime ready: ${runtimeTarget}`);
console.log(`[Desktop Backend] Production dependencies ready: ${dependencies.length}`);
