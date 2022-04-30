#!/bin/bash

gcloud compute ssl-certificates create doughnut-star-odd-e-com \
    --certificate=../ssl/star_odd-e_com.crt \
    --private-key=../ssl/star_odd-e_com.key \
    --global
