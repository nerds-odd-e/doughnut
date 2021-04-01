#!/bin/bash

gcloud compute ssl-certificates create star-odd-e-com \
    --certificate=../ssl/private/star_odd-e_com.crt \
    --private-key=../ssl/private/odde.key \
    --global
