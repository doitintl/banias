#!/bin/bash
#Vegeta from here #Vegeta from here https://github.com/tsenart/vegeta
vegeta -cpus 4 attack -targets=targets.txt -duration=1m -rate 10000  tee /tmp/results.bin | vegeta report
