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
As a user, I want my cluster to be manageable, so that my cluster 
can be created and configured, because without the ability to manage 
my cluster no changes can be made after it is created.

#### Leverage RKE for direct cluster creation
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user, I want the Cluster Management Service to leverage RKE for direct cluster creation, 
so that direct cluster creation is reliable, 
because RKE makes provisioning clusters easily repeatable.  
##### Persistence of RKE state
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user I want the Cluster Management Service to persist RKE state, so that cluster 
operations can be managed, because without persisting RKE state the cluster will be 
inaccessible after provisioning. 
      
#### Hosted cluster creation  
![Generic badge](https://img.shields.io/badge/TECHNICAL-POSTMVP-YELLOW.svg) 

### Configure Kubernetes clusters  
![Generic badge](https://img.shields.io/badge/BUSINESS-MVP-GREEN.svg)  
As a user I want the Cluster Management Service to configure Kubernetes clusters, 
so that my Kubernetes cluster is ready for use, 
because an non configured Kubernetes cluster is unusable.  

### Tear down provisioned Kubernetes clusters  
![Generic badge](https://img.shields.io/badge/BUSINESS-MVP-GREEN.svg)  
As a user, I want the Cluster Management Service to leverage RKE for direct cluster tear down, 
so that direct cluster tear down is reliable, 
because RKE makes tearing down clusters easily repeatable.  

#### Leverage RKE for direct cluster tear down
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user, I want the Cluster Management Service to leverage RKE for direct cluster tear down, 
so that direct cluster tear down is reliable, 
because RKE makes tearing down clusters easily repeatable.
    
#### Hosted cluster tear down  
![Generic badge](https://img.shields.io/badge/TECHNICAL-POSTMVP-YELLOW.svg) 
    
### Async for long running tasks  
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user, I want long running tasks to be handled asynchronously, 
so that cloud resources have enough time to be created, 
because creating cloud resources could take a long time and clients will timeout waiting for a synchronous response.  

### Security  
![Generic badge](https://img.shields.io/badge/BUSINESS-MVP-GREEN.svg)  
As a user, I want the Cluster Management Service to be secure, 
so that my resources are protected, 
because without security resources may be manipulated by unauthorized users.   

#### token_authc and token_authz  
![Generic badge](https://img.shields.io/badge/TECHNICAL-MVP-GREEN.svg)  
As a user, I want token authentication and authorization implemented at runtime, 
so that REST resources are protected, 
because without security resources may be manipulated by unauthorized users.

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
In order to move fast, RKE state should be stored in a local filesystem backed by EBS. This will allow the Crate team to leverage previous work. In the future, a remote solution that allows for locking, such as S3, should be used. 

### Async APIs
Due to the long running nature of infrastructure provisioning requests, some APIs will need to be asynchronous. This will allow clients to request resources and eventually receive them, preventing timeout issues. 

### Message queues
Message queues should be available for implementation following MVP. REST API interfaces should be designed with this in mind. 

    
        
