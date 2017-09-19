package com.cgi.eoss.fstep.search.fstep;

import lombok.Builder;
import lombok.Data;
import okhttp3.HttpUrl;

@Data
@Builder
class FstepSearchProperties {

    private final HttpUrl baseUrl;
    private final String username;
    private final String password;

}