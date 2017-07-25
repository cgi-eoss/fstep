package com.cgi.eoss.fstep.zoomanager.service;

import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.rpc.UpdateActiveZooServicesResult;
import com.cgi.eoss.fstep.rpc.WpsServiceDescriptors;
import com.cgi.eoss.fstep.rpc.ZooManagerServiceGrpc;
import com.cgi.eoss.fstep.zoomanager.stubs.ZooStubWriter;
import com.cgi.eoss.fstep.zoomanager.zcfg.ZcfgWriter;
import com.google.common.base.Preconditions;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@GRpcService
@Log4j2
public class ZooManagerService extends ZooManagerServiceGrpc.ZooManagerServiceImplBase {

    private final ZooStubWriter zooStubWriter;
    private final ZcfgWriter zcfgWriter;
    private final Path zcfgBasePath;
    private final Path zooServicesStubJar;

    @Autowired
    public ZooManagerService(ZooStubWriter zooStubWriter, FstepServiceDescriptorHandler serviceDescriptorHandler, ZcfgWriter zcfgWriter,
                             @Qualifier("zcfgBasePath") Path zcfgBasePath,
                             @Qualifier("zooServicesStubJar") Path zooServicesStubJar) {
        this.zooStubWriter = zooStubWriter;
        this.zcfgWriter = zcfgWriter;
        this.zcfgBasePath = zcfgBasePath;
        this.zooServicesStubJar = zooServicesStubJar;
    }

    @Override
    public void updateActiveZooServices(WpsServiceDescriptors request, StreamObserver<UpdateActiveZooServicesResult> responseObserver) {
        try {
            // Make sure all the paths are writable
            Preconditions.checkArgument(Files.isWritable(zcfgBasePath), "ZOO config path is not writable: %s", zcfgBasePath);
            Preconditions.checkArgument((Files.exists(zooServicesStubJar) && Files.isWritable(zooServicesStubJar)) || Files.isWritable(zooServicesStubJar.getParent()),
                    "ZOO services stub jar is not writable: %s", zooServicesStubJar);

            Set<FstepServiceDescriptor> services = request.getServicesList().stream()
                    .map(d -> FstepServiceDescriptor.fromYaml(d.getContent().toStringUtf8()))
                    .collect(Collectors.toSet());

            zooStubWriter.generateWpsStubLibrary(services, zooServicesStubJar);
            zcfgWriter.generateZcfgs(services, zcfgBasePath);

            responseObserver.onNext(UpdateActiveZooServicesResult.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to synchronise ZOO Kernel services", e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

}
