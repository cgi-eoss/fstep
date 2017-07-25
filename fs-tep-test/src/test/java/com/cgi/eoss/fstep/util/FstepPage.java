package com.cgi.eoss.fstep.util;

import lombok.Getter;

public enum FstepPage {
    EXPLORER("/app/");

    @Getter
    private final String url;

    FstepPage(String url) {
        this.url = url;
    }

}
