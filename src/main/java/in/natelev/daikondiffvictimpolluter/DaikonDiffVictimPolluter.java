package in.natelev.daikondiffvictimpolluter;

import daikon.*;
import daikon.inv.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class DaikonDiffVictimPolluter {
    private static String RESET = "";
    private static String RED = "";
    private static String YELLOW = "";
    private static String GREEN = "";
    private static String BLUE = "";
    private static String CYAN = "";

    // to allow for redirection to a file
    static {
        if (System.console() != null) {
            RESET = "\u001B[0m";
            RED = "\u001B[31m";
            YELLOW = "\u001B[33m";
            GREEN = "\u001B[32m";
            BLUE = "\u001B[34m";
            CYAN = "\u001B[36m";
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println(
                    "Usage: DaikonDiffVictimPolluter daikon-pv.inv daikon-victim.inv daikon-polluter.inv (--debug)");
            System.exit(1);
        }

        PptMap polluterVictim = getPptMap(new File(args[0]));
        PptMap victim = getPptMap(new File(args[1]));
        PptMap polluter = getPptMap(new File(args[2]));
        boolean debug = args.length > 3 && args[3].equals("--debug");

        if (debug) {
            String debugPrelude = "\n\n\n" + RED + "!!! DEBUG: " + RESET;
            System.out.println(debugPrelude + "Polluter+Victim");
            debugPrintPptMap(polluterVictim);
            System.out.println(debugPrelude + "Polluter");
            debugPrintPptMap(polluter);
            System.out.println(debugPrelude + "Victim\n\n\n");
            debugPrintPptMap(victim);
        }

        HashMap<String, PptTopLevel> pvMinusP = allPptsinPVButNotInP(polluterVictim, polluter);

        diff(pvMinusP, victim);
    }

    private static PptMap getPptMap(File file) {
        try {
            return FileIO.read_serialized_pptmap(file, true);
        } catch (Throwable e) {
            System.err.println("Error reading serialized pptmap");
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    private static HashMap<String, PptTopLevel> allPptsinPVButNotInP(PptMap polluterVictim, PptMap polluter) {
        HashMap<String, PptTopLevel> pvMinusP = new HashMap<>();
        for (PptTopLevel pptTopLevel : polluterVictim.pptIterable()) {
            if (!polluter.containsName(pptTopLevel.name)) {
                pvMinusP.put(pptTopLevel.name, pptTopLevel);
            }
        }
        return pvMinusP;
    }

    private static void diff(HashMap<String, PptTopLevel> pvMinusP, PptMap victim) {
        HashSet<String> alreadyIteratedOver = new HashSet<>();

        for (PptTopLevel pptTopLevel : pvMinusP.values()) {
            if (pptTopLevel.num_samples() == 0)
                continue;

            alreadyIteratedOver.add(pptTopLevel.name);

            StringBuilder diffBuilder = new StringBuilder();
            PptTopLevel victimPptTopLevel = victim.get(pptTopLevel.name);

            List<String> invariants = getStringifiedInvariantsOf(pptTopLevel);
            List<String> victimInvariants = getStringifiedInvariantsOf(victimPptTopLevel);

            invariants.removeIf((invariant) -> victimInvariants.contains(invariant));
            victimInvariants.removeIf((invariant) -> invariants.contains(invariant));

            for (String invariant : invariants) {
                diffBuilder.append(
                        RED + "p+v> " + RESET + invariant + " " + RED + "(polluter+victim only)" + RESET + "\n");
            }

            if (victimPptTopLevel == null) {
                System.out.println(
                        RED + "===========================================================================" + RESET);
                System.out.println(YELLOW + pptTopLevel.name() + RESET);
                System.out.println(RED + "Only found when polluter was present" + RESET);
                System.out.print(diffBuilder.toString());
                continue;
            }

            for (String invariant : victimInvariants) {
                diffBuilder
                        .append(GREEN + "  v> " + RESET + invariant + " " + GREEN + "(victim only)" + RESET + "\n");
            }

            String builtDiff = diffBuilder.toString();
            if (builtDiff.length() > 0) {
                System.out.println(
                        BLUE + "===========================================================================" + RESET);
                System.out.println(YELLOW + pptTopLevel.name() + RESET);
                System.out.println(builtDiff);
            }
        }

        for (PptTopLevel pptTopLevel : victim.pptIterable()) {
            if (pptTopLevel.num_samples() == 0 || alreadyIteratedOver.contains(pptTopLevel.name))
                continue;

            StringBuilder diffBuilder = new StringBuilder();
            PptTopLevel pvMinusPPptTopLevel = pvMinusP.get(pptTopLevel.name);

            List<String> invariants = getStringifiedInvariantsOf(pptTopLevel);
            List<String> pvMinusPInvariants = getStringifiedInvariantsOf(pvMinusPPptTopLevel);

            invariants.removeIf((invariant) -> pvMinusPInvariants.contains(invariant));
            pvMinusPInvariants.removeIf((invariant) -> invariants.contains(invariant));

            for (String invariant : invariants) {
                diffBuilder
                        .append(GREEN + "  v> " + RESET + invariant + " " + GREEN + "(victim only)" + RESET + "\n");
            }

            if (pvMinusPPptTopLevel == null) {
                System.out.println(
                        GREEN + "===========================================================================" + RESET);
                System.out.println(YELLOW + pptTopLevel.name() + RESET);
                System.out.println(GREEN + "Only found when polluter was NOT present" + RESET);
                System.out.print(diffBuilder.toString());
                continue;
            }

            for (String invariant : pvMinusPInvariants) {
                diffBuilder.append(
                        RED + "p+v> " + RESET + invariant + " " + RED + "(polluter+victim only)" + RESET + "\n");
            }

            String builtDiff = diffBuilder.toString();
            if (builtDiff.length() > 0) {
                System.out.println(
                        BLUE + "===========================================================================" + RESET);
                System.out.println(YELLOW + pptTopLevel.name() + RESET);
                System.out.println(builtDiff);
            }
        }
    }

    private static List<String> getStringifiedInvariantsOf(PptTopLevel pptTopLevel) {
        if (pptTopLevel == null)
            return new ArrayList<>();
        return pptTopLevel.getInvariants().stream()
                .filter((invariant) -> invariant.isWorthPrinting()).map((invariant) -> invariant.toString()).sorted()
                .collect(Collectors.toList());
    }

    private static void debugPrintPptMap(PptMap pptMap) {
        for (PptTopLevel pptTopLevel : pptMap.pptIterable()) {
            debugPrintPptTopLevel(pptTopLevel);
        }
    }

    private static void debugPrintPptTopLevel(PptTopLevel pptTopLevel) {
        List<Invariant> invariants = pptTopLevel.getInvariants().stream()
                .filter((invariant) -> invariant.isWorthPrinting()).collect(Collectors.toList());

        if (invariants.size() == 0)
            return;
        System.out.println(
                BLUE + "===========================================================================" + RESET);
        System.out.println(YELLOW + pptTopLevel.name() + RESET + "  \t" + CYAN + "vars: "
                + pptTopLevel.var_names() + RESET);
        for (Invariant invariant : invariants) {
            System.out.println(invariant);
        }
    }
}
