package fi.mystes.esbdoc;

import java.util.ArrayList;

/**
 * Created by Vemma on 6.7.2016.
 */
public class ArtifactValidator {
    public static void validateDescription(ArtifactMap artifactMap) {

        for (Artifact artifact : artifactMap.getArtifacts()) {
            if (!artifact.getType().is(ArtifactType.PROXY ) && !artifact.getType().is(ArtifactType.SEQUENCE)) {
                continue;
            }

            if (!artifact.isDescriptionDefined()) {
                throw new EsbDocException("The artifact " + artifact.getName() + " type " + artifact.getType() +
                    " on the path " + artifact.getCarPath() + " doesn't contain the description.");
            }
        }

        return;
    }
}
