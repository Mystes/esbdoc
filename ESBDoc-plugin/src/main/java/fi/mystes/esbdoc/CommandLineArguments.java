package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 28.5.2015.
 */
public class CommandLineArguments {

    public static final String USAGE_HELP = "Usage: java -jar CarAnalyzer.jar [carFiles] [outputFile] [soapUIFiles]\n"
            + "  [carFiles]: comma-separated list of car file names\n"
            + "  [outputFile]: full name of the output file WITHOUT extension.\n"
            + "                Two files will be created, one with a .txt extension and another with a .json extension.\n"
            + "  [soapUIFolders]: comma-separated list of SoapUI folder names. (Optional argument)";
}
