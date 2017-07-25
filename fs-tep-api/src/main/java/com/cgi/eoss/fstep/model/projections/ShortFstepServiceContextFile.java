package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link FstepServiceContextFile}s. Embeds the service as a ShortService, and
 * omits the content.</p>
 */
@Projection(name = "shortServiceFile", types = {FstepServiceContextFile.class})
public interface ShortFstepServiceContextFile extends Identifiable<Long> {
    ShortFstepService getService();
    String getFilename();
    boolean isExecutable();
}
