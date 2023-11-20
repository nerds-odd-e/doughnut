## GCP Production environment

### 1. Interacting with gcloud CLI for cloud infrastructure management

- [Install `Google Cloud SDK`](https://cloud.google.com/sdk/docs/install)
- [Create App Server in GCloud Compute](infra/scripts/create-gcloud-app-compute.sh)
- Login to gcloud sdk: `gcloud auth login`
- Check your login: `gcloud auth list`
- Set/Point to gcloud dough project: `gcloud config set project carbon-syntax-298809`
- Check you can see the project as login user: `gcloud config list`

### 2. View/tail GCP VM instance logs

```bash
gcloud auth login
gcloud config set project carbon-syntax-298809
# Query GCP MIG instance/s health state and grep instance id of each GCP VM in MIG
infra/scripts/check-mig-doughnut-app-service-health.sh
# Expected output
# ❯ ./check-mig-doughnut-app-service-health.sh
# ---
# backend: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instanceGroups/doughnut-app-group
# status:
#  healthStatus:
#  - healthState: HEALTHY
#    instance: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instances/doughnut-app-group-0c2b
#    ipAddress: 10.142.0.7
#    port: 8081
#  - healthState: HEALTHY
#    instance: https://www.googleapis.com/compute/v1/projects/carbon-syntax-298809/zones/us-east1-b/instances/doughnut-app-group-2j9f
#    ipAddress: 10.142.0.8
#    port: 8081
#  kind: compute#backendServiceGroupHealth

# View instance logs - Take/use one of the above healthcheck report instance id for next command (e.g. doughnut-app-group-2j9f)
infra/scripts/view-mig-doughnut-app-instance-logs.sh doughnut-app-group-2j9f

# Tail instance logs - Take/use one of the above healthcheck report instance id for next command (e.g. doughnut-app-group-2j9f)
infra/scripts/tail-mig-doughnut-app-instance-logs.sh doughnut-app-group-2j9f
```

### 3. Building/refreshing doughnut-app MIG VM instance/s base image with Packer + GoogleCompute builder

We use packer + googlecompute builder + shell provisioner to construct and materialise base VM image to speed up deployment and control our OS patches and dependent packages and libraries upgrades

- [Packer](https://www.packer.io)
- [packer googlecompute builder](https://www.packer.io/docs/builders/googlecompute)
- [SaltStack](https://docs.saltproject.io/en/latest/)

#### How-to

From `infra` directory, run the following:

Login to dough GCP project account with `gcloud auth login`
Configure gcloud CLI to project ID with `gcloud config set project carbon-syntax-298809`

```bash
cd infra
export GCLOUDSDK_CORE_PROJECT="$(gcloud config get-value project)"
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/carbon-syntax-298809-f31377ba77a9.json
PACKER_LOG=1 packer build packer.json
```

Expect to see following log line towards end of Packer build stdout log:
`--> googlecompute: A disk image was created: doughnut-debian12-mysql80-base-saltstack`
