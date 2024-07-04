#!/bin/bash

export PROJECTID="{{ salt['pillar.get']('doughnut_app:projectid', 'carbon-syntax-298809') }}"
export BUCKET="{{ salt['pillar.get']('doughnut_app:bucket', 'dough-01') }}"
export JAVA_HOME="{{ salt['pillar.get']('doughnut_app:java_home', '/usr/lib/jvm/zre-22-amd64') }}"
export PATH=$PATH:$JAVA_HOME/bin
