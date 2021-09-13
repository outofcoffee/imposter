package io.gatehill.imposter.embedded;

import com.jayway.restassured.RestAssured;
import io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder;
import io.gatehill.imposter.plugin.openapi.OpenApiPluginImpl;
import io.gatehill.imposter.util.HttpUtil;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiImposterBuilderTest {
    @Test
    public void testFullConfig() throws Exception {
        final Path configDir = Paths.get(OpenApiImposterBuilderTest.class.getResource("/config").toURI());

        final MockEngine imposter = new OpenApiImposterBuilder<>()
                .withPluginClass(OpenApiPluginImpl.class)
                .withConfigurationDir(configDir)
                .startBlocking();

        invokeMockEndpoint(imposter);
    }

    @Test
    public void testStandaloneSpec() throws Exception {
        final Path specFile = Paths.get(OpenApiImposterBuilderTest.class.getResource("/config/petstore-simple.yaml").toURI());

        final MockEngine imposter = new OpenApiImposterBuilder<>()
                .withSpecificationFile(specFile)
                .startBlocking();

        invokeMockEndpoint(imposter);
    }

    private void invokeMockEndpoint(MockEngine imposter) {
        RestAssured.baseURI = String.valueOf(imposter.getBaseUrl());
        given().when()
                .log().ifValidationFails()
                .get("/v1/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }
}
