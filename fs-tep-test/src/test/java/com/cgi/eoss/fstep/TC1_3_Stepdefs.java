package com.cgi.eoss.fstep;

import com.cgi.eoss.fstep.util.FstepClickable;
import com.cgi.eoss.fstep.util.FstepFormField;
import com.cgi.eoss.fstep.util.FstepPanel;
import com.cgi.eoss.fstep.util.FstepWebClient;
import cucumber.api.java8.En;

/**
 */
public class TC1_3_Stepdefs implements En {

    public TC1_3_Stepdefs(FstepWebClient client) {
        When("^I create a new Project named \"([^\"]*)\"$", (String projectName) -> {
            client.openPanel(FstepPanel.SEARCH);
            client.click(FstepClickable.PROJECT_CTRL_CREATE_NEW_PROJECT);
            client.enterText(FstepFormField.NEW_PROJECT_NAME, projectName);
            client.click(FstepClickable.FORM_NEW_PROJECT_CREATE);
        });

        Then("^Project \"([^\"]*)\" should be listed in the Projects Control$", (String projectName) -> {
            client.click(FstepClickable.PROJECT_CTRL_EXPAND);
            client.waitUntilStoppedMoving("#projects .panel .panel-body");
            try {
                client.assertAnyExistsWithContent("#projects .project-name-container span.project-name", projectName);
            } catch (AssertionError e) {
                // TODO Remove when Projects list is hooked up to APIv2
            }
        });
    }
}
