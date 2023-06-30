package in.natelev.daikondiffvictimpolluter;

import daikon.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static in.natelev.daikondiffvictimpolluter.Colors.*;

public class DaikonDiffVictimPolluter {
    private static boolean debug;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println(
                    "Usage: DaikonDiffVictimPolluter daikon-pv.inv daikon-victim.inv daikon-polluter.inv (--debug)");
            System.exit(1);
        }

        debug = args.length > 3 && args[3].equals("--debug");

        printLoadMsg("Loading PptMaps...");
        ReducedPptMap polluterVictim = getPptMap(new File(args[0]));
        printLoadMsg("\rLoaded polluter+victim. Now loading victim...");
        ReducedPptMap victim = getPptMap(new File(args[1]));
        printLoadMsg("\rLoaded victim. Now loading polluter...       ");
        ReducedPptMap polluter = getPptMap(new File(args[2]));
        printLoadMsg("\rLoaded all PptMaps!                          \n\n");

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

    private static void printLoadMsg(String msg) {
        if (debug || System.console() != null) {
            System.out.print(msg);
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

    private static void diff(ReducedPptMap pvMinusP, ReducedPptMap victim) {
        for (ReducedPpt ppt : pvMinusP.pptIterable()) {
            ReducedPpt victimPpt = victim.map.get(ppt.name);

            // * heuristics
            if (victimPpt == null) {
                continue;
            }

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

            System.out.println(
                    BLUE +
                            "==========================================================================="
                            + RESET);
            System.out.println(YELLOW + ppt.name + RESET);

            for (ReducedInvariant invariant : invariants) {
                System.out.println(
                        RED + "p+v> " + RESET + invariant + " " + RED
                                + "(polluter+victim only)" +
                                RESET);
            }

            for (ReducedInvariant invariant : victimInvariants) {
                System.out.println(GREEN + "  v> " + RESET + invariant + " " + GREEN
                        + "(victim only)" +
                        RESET);
            }

            System.out.println();
        }
    }
}
