package com.cgi.eoss.fstep.orchestrator.utils;

import com.cgi.eoss.fstep.rpc.Job;
import com.cgi.eoss.fstep.rpc.Service;

public class ModelToGrpcUtils {
	
	private ModelToGrpcUtils() {
		
	}
	
    /**
         * <p>Convert a {@link com.cgi.eoss.fstep.model.Job} to its gRPC {@link Job} representation.</p>
         *
         * @param job The job to be converted.
         * @return The input job mapped to {@link Job}.
         */
        public static Job toRpcJob(com.cgi.eoss.fstep.model.Job job) {
            return Job.newBuilder()
                    .setId(job.getExtId())
                    .setIntJobId(String.valueOf(job.getId()))
                    .setUserId(job.getOwner().getName())
                    .setServiceId(job.getConfig().getService().getName())
                    .build();
        }
        
        /**
         * <p>Convert a {@link com.cgi.eoss.fstep.model.Service} to its gRPC {@link Service} representation.</p>
         *
         * @param job The service to be converted.
         * @return The input service mapped to {@link Service}.
         */
        public static Service toRpcService(com.cgi.eoss.fstep.model.FstepService service) {
            return Service.newBuilder()
            			.setId(String.valueOf(service.getId()))
                    .setName(service.getName())
                    .setDockerImageTag(service.getDockerTag())
                    .build();
        }
}

