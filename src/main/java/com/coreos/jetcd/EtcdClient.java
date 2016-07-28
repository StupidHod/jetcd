package com.coreos.jetcd;

import com.coreos.jetcd.api.KVGrpc;
import com.coreos.jetcd.api.LeaseGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Etcd Client
 */
public class EtcdClient {

    private final ManagedChannelBuilder<?> channelBuilder;
    private final String[] endpoints;
    private final ManagedChannel channel;

    private final EtcdKV kvClient;
    private final EtcdLease leaseClient;

    public EtcdClient(ManagedChannelBuilder<?> channelBuilder, EtcdClientBuilder builder) {
        this.endpoints = new String[builder.endpoints().size()];
        builder.endpoints().toArray(this.endpoints);
        this.channelBuilder = channelBuilder != null ? channelBuilder :
                ManagedChannelBuilder
                        .forAddress("localhost", 2379);
        this.channel = this.channelBuilder.build();

        KVGrpc.KVFutureStub kvStub = KVGrpc.newFutureStub(this.channel);

        this.kvClient = newKVClient(kvStub);
        this.leaseClient = newLeaseClient(channel);
    }

    /**
     * create a new KV client.
     *
     * @return new KV client
     */
    public EtcdKV newKVClient(KVGrpc.KVFutureStub stub) {
        return new EtcdKVImpl(stub);
    }

    public EtcdLease newLeaseClient(ManagedChannel channel) {
        return new EtcdLeaseImpl(channel);
    }

    public EtcdLease getLeaseClient(){
        return leaseClient;
    }

}
