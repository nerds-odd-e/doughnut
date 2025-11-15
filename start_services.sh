#!/usr/bin/env bash
export JAVA_HOME=/tmp/java24/zulu24.32.13-ca-jdk24.0.2-linux_x64
export PATH=$JAVA_HOME/bin:$PATH
cd /workspace
pnpm sut
