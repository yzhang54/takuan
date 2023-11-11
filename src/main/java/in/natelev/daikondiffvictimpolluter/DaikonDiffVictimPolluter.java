package in.natelev.daikondiffvictimpolluter;

import daikon.*;
import daikon.PptTopLevel.PptType;
import in.natelev.daikondiffvictimpolluter.DiffedInvs.DiffedInvsByVarMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static in.natelev.daikondiffvictimpolluter.Colors.*;
import static in.natelev.daikondiffvictimpolluter.Output.*;

public class DaikonDiffVictimPolluter {
    private static int MAX_RANKED_OUTPUT_INVARIANTS = 5;
    private static File problemInvariantsOutputFile;

    public static void main(String[] args) {
        if (args.length < 3) {
            printUsage();
        }

        if (args.length > 3) {
            boolean lookingForOutput = false;
            boolean lookingForProblemInvariantsOutput = false;
            for (String arg : Arrays.asList(args).subList(3, args.length)) {
                if (lookingForOutput) {
                    lookingForOutput = false;
                    try {
                        Output.setOutputFile(arg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                } else if (arg.equals("-o")) {
                    lookingForOutput = true;
                } else if (lookingForProblemInvariantsOutput) {
                    lookingForProblemInvariantsOutput = false;
                    problemInvariantsOutputFile = new File(arg);
                } else if (arg.equals("--problem-invariants-output")) {
                    lookingForProblemInvariantsOutput = true;
                } else if (arg.equals("-h")) {
                    printUsage();
                } else if (arg.equals("--debug")) {
                    debug = true;
                    log(GREEN + "Debug enabled." + RESET);
                } else if (arg.equals("--output-colors-to-file")) {
                    log(GREEN + "Will send colors to file if possible" + RESET);
                    Output.setOutputColorsToFile(true);
                } else {
                    System.err.println(RED + "Unknown option " + arg + RESET);
                    System.exit(1);
                }
            }
        }

        log(MAGENTA + "Loading PptMaps..." + RESET);
        ReducedPptMap polluterVictim = getPptMap(new File(args[0]));
        log(BLUE + "Loaded polluter+victim. Now loading victim..." + RESET);
        ReducedPptMap victim = getPptMap(new File(args[1]));
        log(CYAN + "Loaded victim. Now loading polluter..." + RESET);
        ReducedPptMap polluter = getPptMap(new File(args[2]));
        log(GREEN + "\rLoaded all PptMaps!\n" + RESET);

        ReducedPptMap pvMinusP = allPptsinPVButNotOnlyInP(polluterVictim, polluter,
                victim);

        if (debug) {
            String debugPrelude = "\n\n\n" + RED + "!!! DEBUG: " + RESET;
            log(debugPrelude + "Polluter+Victim\n" + polluterVictim);
            log(debugPrelude + "Polluter\n" + polluter);
            log(debugPrelude + "Victim\n" + victim);
            log(debugPrelude + "PV-P\n" + pvMinusP);
        }

        log(BLUE + "Diffing..." + RESET);
        ArrayList<DiffedInvs> rankedDiffedInvs = diffAndRank(pvMinusP, victim);
        log(GREEN + "Finished diffing." + RESET);

        if (rankedDiffedInvs.size() == 0) {
            log(RED + "No problem invariants found using current heuristics." + RESET);
            System.exit(1);
        } else {
            log("Ppts of problem invs: " + RESET + String.join(", ",
                    rankedDiffedInvs.stream().map((diffedInvs) -> diffedInvs.getPptName())
                            .collect(Collectors.toList())));
        }

        if (problemInvariantsOutputFile != null) {
            try (PrintWriter problemInvariantsOutputWriter = new PrintWriter(problemInvariantsOutputFile)) {
                for (DiffedInvs diffedInvs : rankedDiffedInvs) {
                    problemInvariantsOutputWriter.println(diffedInvs.toCSV());
                }
                problemInvariantsOutputWriter.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
            log(BLUE + "Outputted problem invariants to " + problemInvariantsOutputFile.getPath() + RESET);
        }

        Output.shutdown();
    }

    private static void printUsage() {
        System.err.println(
                RED + "Usage: DaikonDiffVictimPolluter daikon-pv.inv daikon-victim.inv daikon-polluter.inv (-o <output.dinv>) (--debug) (--output-colors-to-file) (--cleaner-finder-output-file file.csv)"
                        + RESET);
        System.exit(1);
    }

    // currently unused, could be called from inside `main`
    @SuppressWarnings("unused")
    private static void findRootCause(ArrayList<DiffedInvs> rankedDiffedInvs, ReducedPptMap polluterVictim,
            ReducedPptMap polluter, ReducedPptMap victim) {
        log("Now attempting to find root cause methods...");

        ArrayList<String> possibleRootCauseMethods = new ArrayList<>();
        if (rankedDiffedInvs.size() == 0) {
            output(RED + "ERR: No invariants differed using current heuristics.");
        } else {
            for (DiffedInvs diffedInvs : rankedDiffedInvs) {
                possibleRootCauseMethods
                        .addAll(diffedInvs.findMethodsOfPossibleRootCause(polluterVictim, polluter, victim).stream()
                                .map((tup) -> tup[0] + "\n  " + BLUE + "\u001B[2m\u2B91  " + RESET + "\u001B[2m"
                                        + tup[1].toString()
                                        + "\u001B[22m" + RESET)
                                .collect(Collectors.toList()));
            }
        }

        if (possibleRootCauseMethods.size() > 0) {
            log(GREEN + "Possible root cause methods found:\n" + RESET + String.join("\n",
                    possibleRootCauseMethods.stream().map((s) -> BLUE + "* " + RESET + s)
                            .collect(Collectors.toList()))
                    + "\n");
        } else {
            log(RED + "No possible root cause methods found.\n" + RESET);
        }
    }

    private static ReducedPptMap getPptMap(File file) {
        try {
            return new ReducedPptMap(FileIO.read_serialized_pptmap(file, true));
        } catch (Throwable e) {
            System.err.println("Error reading serialized pptmap");
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    private static ReducedPptMap allPptsinPVButNotOnlyInP(ReducedPptMap polluterVictim, ReducedPptMap polluter,
            ReducedPptMap victim) {
        ReducedPptMap pvMinusP = new ReducedPptMap();
        for (ReducedPpt ppt : polluterVictim.pptIterable()) {
            boolean inPolluter = polluter.containsName(ppt.name);
            boolean inVictim = victim.containsName(ppt.name);

            if (!inPolluter || inVictim) {
                pvMinusP.map.put(ppt.name, ppt);
            }
        }
        return pvMinusP;
    }

    private static ArrayList<DiffedInvs> diffAndRank(ReducedPptMap pvMinusP, ReducedPptMap victim) {
        StringBuilder diffBuilder = new StringBuilder();

        ArrayList<DiffedInvs> rankedOutputInvariants = new ArrayList<>();

        for (ReducedPpt ppt : pvMinusP.pptIterable()) {
            if (ppt.getType().equals(PptType.ENTER))
                continue;

            ReducedPpt victimPpt = victim.map.get(ppt.name);

            if (victimPpt == null) {
                continue;
            }

            boolean continueRankingInvariants = rankedOutputInvariants.size() < MAX_RANKED_OUTPUT_INVARIANTS;
            DiffedInvsByVarMap invariantsByVariable = continueRankingInvariants ? new DiffedInvsByVarMap()
                    : null;
            List<ReducedInvariant> originalInvariants = ppt.getInvariants();
            List<ReducedInvariant> victimInvariants = victimPpt.getInvariants();

            // remove duplicates
            List<ReducedInvariant> invariants = originalInvariants.stream()
                    .filter((invariant) -> !victimInvariants.contains(invariant)).collect(Collectors.toList());
            victimInvariants.removeIf((invariant) -> originalInvariants.contains(invariant));

            // remove all invariants without a similar invariant in the other run
            invariants.removeIf(
                    (invariant) -> !victimInvariants.stream()
                            .anyMatch((other) -> invariant.hasSameFirstVariableAs(other)));
            victimInvariants.removeIf(
                    (invariant) -> !invariants.stream().anyMatch((other) -> invariant.hasSameFirstVariableAs(other)));

            if (invariants.size() == 0 || victimInvariants.size() == 0)
                continue;

            diffBuilder.append(
                    BLUE +
                            "==========================================================================="
                            + RESET + "\n");
            diffBuilder.append(ppt.prettyName() + "\n");

            for (ReducedInvariant invariant : invariants) {
                if (invariantsByVariable != null && !invariant.getType().contains("NonEqual")
                        && invariant.firstVar() != null)
                    invariantsByVariable.put(invariant.firstVar(), invariant, true, ppt.prettyName());
                diffBuilder.append(
                        RED + "p+v> " + RESET + invariant + " " + RED
                                + "(polluter+victim only)" +
                                RESET + "\n");
            }

            for (ReducedInvariant invariant : victimInvariants) {
                if (invariantsByVariable != null && !invariant.getType().contains("NonEqual")
                        && invariant.firstVar() != null)
                    invariantsByVariable.put(invariant.firstVar(), invariant, false, ppt.prettyName());
                diffBuilder.append(GREEN + "  v> " + RESET + invariant + " " + GREEN
                        + "(victim only)" +
                        RESET + "\n");
            }

            if (continueRankingInvariants) {
                rankedOutputInvariants.addAll(invariantsByVariable.values().stream()
                        .filter((diffedInvs) -> diffedInvs.hasBoth())
                        .sorted((a, b) -> a.totalSize() - b.totalSize())
                        .limit(MAX_RANKED_OUTPUT_INVARIANTS - rankedOutputInvariants.size())
                        .collect(Collectors.toList()));
            }

            diffBuilder.append("\n");
        }

        output(String.join("\n", rankedOutputInvariants.stream()
                .map(diffedInvs -> diffedInvs.toString())
                .collect(Collectors.toList())));

        outputIfFile("\n");
        outputIfFile(diffBuilder.toString());

        return rankedOutputInvariants;
    }
}
