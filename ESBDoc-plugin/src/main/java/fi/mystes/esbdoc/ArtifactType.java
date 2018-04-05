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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mystes-am on 29.5.2015.
 */
public enum ArtifactType {

    PROXY("synapse/proxy-service", "proxy"),
    SEQUENCE("synapse/sequence", "sequence"),
    ENDPOINT("synapse/endpoint", "endpoint"),
    API("synapse/api", "api"),
    EXTERNAL_API("external", "api"),
    RESOURCE("registry/resource", "resource"),
    MESSAGE_PROCESSOR("synapse/message-processors", "messageProcessor"),
    MESSAGE_STORE("synapse/message-store", "messageStore"),
    TASK("synapse/task", "task"),
    DATASERVICE("service/dataservice", "dataservice");

    private static final Map<String, ArtifactType> artifactTypeMap;

    static {
        artifactTypeMap = new HashMap<String, ArtifactType>(6); //TODO why that 6 right there?

        for (ArtifactType at : ArtifactType.values()) {
            artifactTypeMap.put(at.synapseType, at);
        }
    }

    private final String synapseType;
    private final String typeString;

    ArtifactType(String synapseType, String typeString) {
        this.synapseType = synapseType;
        this.typeString = typeString;
    }

    public static ArtifactType correspondingTo(String typeString) {
        return artifactTypeMap.get(typeString);
    }

    @Override
    public String toString() {
        return typeString;
    }

    public boolean is(ArtifactType that){
        return this == that;
    }

    public boolean isNot(ArtifactType that){
        return !is(that);
    }
}
