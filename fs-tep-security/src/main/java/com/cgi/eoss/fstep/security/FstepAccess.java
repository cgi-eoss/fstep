package com.cgi.eoss.fstep.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FstepAccess {
    private final boolean published;
    private final boolean publishRequested;
    private final FstepPermission currentLevel;
}
