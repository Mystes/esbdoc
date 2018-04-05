/**
 * Copyright 2018 Mystes Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
