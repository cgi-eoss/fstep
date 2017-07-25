package com.cgi.eoss.fstep.orchestrator.service;

import com.cgi.eoss.fstep.orchestrator.OrchestratorConfig;
import com.cgi.eoss.fstep.orchestrator.OrchestratorTestConfig;
import com.cgi.eoss.fstep.rpc.Job;
import com.cgi.eoss.fstep.rpc.worker.Binding;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.PortBinding;
import com.cgi.eoss.fstep.rpc.worker.PortBindings;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.ServerImpl;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {OrchestratorConfig.class, OrchestratorTestConfig.class})
@TestPropertySource("classpath:test-orchestrator.properties")
@Transactional
public class FstepGuiServiceManagerTest {

    @Autowired
    private FstepGuiServiceManager fstepGuiServiceManager;

    @Autowired
    private InProcessServerBuilder serverBuilder;

    @Autowired
    private ManagedChannelBuilder channelBuilder;

    private FstepWorkerGrpc.FstepWorkerBlockingStub worker;

    private ServerImpl server;

    @Before
    public void setUp() throws IOException {
        serverBuilder.addService(new WorkerStub());
        server = serverBuilder.build().start();

        worker = FstepWorkerGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void getGuiUrl() throws Exception {
        String guiUrl = fstepGuiServiceManager.getGuiUrl(worker, Job.getDefaultInstance());
        assertThat(guiUrl, is("/gui/:12345/"));
    }

    private class WorkerStub extends FstepWorkerGrpc.FstepWorkerImplBase {
        @Override
        public void getPortBindings(Job request, StreamObserver<PortBindings> responseObserver) {
            PortBinding portBinding = PortBinding.newBuilder()
                    .setPortDef("8080/tcp")
                    .setBinding(Binding.newBuilder().setPort(12345).build())
                    .build();

            responseObserver.onNext(PortBindings.newBuilder().addBindings(portBinding).build());
            responseObserver.onCompleted();
        }
    }

}