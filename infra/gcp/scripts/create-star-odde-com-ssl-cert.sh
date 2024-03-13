#!/bin/bash

gcloud compute ssl-certificates create star-odd-e-com \
    --certificate=../ssl/2023/star_odd-e_com.crt \
    --private-key=../ssl/2023/star_odd-e_com.key \
    --global
