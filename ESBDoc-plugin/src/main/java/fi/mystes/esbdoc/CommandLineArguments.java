package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by mystes-am on 28.5.2015.
 */
public class CommandLineArguments {
    private static Log log = LogFactory.getLog(CommandLineArguments.class);

    private static CommandLineArguments instance = null;

    private final String[] args;
    private boolean validateArtifacts;
    private String commaSeparatedListOfCarFilenames = "";
    private String commonPartOfOutputFilename = "";
    private String commaSeparatedListOfSoapUiFolderNames = "";

    private static final String USAGE_HELP = "Usage: java -jar CarAnalyzer.jar [validateArtifacts] [carFiles] [outputFile] [soapUIFiles]\n"
            + "  [validateArtifacts]: true if artifacts will be validated otherwise false\n"
            + "  [carFiles]: comma-separated list of car file names\n"
            + "  [outputFile]: full name of the output file WITHOUT extension.\n"
            + "                Two files will be created, one with a .txt extension and another with a .json extension.\n"
            + "  [soapUIFolders]: comma-separated list of SoapUI folder names. (Optional argument)";

    private CommandLineArguments(String[] args){
        this.args = args;

        if(areNotOk()){
            printUsage();
            return;
        }

        validateArtifacts = Boolean.parseBoolean(args[0]);
        commaSeparatedListOfCarFilenames = args[1];
        commonPartOfOutputFilename = args[2];
        if(args.length > 3) {
            commaSeparatedListOfSoapUiFolderNames = args[3];
        }
    }

    private static void printUsage(){
        System.out.println(USAGE_HELP);
    }

    public static CommandLineArguments from(String[] args){
        instance = new CommandLineArguments(args);
        return instance;
    }

    public boolean areNotOk(){
        return !areOk();
    }

    private boolean areOk(){
        if(null == args){
            return false;
        }

        if(args.length < 3){
            return false;
        }

        if(args.length > 3){
            return false;
        }

        if(StringUtils.isBlank(args[0])){
            return false;
        }

        if(StringUtils.isBlank(args[1])){
            return false;
        }

        return true;
    }

    public static boolean isValidateArtifacts() {
        return instance.validateArtifacts;
    }

    public static String getCommaSeparatedListOfCarFilenames(){
        return instance.commaSeparatedListOfCarFilenames;
    }

    public static String getCommonPartOfOutputFilename(){
        return instance.commonPartOfOutputFilename;
    }

    public static String getCommaSeparatedListOfSoapUiFolderNames(){
        return instance.commaSeparatedListOfSoapUiFolderNames;
    }

    public static boolean containSoapUiFolderNames(){
        return StringUtils.isNotBlank(getCommaSeparatedListOfSoapUiFolderNames());
    }
}
