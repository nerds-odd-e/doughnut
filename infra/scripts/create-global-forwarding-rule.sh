#!/bin/bash
gcloud compute forwarding-rules create doughnut-http-rule \
	--target-http-proxy doughnut-service-proxy \
	--ports 80,443 \
	--global
