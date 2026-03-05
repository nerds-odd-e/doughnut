#!/usr/bin/env node

// src/version.ts
var VERSION = true ? "0.2.0" : "0.1.0";
function getVersion() {
  return VERSION;
}
function formatVersionOutput() {
  return `doughnut ${getVersion()}`;
}
function parseVersionFromOutput(output) {
  const match = output.match(/doughnut\s+(\d+\.\d+\.\d+)/);
  return match ? match[1] : null;
}
function compareVersions(a, b) {
  const [aMajor, aMinor, aPatch] = a.split(".").map(Number);
  const [bMajor, bMinor, bPatch] = b.split(".").map(Number);
  if (aMajor !== bMajor) return aMajor - bMajor;
  if (aMinor !== bMinor) return aMinor - bMinor;
  return aPatch - bPatch;
}

// src/update.ts
import { chmodSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
var BASE_URL = process.env.BASE_URL ?? "https://doughnut.odd-e.com";
var DOWNLOAD_PATH = `${BASE_URL}/doughnut-cli-latest/doughnut`;
async function runUpdate() {
  const currentVersion = getVersion();
  const currentPath = process.argv[1];
  if (!currentPath) {
    console.error("Could not determine executable path");
    process.exit(1);
  }
  const tempFile = join(tmpdir(), `doughnut-update-${Date.now()}`);
  let response;
  try {
    response = await fetch(DOWNLOAD_PATH);
  } catch (e) {
    console.error(`Failed to download: ${e instanceof Error ? e.message : e}`);
    process.exit(1);
  }
  if (!response.ok) {
    console.error(`Download failed: HTTP ${response.status}`);
    process.exit(1);
  }
  const buffer = await response.arrayBuffer();
  writeFileSync(tempFile, Buffer.from(buffer));
  chmodSync(tempFile, 493);
  const result = spawnSync(tempFile, ["version"], { encoding: "utf8" });
  if (result.error || result.status !== 0) {
    rmSync(tempFile, { force: true });
    console.error("Downloaded binary is invalid or failed to run");
    process.exit(1);
  }
  const incomingVersion = parseVersionFromOutput(result.stdout);
  if (!incomingVersion) {
    rmSync(tempFile, { force: true });
    console.error("Could not determine version of downloaded binary");
    process.exit(1);
  }
  if (compareVersions(incomingVersion, currentVersion) <= 0) {
    rmSync(tempFile, { force: true });
    console.log(`${formatVersionOutput()} is already the latest version`);
    return;
  }
  try {
    renameSync(tempFile, currentPath);
    chmodSync(currentPath, 493);
  } catch (e) {
    rmSync(tempFile, { force: true });
    console.error(`Failed to replace binary: ${e instanceof Error ? e.message : e}`);
    process.exit(1);
  }
  console.log(`Updated doughnut from ${currentVersion} to ${incomingVersion}`);
}

// src/index.ts
async function main() {
  const args = process.argv.slice(2);
  const hasVersionFlag = args.includes("--version") || args.includes("-v");
  const subcommand = args.find((a) => !a.startsWith("-"));
  if (hasVersionFlag || subcommand === "version") {
    console.log(formatVersionOutput());
    return;
  }
  if (subcommand === "update") {
    await runUpdate();
    return;
  }
  console.log(formatVersionOutput());
}
main();
