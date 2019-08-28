package com.cgi.eoss.fstep.orchestrator.service;

import com.cgi.eoss.fstep.rpc.FileStream;
import com.google.protobuf.Message;
import io.grpc.stub.ClientResponseObserver;

@FunctionalInterface
public interface OutputFileProvider<T extends Message> {

	void provideFile(ClientResponseObserver<T, FileStream> fileStreamObserver);

}
