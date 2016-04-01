package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * Created by mystes-am on 8.2.2016.
 */
public class ArtifactUtil {
    private static Log log = LogFactory.getLog(ArtifactUtil.class);

    /**
     * Returns an artifact resolved from a URL or null if no Artifact could be
     * resolved.
     *
     * @param string
     * @return
     */
    public static Artifact getArtifactFromString(SortedMap<String, String> servicePathMap, ArtifactMap artifactMap, String string) {
        // The typical case: string is an artifact name
        Artifact dependency = artifactMap.get(string);

        if(null != dependency){
            return dependency;
        }

        if(null == string){
            return null; //TODO This should probably throw something.
        }

        URI uri = stringToUri(string);
        switch(Protocol.scheme(uri)){
            case MAILTO: case VFS: return null;
            case HTTP:case HTTPS: return getArtifactFromHttpUri(servicePathMap, artifactMap, uri);
            case GOV:case CONF: return getArtifactFromRegistyUri(servicePathMap, artifactMap, uri);
            case JMS: return getArtifactFromJmsUri(artifactMap, uri);
            default: return null;
        }
    }

    private static URI stringToUri(String string){
        if(StringUtils.isBlank(string)){
            return null;
        }
        String urifiedString = urifyString(string);
        try {
            return new URI(urifiedString);
        } catch (URISyntaxException e) {
            log.warn("Unparseable URI: " + urifiedString);
            return null;
        }
    }

    /**
     * Under Windows the Maven build sometimes creates rather ugly (and invalid)
     * file URIs.
     *
     * @return
     */
    private static String urifyString(String string) {
        // First replace any two subsequent backslashes with a single one
        string = string.replace("\\\\", "\\");
        // Then replace any backslashes with a slash
        return string.replace('\\', '/');
    }

    private static Artifact getArtifactFromRegistyUri(SortedMap<String, String> servicePathMap, ArtifactMap artifactMap, URI uri) {
        return getArtifactFromPath(servicePathMap, artifactMap, uri.getSchemeSpecificPart());
    }

    /**
     * Attemps to find a dependency by examining the URI path
     *
     * A URI of a proxy may take for instance the following forms:
     * http://localhost:9768/services/EventService.SOAP11Endpoint/
     * http://localhost:8280/services/MetadataLookup_queryProxy/GetAll
     *
     * JMS URIs look like:
     * jms:/TosUserToVleChangeQueueProxy?transport.jms.ConnectionFactoryJNDIName=QueueConnectionFactory&amp;java.naming.factory.initial=org.apache.activemq.jndi.ActiveMQInitialContextFactory&amp;transport.jms.DestinationType=queue&amp;java.naming.provider.url=tcp://localhost:61616
     * where the interesting bit is just the path part.
     *
     * @param uri
     * @return
     */
    private static Artifact getArtifactFromHttpUri(SortedMap<String, String> servicePathMap, ArtifactMap artifactMap, URI uri) {
        return getArtifactFromPath(servicePathMap, artifactMap, uri.getPath());
    }

    /**
     * Attemps to resolve an Artifact's name from a path String one path element
     * at a time
     *
     * @param path
     * @return
     */
    private static Artifact getArtifactFromPath(SortedMap<String, String> servicePathMap, ArtifactMap artifactMap, String path) {
        // path might be in the service path map
        if (servicePathMap.containsKey(path)) {
            String artifactName = servicePathMap.get(path);
            Artifact artifact = artifactMap.get(artifactName);
            if (artifact != null) {
                return artifact;
            }
        }

        return resolveArtifactBasedOnUri(artifactMap, path);
    }

    private static Artifact resolveArtifactBasedOnUri(ArtifactMap artifactMap, String path){
        String[] pathComponents = path.split("/");

        for (String pathComponent : pathComponents) {
            Artifact artifact = artifactMap.get(pathComponent);
            if(artifact != null){
                return artifact;
            }

            artifact = resolveArtifactBasedOnUriComponent(artifactMap, pathComponent);
            if(artifact != null){
                return artifact;
            }
        }

        return null;
    }

    private static Artifact resolveArtifactBasedOnUriComponent(ArtifactMap artifactMap, String pathComponent){
        List<String> names = getArtifactNameCandidates(pathComponent);
        for(String name : names){
            Artifact artifact = artifactMap.get(name);

            if (artifact != null) {
                return artifact;
            }
        }

        return null;
    }

    private static List<String> getArtifactNameCandidates(String pathComponent){
        List<String> names = new ArrayList<String>();

        for (String componentPart : pathComponent.split("\\.")) {
            if(names.size() == 0){
                names.add(componentPart);
            } else {
                String previous = names.get(names.size()-1);
                names.add(previous + "." + componentPart);
            }
        }

        return names;
    }

    private static Artifact getArtifactFromJmsUri(ArtifactMap artifactMap, URI uri) {
        String artifactNameCandidate = uri.getPath().replace("/", "");
        return artifactMap.get(artifactNameCandidate);
    }
}
