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
 * @author pete
 */
public class ImposterBuilderTest {
    @Test
    public void testFullConfig() throws Exception {
        final Path configDir = Paths.get(ImposterBuilderTest.class.getResource("/config").toURI());

        final MockEngine imposter = new ImposterBuilder<>()
                .withPluginClass(OpenApiPluginImpl.class)
                .withConfigurationDir(configDir)
                .startBlocking();

        testMockEndpoint(imposter);
    }

    @Test
    public void testStandaloneSpec() throws Exception {
        final Path specFile = Paths.get(ImposterBuilderTest.class.getResource("/config/petstore-simple.yaml").toURI());

        final MockEngine imposter = new OpenApiImposterBuilder<>()
                .withSpecificationFile(specFile)
                .startBlocking();

        testMockEndpoint(imposter);
    }

    private void testMockEndpoint(MockEngine imposter) {
        RestAssured.baseURI = String.valueOf(imposter.getBaseUrl());
        given().when()
                .log().ifValidationFails()
                .get("/v1/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }
}
