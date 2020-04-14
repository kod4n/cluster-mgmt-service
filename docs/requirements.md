# Cluster Management Service Design Requirements
## Introduction  
The following requirements are intended to provide guidance and structure for
implementing a Cluster Management Service. Each requirement has been identified
as an essential part of the architecture and must be incorporated to maximize
value to administrators and customers.

## Scope  
These requirements are scoped to encompass both business and technical
requirements for a Cluster Management Service.  

## Requirements

### Provision Kubernetes clusters
![Generic badge](https://img.shields.io/badge/BUSINESS-MVP-GREEN.svg)  
As a user, I want CrateKube to provision Kubernetes clusters for me, so that I
can use Kubernetes without having to learn how to create a cluster, because
Kubernetes is complicated and it is easy to make mistakes when creating a new cluster.

#### Leverage RKE for direct cluster creation
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user, I want the Cluster Management Service to leverage RKE for direct cluster creation,
so that the CrateKube team can focus on building services that provide value to me,
because without RKE the CrateKube team would need to invest in building a tool similar to RKE.

##### Persistence of RKE state
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user I want the Cluster Management Service to persist RKE state, so that my
cluster can be reconfigured after it is initially created, because without
persisting RKE state the cluster will be inaccessible after provisioning.

#### Hosted cluster creation  
![Generic badge](https://img.shields.io/badge/TECHNICAL-POSTMVP-YELLOW.svg)

### Configure Kubernetes clusters  
![Generic badge](https://img.shields.io/badge/BUSINESS-MVP-GREEN.svg)  
As a user I want the Cluster Management Service to configure Kubernetes clusters,
so that my Kubernetes cluster is ready for use,
because users would otherwise need to learn all of the Kubernetes configuration settings that CrateKube is built to abstract.  

### Tear down provisioned Kubernetes clusters  
![Generic badge](https://img.shields.io/badge/BUSINESS-MVP-GREEN.svg)  
As a user, I want CrateKube to be able to clean up the clusters it creates,
so that I have control over my costs,
because users unfamiliar with AWS operations may not be able to find all of the artifacts CrateKube creates unless we automate the teardown

#### Leverage RKE for direct cluster tear down
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user, I want the Cluster Management Service to leverage RKE for direct cluster tear down,
so that the CrateKube team can focus on building services that provide value to me,
because without RKE the CrateKube team would need to invest in building a tool similar to RKE.

#### Hosted cluster tear down  
![Generic badge](https://img.shields.io/badge/TECHNICAL-POSTMVP-YELLOW.svg)

### Async for long running tasks  
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user, I want long running tasks to be handled asynchronously,
so that cloud resources have enough time to be created,
because creating cloud resources could take a long time and clients may timeout waiting for a synchronous response.  

### Security  
![Generic badge](https://img.shields.io/badge/BUSINESS-MVP-GREEN.svg)  
As a user, I want the Cluster Management Service to be secure,
so that my resources are protected,
because without security my resources may be compromised by unauthorized users.   

#### token_authc and token_authz  
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user, I want token authentication and authorization,
so that REST resources are protected,
because without some form of authentication and authorization malicious actors may gain access to my cluster.

### Monitor Kubernetes clusters  
![Generic badge](https://img.shields.io/badge/BUSINESS-POSTMVP-YELLOW.svg)  

### Kubernetes hardening
![Generic badge](https://img.shields.io/badge/BUSINESS-POSTMVP-YELLOW.svg)  

#### Pod security policies  
![Generic badge](https://img.shields.io/badge/TECHNICAL-POSTMVP-YELLOW.svg)  

## Decisions made during requirements gathering  
The following decisions were made during requirements gathering:

### Leveraging RKE
Rancher Kubernetes Engine (RKE) will be used for direct cluster creation and tear down because it vastly mitigates complexity.

###  RKE state location
In order to move fast, RKE state should be stored in a local filesystem backed by EBS. This will allow the CrateKube team to leverage previous work. In the future, a remote solution that allows for locking, such as S3, should be used.

### Async APIs
Due to the long running nature of infrastructure provisioning requests, some APIs will need to be asynchronous. This will allow clients to request resources and eventually receive them, preventing timeout issues.

### Message queues
Message queues will not be part of the scope of the CrateKube MVP. The asynchronous API design will allow us to support messaging queues between services in the future, but messaging queues were determined to not be a critical feature in the MVP.
