package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.rpc.GetServiceContextFilesParams;
import com.cgi.eoss.fstep.rpc.ServiceContextFiles;
import com.cgi.eoss.fstep.rpc.ServiceContextFilesServiceGrpc;
import com.cgi.eoss.fstep.rpc.ShortFile;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@GRpcService
public class RpcServiceFileService extends ServiceContextFilesServiceGrpc.ServiceContextFilesServiceImplBase {

    private final ServiceDataService serviceDataService;
    private final ServiceFileDataService fileDataService;

    @Autowired
    public RpcServiceFileService(ServiceDataService serviceDataService, ServiceFileDataService fileDataService) {
        this.serviceDataService = serviceDataService;
        this.fileDataService = fileDataService;
    }

    @Override
    public void getServiceContextFiles(GetServiceContextFilesParams request, StreamObserver<ServiceContextFiles> responseObserver) {
        List<FstepServiceContextFile> serviceFiles = fileDataService.findByService(serviceDataService.getByName(request.getServiceName()));

        ServiceContextFiles.Builder responseBuilder = ServiceContextFiles.newBuilder()
                .setServiceName(request.getServiceName());
        serviceFiles.stream().map(this::convertToRpcShortFile).forEach(responseBuilder::addFiles);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private ShortFile convertToRpcShortFile(FstepServiceContextFile file) {
        return ShortFile.newBuilder()
                .setFilename(file.getFilename())
                .setContent(ByteString.copyFromUtf8(file.getContent()))
                .setExecutable(file.isExecutable())
                .build();
    }

}
