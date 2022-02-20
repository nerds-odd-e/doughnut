#!/bin/bash

export expected_healthy_services_count=2
export healthy_services_counter=0
export unhealthy_mig_state_count=0
export unhealthy_mig_state_count_threshold=3

get_healthy_instances_count() {
	healthy_services_counter=$(gcloud compute backend-services get-health doughnut-app-service --global | grep "healthState: HEALTHY" | wc -l)
}

while :
do
	get_healthy_instances_count
	if [ $healthy_services_counter -lt $expected_healthy_services_count ]; then
		((unhealthy_mig_state_count=unhealthy_mig_state_count+1))
	else
		unhealthy_mig_state_count=0
	fi

	sleep 90

	if [ $unhealthy_mig_state_count -gt $unhealthy_mig_state_count_threshold ]; then
		say "doughnut Production is DOWN!"
	fi
done
