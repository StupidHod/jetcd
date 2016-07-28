# Overview
The EtcdLease implement interface `grant` and `revoke` to new a lease and revoke a lease to etcd.
It can manage the leases by send keep alive request for leases.

##grant function
1. The function build leaseGrantRequest with ttl value
2. It call respond gRPC interface with leaseGrantRequest to build a new lease in etcd.

##revoke function
1. The function build leaseRevokeRequest with lease id
2. It call respond gRPC interface with leaseRevokeRequest to revoke respond lease.

## startKeepAliveService function
1. The function will start a backgroud scheduler `keepAliveSchedule` to send keep alive request for lease registered to EtcdLease Client.
2. It will open a StreamObserver to etcd, send request to this StreamObserver can keep the lease alive
3. It will new a ScheduledExecutorService to run `keepAliveExecutor` and `deadLineExecutor` periodically

## closeKeepAliveService function
1. This function will end the backgroud scheduler.
2. It will close the StreamObserver to etcd

## setEtcdLeaseHandler function
1. This function set EtcdLeaseHandler for etcd lease client
2. The EtcdLeaseHandler interface:
     * onKeepAliveRespond
         - It will be called when lease client receive response from etcd
     * onLeaseExpired
         - It will be called when any lease expire and removed from etcd.
     * onError
         - It will be called when some exception occurred.

## keepAlive function
1. It will create a Lease Object, and keep the lease alive.
2. It will add the created lease object to the keepAlives map.
3. The background scheduler will send keep alive requests for the added lease when it approaches to the nextKeepAliveTime.

## keepAliveExecutor function
1. This function is called periodically by `keepAliveSchedule`
2. The lease may expire as:
     * The etcd respond overtime.
     * The client faile to send request in time.
3. It will scan the keepAlives map and find the leases that requires keepAlive requests to send based on its nextKeepAliveTime.
4. Send request to the StreamObserver for these leases

## deadLineExecutor function
1. This function is called periodically by `keepAliveSchedule`.
2. It will scan the keepAlives map and find the leases that requires removed from the map based on its DeadLine
3. call the on onLeaseExpired method for these leases.

## keepAliveResponseStreamObserver interface instance
1. The StreamObserver is the stream etcd uses to send responses.
2. The gRPC client calls onNext when etcd server delivers a response.
3. The onNext function will call `processKeepAliveRespond`

## processKeepAliveRespond function
1. It will get the lease instance based on the leaseID.
2. if repond ttl < 0, it remove respond lease from keepAlives map and call `onLeaseExpired`
3. else it reset the lease's nextKeepAliveTime and deadLine value