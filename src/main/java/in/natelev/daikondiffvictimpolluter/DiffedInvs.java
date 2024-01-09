package in.natelev.daikondiffvictimpolluter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<String[]> findMethodsOfPossibleRootCause(ReducedPptMap polluterVictim, ReducedPptMap polluter,
            ReducedPptMap victim) {
        ArrayList<String[]> methods = new ArrayList<>();

        // * technique 1: search for a pvInv that is in the polluter
        for (ReducedPpt ppt : polluter.pptIterable()) {
            if (ppt.getType() != PptType.EXIT)
                continue;
            ReducedPpt polluterEnterPpt = polluter.map.get(ppt.name.replace(":::EXIT", ":::ENTER"));
            if (polluterEnterPpt == null)
                continue;

            nextPolluterInv: for (ReducedInvariant polluterInv : pvInvs) {
                for (ReducedInvariant inv : ppt.getInvariants()) {
                    // ignore params, as matching params are unlikely to be the actual root cause
                    if (polluterInv.firstVar().startsWith("p("))
                        continue;

                    // check for e.g.:
                    // pv> var one of { "a", "b" }
                    // .v> var == "a"
                    // naive check for identical invariant will not work in this case, so we need to
                    // diff the invariants' eqValues
                    if (polluterInv.hasSameFirstVariableAs(inv) && inv.getEqValues() != null) {
                        List<String> polluterInvDiffed = polluterInv.diffEqValues(victimInvs);
                        if (polluterInvDiffed != null) {
                            for (String val : inv.getEqValues()) {
                                if (val == null)
                                    continue;
                                for (String polluterVal : polluterInvDiffed) {
                                    if (val.equals(polluterVal)) {
                                        // check if a victim invariant is seen in the :::ENTER
                                        for (ReducedInvariant polluterEnterInv : polluterEnterPpt.getInvariants()) {
                                            if (polluterEnterInv.getEqValues() == null)
                                                continue;
                                            for (ReducedInvariant victimInv : victimInvs) {
                                                List<String> diffedWithVictimEqValues = polluterEnterInv
                                                        .diffEqValues(Collections.singletonList(victimInv));
                                                if (diffedWithVictimEqValues != null && diffedWithVictimEqValues
                                                        .size() != polluterEnterInv.getEqValues().length) {
                                                    // if the sizes aren't equal, then one of the values was removed!
                                                    methods.add(new String[] { ppt.prettyName(), inv.toString() });
                                                    continue nextPolluterInv;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // naive check for identical invariants: polluter in EXIT, victim in ENTER
                        if (inv.toString().equals(polluterInv.toString())) {
                            for (ReducedInvariant polluterEnterInv : polluterEnterPpt.getInvariants()) {
                                for (ReducedInvariant victimInv : victimInvs) {
                                    if (polluterEnterInv.toString().equals(victimInv.toString())) {
                                        methods.add(new String[] { ppt.prettyName(), inv.toString() });
                                        continue nextPolluterInv;
                                    }
                                }
                            }
                        }
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

    public String toPVI() {
        StringBuilder builder = new StringBuilder();

        builder.append(Output.cleanMsgOfColorsIfNeeded(pptName + "\n"));

        List<String> pvPvis = invariantsToPvi(pvInvs);

        if (pvPvis.size() == 0) {
            return "";
        }

        builder.append("pv " + pvPvis.size() + "\n" +
                String.join("\n", pvPvis));

        builder.append("\n");

        List<String> victimPvis = invariantsToPvi(victimInvs);

        if (victimPvis.size() == 0) {
            return "";
        }

        builder.append("v " + victimPvis.size() + "\n" +
                String.join("\n", victimPvis));

        builder.append("\n");

        return builder.toString();
    }

    private List<String> invariantsToPvi(List<ReducedInvariant> invs) {
        return invs.stream().map((inv) -> {
            if (inv.getEqValues() != null) {
                if (inv.firstVar().startsWith("p("))
                    return null;

                List<String> diffed = inv.diffEqValues(victimInvs);
                if (diffed == null || diffed.size() == 0)
                    return null;

                return inv.fullFirstVar() + " " + diffed.size() + "\n" + String.join("\n", diffed);
            }
            return null;
        })
                .filter(s -> s != null).collect(Collectors.toList());
    }

    public String getPptName() {
        return pptName;
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
