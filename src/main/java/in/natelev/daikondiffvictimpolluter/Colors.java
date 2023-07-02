package in.natelev.daikondiffvictimpolluter;

public class Colors {
    public static String RESET = "";
    public static String RED = "";
    public static String YELLOW = "";
    public static String GREEN = "";
    public static String BLUE = "";
    public static String CYAN = "";
    public static String MAGENTA = "";

    // to allow for redirection to a file
    static {
        if (System.console() != null) {
            RESET = "\u001B[0m";
            RED = "\u001B[31m";
            YELLOW = "\u001B[33m";
            GREEN = "\u001B[32m";
            BLUE = "\u001B[34m";
            CYAN = "\u001B[36m";
            MAGENTA = "\u001B[35m";
        }
    }
}
