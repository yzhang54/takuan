package in.natelev.daikondiffvictimpolluter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import static in.natelev.daikondiffvictimpolluter.Colors.*;

public class Output {
    public static boolean debug;
    private static File file;
    private static PrintWriter outputWriter;
    private static boolean outputColorsToFile;

    public static void setOutputFile(String outputFile) throws IOException {
        file = new File(outputFile);
        file.createNewFile();
        outputWriter = new PrintWriter(outputFile, "utf-8");
    }

    public static void shutdown() {
        if (outputWriter != null) {
            log(GREEN + "\u2713 Succesfully wrote to " + file + "." + RESET);
            outputWriter.close();
        }
    }

    public static void log(String msg) {
        System.out.println(msg);
    }

    public static void output(String msg) {
        if (outputWriter == null) {
            log(msg);
            return;
        }

        if (debug)
            log(msg);

        outputWriter.println(cleanMsgOfColorsIfNeeded(msg));
    }

    public static void outputIfFile(String msg) {
        if (outputWriter != null)
            outputWriter.println(cleanMsgOfColorsIfNeeded(msg));
    }

    private static String cleanMsgOfColorsIfNeeded(String msg) {
        if (outputColorsToFile) {
            return msg;
        } else {
            return msg.replaceAll("\u001B\\[[;\\d]*m", "");
        }
    }

    public static void setOutputColorsToFile(boolean should) {
        outputColorsToFile = should;
    }
}
