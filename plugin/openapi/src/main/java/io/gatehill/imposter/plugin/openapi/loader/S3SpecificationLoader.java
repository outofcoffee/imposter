package io.gatehill.imposter.plugin.openapi.loader;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * Loads an OpenAPI specification from S3.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class S3SpecificationLoader {
    public static final String ENV_OPENAPI_S3_API_ENDPOINT = "IMPOSTER_OPENAPI_S3_API_ENDPOINT";
    public static final String SYS_PROP_OPENAPI_S3_API_ENDPOINT = "imposter.openapi.s3.api.endpoint";
    private static final Logger LOGGER = LoggerFactory.getLogger(S3SpecificationLoader.class);

    private static S3SpecificationLoader instance;

    private final AmazonS3 s3client;

    private S3SpecificationLoader() {
        final AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard().enablePathStyleAccess();
        ofNullable(System.getProperty(SYS_PROP_OPENAPI_S3_API_ENDPOINT, System.getenv(ENV_OPENAPI_S3_API_ENDPOINT))).ifPresent(s3Endpoint -> {
            clientBuilder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(s3Endpoint, new DefaultAwsRegionProviderChain().getRegion())
            );
        });
        s3client = clientBuilder.build();
    }

    public static S3SpecificationLoader getInstance() {
        if (isNull(instance)) {
            instance = new S3SpecificationLoader();
        }
        return instance;
    }

    /**
     * Clears the instance holding the S3 client. Typically only called from tests.
     */
    public static void destroyInstance() {
        instance = null;
    }

    /**
     * @param s3Url an S3 URL in the form <pre>s3://bucket_name/key_name</pre>
     * @return the contents of the file
     */
    public String readSpecFromS3(String s3Url) {
        try {
            final String bucketName = s3Url.substring(5, s3Url.indexOf("/", 5));
            final String keyName = s3Url.substring(bucketName.length() + 6);

            final String specData;
            final S3Object obj = s3client.getObject(bucketName, keyName);
            try (final S3ObjectInputStream s3is = obj.getObjectContent()) {
                specData = new BufferedReader(new InputStreamReader(s3is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
            LOGGER.debug("Specification read [{} bytes] from S3: {}", specData.length(), s3Url);
            return specData;

        } catch (Exception e) {
            throw new RuntimeException(String.format("Error fetching specification from S3: %s", s3Url), e);
        }
    }
}
