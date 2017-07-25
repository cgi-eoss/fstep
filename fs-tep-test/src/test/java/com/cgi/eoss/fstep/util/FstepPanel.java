package com.cgi.eoss.fstep.util;

import lombok.Getter;

public enum FstepPanel {
    SEARCH("#sidenav i[uib-tooltip='Search']");

    @Getter
    private final String selector;

    FstepPanel(String selector) {
        this.selector = selector;
    }

}
