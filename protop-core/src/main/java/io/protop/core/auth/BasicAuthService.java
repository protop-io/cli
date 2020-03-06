package io.protop.core.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.protop.api.auth.AuthTokenResponse;
import io.protop.core.Environment;
import io.protop.utils.HttpUtils;
import io.protop.core.error.ServiceError;
import io.protop.core.error.ServiceException;
import io.protop.core.logs.Logger;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageService;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

public class BasicAuthService implements AuthService<BasicCredentials> {

    private static final Logger logger = Logger.getLogger(BasicAuthService.class);

    private static final String CREDENTIAL_STORE_FILE_NAME = ".auth.json";

    private final StorageService storageService;
    private final Path storageFilePath;

    public BasicAuthService(StorageService storageService) {
        this.storageService = storageService;
        this.storageFilePath = Storage.pathOf(Storage.GlobalDirectory.SESSION_STORE)
                .resolve(CREDENTIAL_STORE_FILE_NAME);
    }

    @Override
    public Completable authorize(BasicCredentials credentials) {
        return Completable.create(emitter -> {
            CredentialStore credentialStore = loadCredentialStore();

            ObjectMapper objectMapper = Environment.getInstance().getObjectMapper();

            HttpClient client = HttpUtils.createHttpClient();

            String body = objectMapper.writeValueAsString(credentials);
            HttpPut put = new HttpPut(createLoginUri(credentials));
            put.setEntity(new StringEntity(body));

            HttpResponse response = client.execute(put);
            String entity = EntityUtils.toString(response.getEntity());

            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
                AuthTokenResponse authTokenResponse = objectMapper.readValue(
                        entity, AuthTokenResponse.class);
                credentialStore.add(AuthToken.builder()
                        .registry(credentials.getRegistry())
                        .value(authTokenResponse.getToken())
                        .build());
                storageService.storeJson(credentialStore, storageFilePath);
                emitter.onComplete();
            } else {
                // TODO handle better
                logger.info("Not OK status: " + status);
                logger.info("Not OK response: " + entity);
                emitter.onError(new ServiceException(ServiceError.AUTH_FAILED));
            }
        });
    }

    @Override
    public Maybe<AuthToken> getStoredToken(URI registry) {
        return Maybe.fromCallable(() -> {
            CredentialStore credentialStore = loadCredentialStore();
            return credentialStore.get(registry).orElse(null);
        });
    }

    @Override
    public Completable forget(URI registry) {
        // TODO
        return Completable.complete();
    }

    private CredentialStore loadCredentialStore() {
        return storageService.loadResource(storageFilePath, CredentialStore.class)
                .blockingGet(new CredentialStore());
    }

    private URI createLoginUri(BasicCredentials credentials) throws URISyntaxException {
        return new URIBuilder(credentials.getRegistry())
                .setPath(credentials.getRegistry().getPath()
                        +  "/-/user/org.couchdb.user:" + credentials.getUsername())
                .build();
    }
}
