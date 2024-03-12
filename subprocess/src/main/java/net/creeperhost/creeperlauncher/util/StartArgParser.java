package net.creeperhost.creeperlauncher.util;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

public class StartArgParser {
    private String[] input;
    private HashMap<String, String> args = new HashMap<>();

    public static StartArgParser parse(String[] programArgs) {
        StartArgParser startArgParser = new StartArgParser(programArgs);
        startArgParser.parseInput();
        return startArgParser;
    }

    public StartArgParser(String[] programArgs) {
        this.input = programArgs;
    }

    private void parseInput() {
        String argName = null;

        for(String arg : this.input) {
            if (arg.isEmpty()) continue;
            if (arg.startsWith("--")) {
                argName = arg.substring(2);
                // Setup the arg in-case it has a value later
                this.args.put(argName, "");
            } else if (argName != null) {
                if (this.args.containsKey(argName)) {
                    this.args.put(argName, arg);
                }
                argName = null;
            } else {
                this.args.put(arg, "");
            }
        }
    }

    public ImmutableMap<String, String> getArgs() {
        return ImmutableMap.copyOf(this.args);
    }
}
