package in.natelev.daikondiffvictimpolluter;

import daikon.*;
import java.io.File;
import java.util.List;
import static in.natelev.daikondiffvictimpolluter.Colors.*;

public class DaikonDiffVictimPolluter {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println(
                    "Usage: DaikonDiffVictimPolluter daikon-pv.inv daikon-victim.inv daikon-polluter.inv (--debug)");
            System.exit(1);
        }

        ReducedPptMap polluterVictim = getPptMap(new File(args[0]));
        ReducedPptMap victim = getPptMap(new File(args[1]));
        ReducedPptMap polluter = getPptMap(new File(args[2]));
        boolean debug = args.length > 3 && args[3].equals("--debug");

        if (debug) {
            String debugPrelude = "\n\n\n" + RED + "!!! DEBUG: " + RESET;
            System.out.println(debugPrelude + "Polluter+Victim\n" + polluterVictim);
            System.out.println(debugPrelude + "Polluter\n" + polluter);
            System.out.println(debugPrelude + "Victim\n" + victim);
        }

        ReducedPptMap pvMinusP = allPptsinPVButNotOnlyInP(polluterVictim, polluter,
                victim);

        diff(pvMinusP, victim);
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

    private static void diff(ReducedPptMap pvMinusP, ReducedPptMap victim) {
        for (ReducedPpt ppt : pvMinusP.pptIterable()) {
            StringBuilder diffBuilder = new StringBuilder();
            ReducedPpt victimPpt = victim.map.get(ppt.name);

            // * heuristics
            if (victimPpt == null) {
                continue;
            }

            List<String> invariants = ppt.getInvariants();
            List<String> victimInvariants = victimPpt.getInvariants();

            invariants.removeIf((invariant) -> victimInvariants.contains(invariant));
            victimInvariants.removeIf((invariant) -> invariants.contains(invariant));

            for (String invariant : invariants) {
                diffBuilder.append(
                        RED + "p+v> " + RESET + invariant + " " + RED + "(polluter+victim only)" +
                                RESET + "\n");
            }

            for (String invariant : victimInvariants) {
                diffBuilder
                        .append(GREEN + " v> " + RESET + invariant + " " + GREEN + "(victim only)" +
                                RESET + "\n");
            }

            String builtDiff = diffBuilder.toString();
            if (builtDiff.length() > 0) {
                System.out.println(
                        BLUE +
                                "==========================================================================="
                                + RESET);
                System.out.println(YELLOW + ppt.name + RESET);
                System.out.println(builtDiff);
            }
        }
    }
}
