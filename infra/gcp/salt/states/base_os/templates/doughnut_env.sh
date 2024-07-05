#!/bin/bash

export PROJECTID="{{ salt['pillar.get']('doughnut_app:project_id') }}"
export BUCKET="{{ salt['pillar.get']('doughnut_app:bucket') }}"
export JAVA_HOME="{{ salt['pillar.get']('doughnut_app:java_home') }}"
export PATH=$PATH:$JAVA_HOME/bin
