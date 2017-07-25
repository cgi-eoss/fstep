package com.cgi.eoss.fstep;

import com.cgi.eoss.fstep.util.FstepWebClient;
import cucumber.api.java8.En;
import okhttp3.Response;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class WpsStepdefs implements En {

    private String responseBody;

    public WpsStepdefs(FstepWebClient client) {
        Given("^FS-TEP WPS is available", () -> {
            assertThat(client.get("/secure/wps/zoo_loader.cgi").code(), is(400));
        });

        When("^a user requests GetCapabilities from WPS$", () -> {
            Response response = client.get("/secure/wps/zoo_loader.cgi?request=GetCapabilities&service=WPS");
            assertThat(response.code(), is(200));
            responseBody = client.getContent(response);
        });

        Then("^they receive the FS-TEP service list$", () -> {
            assertThat(responseBody, allOf(
                    containsString("<ows:Title>Food Security TEP (FS-TEP)  WPS Server</ows:Title>"),
                    containsString("<ows:Identifier>LandCoverS2</ows:Identifier>"),
                    containsString("<ows:Identifier>VegetationIndices</ows:Identifier>"),
                    containsString("<ows:Identifier>QGIS</ows:Identifier>"),
                    containsString("<ows:Identifier>SNAP</ows:Identifier>")
            ));
        });
    }

}
