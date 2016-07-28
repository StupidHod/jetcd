package com.coreos.jetcd;

import com.coreos.jetcd.api.LeaseGrantResponse;
import com.coreos.jetcd.api.LeaseKeepAliveResponse;
import com.coreos.jetcd.api.LeaseRevokeResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveServiceAlreadyStartException;
import com.coreos.jetcd.lease.LeaseKeepAliveServiceNotStartException;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

/**
 * The class test the function related to EtcdLease
 */
public class EtcdLeaseTest {

    private EtcdClient etcdClient;
    private EtcdLease etcdLease;
    private long leaseId;
    private Assertion as = new Assertion();
    private boolean testOnline = true;

    private Object lock = new Object();

    @BeforeTest
    public void setup() {
        ManagedChannelBuilder<?> channelBuilder =
                NettyChannelBuilder
                        .forAddress("localhost", 2379)
                        .usePlaintext(true);
        //.sslContext(GrpcSslContexts.forClient().trustManager(new File("roots.pem")).build());

        etcdClient = new EtcdClient(channelBuilder,
                EtcdClientBuilder.newBuilder().endpoints("127.0.0.1:2379"));
        etcdLease = etcdClient.getLeaseClient();
    }

    @Test
    public void testGrant() {

        ListenableFuture<LeaseGrantResponse> responseListenableFuture
                = etcdLease.grant(2);
        leaseId = -1;
        try {
            leaseId = responseListenableFuture.get().getID();
        }catch (Exception ie){
            testOnline = false;
        }
        if (testOnline) as.assertTrue(leaseId > 0);
    }

    @Test(dependsOnMethods = {"testGrant"})
    public void testStartKeepAliveService() throws LeaseKeepAliveServiceAlreadyStartException {
        if(!testOnline){
            return;
        }
        etcdLease.startKeepAliveService();
    }

    @Test(dependsOnMethods = {"testStartKeepAliveService"})
    public void testSetEtcdLeaseHandler() {
        if(!testOnline){
            return;
        }
        etcdLease.setEtcdLeaseHandler(new EtcdLease.EtcdLeaseHandler() {
            @Override
            public void onKeepAliveRespond(LeaseKeepAliveResponse keepAliveResponse) {
                lock.notify();
            }

            @Override
            public void onLeaseExpired(long leaseId) {
            }

            @Override
            public void onThrowable(Throwable throwable) {
            }
        });
        System.out.println(this);
    }

    @Test(dependsOnMethods = {"testSetEtcdLeaseHandler"})
    public void testKeepAlive() throws InterruptedException {
        if(!testOnline){
            return;
        }
        etcdLease.keepAlive(leaseId);
        synchronized (lock) {
            lock.wait(1000);
        }
    }

    @Test(dependsOnMethods = {"testKeepAlive"})
    public void testRevoke() {
        if(!testOnline){
            return;
        }
        ListenableFuture<LeaseRevokeResponse> futureResp = etcdLease.revoke(leaseId);
        Throwable throwable = null;
        try {
            futureResp.get();
        } catch (Exception e) {
            throwable = e;
        }
        as.assertNull(throwable, "lease revoke test");
    }

    @Test(dependsOnMethods = {"testRevoke"})
    public void testEndKeepAliveService() throws LeaseKeepAliveServiceNotStartException {
        if(!testOnline){
            return;
        }
        System.out.println(this);
        etcdLease.closeKeepAliveService();
    }

}
