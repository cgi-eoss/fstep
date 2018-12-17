package com.cgi.eoss.fstep.search.creodias;

import lombok.Builder;
import lombok.Data;
import okhttp3.HttpUrl;

@Data
@Builder
class CreoDIASSearchProperties {

    private final HttpUrl baseUrl;
    private final String username;
    private final String password;

}