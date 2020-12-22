package io.protop.cli;

import io.protop.cli.errors.ExceptionHandler;
import io.protop.core.Context;
import io.protop.core.RuntimeConfiguration;
import io.protop.core.auth.AuthService;
import io.protop.core.grpc.GrpcService;
import io.protop.core.logs.Logger;
import io.protop.core.logs.Logs;
import io.protop.core.publish.ProjectPublisher;
import io.protop.core.publish.ProjectPublisherImpl;
import io.protop.core.publish.PublishableProject;
import io.protop.core.storage.StorageService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;

@Command(name = "publish",
        description = "(experimental) Publish project to the registry.")
public class Publish implements Runnable {

    private static final Logger logger = Logger.getLogger(Publish.class);

    @ParentCommand
    private ProtopCli protop;

    @Parameters(arity = "0..1",
            description = "Location of project",
            defaultValue = ".")
    private Path location;

    @Option(names = {"-r", "--registry"},
            description = "Registry",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String registry;

    @Option(names = {"-u", "--username"},
            description = "Username",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String username;

    @Option(names = {"-p", "--password"},
            description = "Password",
            required = false,
            arity = "0..1",
            defaultValue = "")
    private String password;

    @Override
    public void run() {
        Logs.enableIf(protop.isDebugMode());
        new ExceptionHandler().run(() -> {
            RuntimeConfiguration cliRc = RuntimeConfiguration.builder()
                    .repositoryUrl(registry)
                    .username(username)
                    .password(password)
                    .build();
            Context context = Context.from(location, cliRc);
            StorageService storageService = new StorageService();
            GrpcService grpcService = new GrpcService();
            AuthService authService = new AuthService(storageService, grpcService, context);
            ProjectPublisher projectPublisher = new ProjectPublisherImpl(context, authService, grpcService);

            PublishableProject project = PublishableProject.from(location);

            projectPublisher.publish(project).blockingAwait();
            handleSuccess();
        });
    }

    private void handleSuccess() {
        logger.always("Published!");
    }
}
