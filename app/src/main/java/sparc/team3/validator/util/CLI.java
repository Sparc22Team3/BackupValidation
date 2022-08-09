package sparc.team3.validator.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Formatter;

/**
 * Wrappers for output to command line and user input
 */
public class CLI {
    BufferedReader inputReader;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";

    private final String yesOrNoColor = ANSI_GREEN;
    public CLI() {
        inputReader = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Print prompt and wait for input from user
     * @param format String format to pass to Formatter
     * @param args Objects to pass to Formatter to add to format
     * @return String of selection
     * @throws IOException if there is an IO error
     */
    public synchronized String prompt(String format, Object ... args) throws IOException {
        out(format + " ", args);
        return inputReader.readLine();
    }

    /**
     *
     * @param format String format to pass to Formatter
     * @param color String of ANSI Color to make output.  Constants available in this class.
     * @param args Objects to pass to Formatter to add to format
     * @return String of selection
     * @throws IOException if there is an IO error
     */
    public synchronized String promptColor(String format, String color, Object ... args) throws IOException {
        return prompt(color + format + ANSI_RESET, args);
    }

    /**
     *
     * @param format String format to pass to Formatter
     * @param defaultValue String of default value.  Will be printed as [default value: String]
     * @param args Objects to pass to Formatter to add to format
     * @return String of selection
     * @throws IOException if there is an IO error
     */
    public synchronized String promptDefault(String format, String defaultValue, Object ... args) throws IOException {
        Object[] newArgs = Arrays.copyOf(args,args.length + 1);
        newArgs[newArgs.length - 1] = defaultValue;
        String result = prompt(format + " [default value: %s] ", newArgs);
        if(result.strip().equals(""))
            return defaultValue;
        return result;
    }

    /**
     *
     * @param format String format to pass to Formatter
     * @param args Objects to pass to Formatter to add to format
     * @return String of selection
     * @throws IOException if there is an IO error
     */
    public synchronized boolean promptYesOrNo(String format, Object ... args) throws IOException {
        String input = prompt(format + yesOrNoColor +" [yes|no] " + ANSI_RESET, args);
        return input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y");
    }

    /**
     *
     * @param format String format to pass to Formatter
     * @param color String of ANSI Color to make output.  Constants available in this class.
     * @param args Objects to pass to Formatter to add to format
     * @return String of selection
     * @throws IOException if there is an IO error
     */
    public synchronized boolean promptYesOrNoColor(String format, String color, Object ... args) throws IOException {
        return promptYesOrNo(color + format, args);
    }

    /**
     *
     * @param format String format to pass to Formatter
     * @param min int of minimum number to accept
     * @param max int of maximum number to accept
     * @param args Objects to pass to Formatter to add to format
     * @return String of selection
     * @throws IOException if there is an IO error
     */
    public synchronized int promptNumber(String format, int min, int max, Object...args) throws IOException {
        Object[] newArgs = Arrays.copyOf(args,args.length + 2);
        newArgs[newArgs.length - 2] = min;
        newArgs[newArgs.length - 1] = max;

        String number;
        int result = min - 1;
        while(result < min || result > max){
            number = prompt(format, newArgs);
            try {
                result = Integer.parseInt(number);
            } catch(NumberFormatException e){
                result = min -1;
                outColor("Please enter a number between %d and $d", ANSI_RED, min, max);
            }
        }

        return result;

    }

    /**
     *
     * @param format String format to pass to Formatter
     * @param args Objects to pass to Formatter to add to format
     */
    public synchronized void out(String format, Object ... args){
        System.out.print(new Formatter().format(format, args));
    }

    /**
     *
     * @param format String format to pass to Formatter
     * @param color String of ANSI Color to make output.  Constants available in this class.
     * @param args Objects to pass to Formatter to add to format
     */
    public synchronized void outColor(String format, String color, Object ... args){
        out(color + format + ANSI_RESET, args);
    }
}
