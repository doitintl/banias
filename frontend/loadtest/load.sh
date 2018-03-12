#!/bin/bash
SLAVE_INSTANCES=$(gcloud compute instances list | grep vegeta- | awk '{print $1}')
SLAVE_INSTANCES_IPS=$(gcloud compute instances list | grep vegeta- | awk '{print $5}')
SSH_ARGS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"


SLAVE_INSTANCES_STR=$(echo "$SLAVE_INSTANCES" | paste -sd "," -)
echo Run on $SLAVE_INSTANCES_STR

IFS=';' read -r -a MACHINES <<< "$SLAVE_INSTANCES_STR"



SSH_CMD="ulimit -n"
IFS=';' read -r -a MACHINES <<< "$SLAVE_INSTANCES_STR"
for m in "${MACHINES[@]}"
do
    echo fetch from ${m}
    pdsh -R ssh -l aviv -b -w  ${SLAVE_INSTANCES_IPS} ${SSH_CMD}
    #scp -q ${SSH_ARGS} ${m}:~/result.bin ${m}.bin &
done






net.ipv4.tcp_fin_timeout = 30
net.ipv4.ip_local_port_range= 15000 65535
net.ipv4.ip_forward = 0
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.all.send_redirects = 0
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_tw_recycle













#ulimit -n 64000
#vegeta -cpus 4 attack -targets=targets.txt -duration=1m -rate 500   >/tmp/load.bin
#cat /tmp/load.bin | vegeta report -reporter=plot > /tmp/plot.html
#vegeta report -inputs=/tmp/load.bin
#open /tmp/plot.html


#tc qdisc add dev eth0 root netem delay 50ms
#tc qdisc add dev eth0 root netem loss 10%
#tc qdisc change dev eth0 root netem corrupt 5%
#tc qdisc change dev eth0 root netem duplicate 1%
#tc qdisc add dev eth0 root tbf rate 1mbit burst 32kbit latency 400ms
#tc qdisc del dev eth0 root

#3g
#bandwidth=100kbps
#latency=350ms

#tc class $verb dev $device parent 1:1 classid 1:12 htb rate $bandwidth ceil $bandwidth  &&
#tc qdisc $verb dev $device parent 1:12 netem delay $latency