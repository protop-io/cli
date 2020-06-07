package io.protop.core.link;

import io.protop.core.logs.Logger;
import io.protop.core.manifest.Coordinate;
import io.protop.core.manifest.Manifest;
import io.protop.core.publish.PublishableProject;
import io.protop.core.storage.Storage;
import io.protop.core.storage.StorageUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LinkService {

    Logger logger = Logger.getLogger(LinkService.class);

    public void link(PublishableProject project) {
        try {
            Coordinate coordinate = getCoordinate(project);
            Path linkPath = getPathToLink(coordinate);
            Files.deleteIfExists(linkPath);
            Files.createSymbolicLink(linkPath, project.getProjectLocation());
        } catch (IOException e) {
            String message = "Failed to link project.";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public void unlink(PublishableProject project) {
        try {
            Coordinate coordinate = getCoordinate(project);
            Path linkPath = getPathToLink(coordinate);
            Files.deleteIfExists(linkPath);
        } catch (IOException e) {
            String message = "Failed to unlink project.";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public Completable clean() {
        return Completable.create(emitter -> {
            LinkedProjectsMap.load().subscribe(map -> {
                map.getProjects().forEach(((coordinate, path) -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.error("Failed to delete " + path, e);
                        emitter.onError(e);
                    }
                }));
                emitter.onComplete();
            }).dispose();
        });
    }

    /**
     *
     * @return a list of details about locally linked projects
     */
    public Observable<LinkDetail> list() {
        return Observable.create(emitter -> {
            LinkedProjectsMap.load().subscribe(map -> {
                map.getProjects().forEach(((coordinate, path) -> {
                    try {
                        emitter.onNext(new LinkDetail(coordinate, path.toRealPath()));
                    } catch (IOException e) {
                        emitter.onError(e);
                    }
                }));
                emitter.onComplete();
            }).dispose();
        });
    }

    private Path getPathToLink(Coordinate coordinate) throws IOException {
        Path linksDirectory = getLinksDirectory();

        Path orgPath = linksDirectory.resolve(coordinate.getOrganizationId());
        StorageUtils.createDirectoryIfNotExists(orgPath);

        return orgPath.resolve(coordinate.getProjectId());
    }

    private Coordinate getCoordinate(PublishableProject project) {
        Manifest manifest = project.getManifest();
        return new Coordinate(manifest.getOrganization(), manifest.getName());
    }

    private Path getLinksDirectory() {
        return Storage.pathOf(Storage.GlobalDirectory.LINKS);
    }
}
