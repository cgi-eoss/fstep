package com.cgi.eoss.fstep.util;

import lombok.Getter;

public enum FstepFormField {
    NEW_PROJECT_NAME("#item-dialog[aria-label='Create Project dialog'] md-input-container input");

    @Getter
    private final String selector;

    FstepFormField(String selector) {
        this.selector = selector;
    }

}
