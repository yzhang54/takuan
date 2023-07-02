package in.natelev.daikondiffvictimpolluter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import static in.natelev.daikondiffvictimpolluter.Colors.*;

public class RankedInvs {
    private String pptName;
    // these are all guaranteed to have the same first variable & the same PPT
    private List<ReducedInvariant> pvInvs = new ArrayList<>();
    private List<ReducedInvariant> victimInvs = new ArrayList<>();

    RankedInvs(String pptName) {
        this.pptName = pptName;
    }

    public boolean hasBoth() {
        return pvInvs.size() > 0 && victimInvs.size() > 0;
    }

    public int totalSize() {
        return pvInvs.size() + victimInvs.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(YELLOW + pptName + RESET + "\n");
        for (ReducedInvariant inv : pvInvs) {
            builder.append(RED + "    pv> " + RESET + inv.toString() + "\n");
        }
        for (ReducedInvariant inv : victimInvs) {
            builder.append(GREEN + "    .v> " + RESET + inv.toString() + "\n");
        }
        return builder.toString();
    }

    public static class RankedInvByVarMap {
        private HashMap<String, RankedInvs> map = new HashMap<>();

        public Collection<RankedInvs> values() {
            return map.values();
        }

        public void put(String key, ReducedInvariant invariant, boolean isPv, String pptName) {
            map.compute(key, (_k, value) -> {
                if (value == null) {
                    value = new RankedInvs(pptName);
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
