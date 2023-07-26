package in.natelev.daikondiffvictimpolluter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import daikon.PptTopLevel.PptType;

import static in.natelev.daikondiffvictimpolluter.Colors.*;

public class DiffedInvs {
    private String pptName;
    // these are all guaranteed to have the same first variable & the same PPT
    private List<ReducedInvariant> pvInvs = new ArrayList<>();
    private List<ReducedInvariant> victimInvs = new ArrayList<>();

    DiffedInvs(String pptName) {
        this.pptName = pptName;
    }

    public boolean hasBoth() {
        return pvInvs.size() > 0 && victimInvs.size() > 0;
    }

    public int totalSize() {
        return pvInvs.size() + victimInvs.size();
    }

    public List<String> findMethodsOfPossibleRootCause(ReducedPptMap polluterVictim, ReducedPptMap polluter,
            ReducedPptMap victim) {
        ArrayList<String> methods = new ArrayList<>();

        // * technique 1: search for a pvInv that is in the polluter
        for (ReducedPpt ppt : polluter.pptIterable()) {
            // FIXME: we should allow :::ENTER as well, but that is currently stripped in
            // the reducing phase
            if (ppt.getType() != PptType.EXIT)
                continue;

            for (ReducedInvariant inv : ppt.getInvariants()) {
                for (ReducedInvariant polluterInv : pvInvs) {
                    // ignore params, as matching params are unlikely to be the actual root cause
                    if (polluterInv.firstVar().startsWith("p("))
                        continue;

                    if (inv.toString().equals(polluterInv.toString())) {
                        methods.add(ppt.prettyName() + "\n  " + BLUE + "\u001B[2m\u2B91  " + RESET + "\u001B[2m"
                                + inv.toString()
                                + "\u001B[22m" + RESET);
                    }
                }
            }
        }

        return methods;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(YELLOW + pptName + RESET + "\n");
        for (ReducedInvariant inv : pvInvs) {
            builder.append(RED + "    pv> " + RESET + inv.getUniquelyHadIfNeeded(victimInvs) + "\n");
        }
        for (ReducedInvariant inv : victimInvs) {
            builder.append(GREEN + "    .v> " + RESET + inv.getUniquelyHadIfNeeded(pvInvs) + "\n");
        }
        return builder.toString();
    }

    public static class DiffedInvsByVarMap {
        private HashMap<String, DiffedInvs> map = new HashMap<>();

        public Collection<DiffedInvs> values() {
            return map.values();
        }

        public void put(String key, ReducedInvariant invariant, boolean isPv, String pptName) {
            map.compute(key, (_k, value) -> {
                if (value == null) {
                    value = new DiffedInvs(pptName);
                }

                if (isPv) {
                    value.pvInvs.add(invariant);
                } else {
                    value.victimInvs.add(invariant);
                }

                return value;
            });
        }
    }
}
