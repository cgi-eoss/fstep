package com.cgi.eoss.fstep;

import com.cgi.eoss.fstep.util.FstepPage;
import com.cgi.eoss.fstep.util.FstepWebClient;
import cucumber.api.java8.En;

public class CommonStepdefs implements En {

    public CommonStepdefs(FstepWebClient client) {
        Given("^I am on the \"([^\"]*)\" page$", (String page) -> {
            client.load(FstepPage.valueOf(page));
        });

        Given("^I am logged in as \"([^\"]*)\" with role \"([^\"]*)\"$", (String username, String role) -> {
            client.loginAs(username, role);
        });

        Given("^the default services are loaded$", () -> {
            client.oneShotApiPost("/contentAuthority/services/restoreDefaults");
            client.oneShotApiPost("/contentAuthority/services/wps/syncAllPublic");
        });
    }

}
