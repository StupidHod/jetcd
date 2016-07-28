package com.coreos.jetcd;

import com.coreos.jetcd.api.LeaseGrantResponse;
import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.api.LeaseRevokeResponse;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Interface of Lease talking to etcd
 */
public interface EtcdLease {

    /**
     * New a lease with ttl value
     * @param ttl ttl value
     * @return
     */
    ListenableFuture<LeaseGrantResponse> grant(long ttl);

    /**
     * revoke one lease and the key bind to this lease will be removed
     * @param leaseId the id of lease
     * @return
     */
    ListenableFuture<LeaseRevokeResponse> revoke(long leaseId);

    /**
     * keep alive one lease in background
     * @param leaseId
     */
    void keepAlive(long leaseId);

    /**
     * keep alive one lease only once
     * @param leaseId
     * @return The keepAlive response
     */
    ListenableFuture<LeaseKeepAliveResponse> keepAliveOnce(long leaseId);

    /**
     * set EtcdLeaseHandler for EtcdLease
     * @param etcdLeaseHandler
     */
    void setEtcdLeaseHandler(EtcdLeaseHandler etcdLeaseHandler);

    /**
     * Init the request stream to etcd
     * start schedule to keep heartbeat to keep alive and remove dead leases
     */
    void startKeepAliveService() throws IllegalStateException;

    /**
     * end the schedule for keep alive and remove dead leases
     */
    void closeKeepAliveService() throws IllegalStateException;

    /**
     * This interface is called by Etcd Lease client to notify user about lease expiration and exception
     */
    interface EtcdLeaseHandler{

        /**
         * keepAliveResponse will be called when heartbeat keep alive call respond.
         * @param keepAliveResponse
         */
        void onKeepAliveRespond(LeaseKeepAliveResponse keepAliveResponse);

        /**
         * onLeaseExpired will be called when any leases is expired and remove from keep alive task.
         * @param leaseId
         */
        void onLeaseExpired(long leaseId);

        /**
         * onThrowable will be called when keep alive encountered exception
         * @param throwable
         */
        void onThrowable(Throwable throwable);
    }

}
