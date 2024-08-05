#!/bin/bash

gcloud compute target-https-proxies update doughnut-app-service-map-target-proxy-2 \
--global \
--ssl-certificates=doughnut-odd-e-com-2 \
--global-ssl-certificates \
