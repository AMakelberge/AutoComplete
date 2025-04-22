#!/bin/bash
if [ ! -f AutoComplete.jar ]; then
  kotlinc AutoComplete.kt -include-runtime -d AutoComplete.jar
fi
kotlin AutoComplete.jar "$@"