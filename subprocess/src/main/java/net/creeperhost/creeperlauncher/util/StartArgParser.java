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

        for(String arg : this.input)
        {
            if(arg.length() > 2) {
                if (arg.startsWith("--")) {
                    argName = arg.substring(2);
                    this.args.put(argName, "");
                }
                if (argName != null) {
                    if (argName.length() > 2) {
                        if (!argName.equals(arg.substring(2))) {
                            if (this.args.containsKey(argName)) {
                                this.args.remove(argName);
                            }
                            this.args.put(argName, arg);
                            argName = null;
                        }
                    }
                }
            }
        }
    }

    public ImmutableMap<String, String> getArgs() {
        return ImmutableMap.copyOf(this.args);
    }
}
