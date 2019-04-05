package com.cgi.eoss.fstep.worker.worker;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * <p>Describes the workspace for an FS-TEP job execution.</p>
 */
@Data
@Builder
class JobEnvironment {
    /**
     * <p>The identifier of the job using this environment.</p>
     */
    private String jobId;

    /**
     * <p>The in-process workspace, used for temporary files created during service execution.</p>
     */
    private Path workingDir;

    /**
     * <p>The input workspace, pre-populated by the {@link FstepWorker} before service execution.</p>
     */
    private Path inputDir;

    /**
     * <p>The output workspace, used for end-result files created during service execution. The {@link FstepWorker}
     * collects from this path.</p>
     */
    private Path outputDir;
    
    /**
     * <p>The persistent workspace, used for files that should survive across service executions.</p>
     */
    private Path persistentDir;
}
