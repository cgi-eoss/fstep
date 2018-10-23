package com.cgi.eoss.fstep.orchestrator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.rpc.Job;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.PortBinding;

/**
 * <p>Service for more specific interaction with graphical application-type FS-TEP services.</p>
 */
@Component
public class FstepGuiServiceManager {

    static final String GUACAMOLE_PORT = "8080/tcp";

    @Autowired
    public FstepGuiServiceManager() {
    }

    /**
     * @return The string representation of a URL suitable for accessing the GUI application represented by the given
     * Job running on the given worker.
     */
    public PortBinding getGuiPortBinding(FstepWorkerGrpc.FstepWorkerBlockingStub worker, Job rpcJob) {
        return worker.getPortBindings(rpcJob).getBindingsList().stream()
                .filter(b -> b.getPortDef().equals(GUACAMOLE_PORT))
                .findFirst()
                .orElseThrow(() -> new ServiceExecutionException("Could not find GUI port on docker container for job: " + rpcJob.getId()));
    }

}
