package com.cgi.eoss.fstep.model.projections;

import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;

/**
 * <p>Default JSON projection for embedded {@link FstepServiceTemplateFile}s. Embeds the service as a ShortServiceTemplate, and
 * omits the content.</p>
 */
@Projection(name = "shortServiceTemplateFile", types = {FstepServiceTemplateFile.class})
public interface ShortFstepServiceTemplateFile extends Identifiable<Long> {
    ShortFstepServiceTemplate getServiceTemplate();
    String getFilename();
    boolean isExecutable();
}
