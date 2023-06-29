package in.natelev.daikondiffvictimpolluter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import daikon.PptMap;
import daikon.PptTopLevel;

public class ReducedPptMap {
    public LinkedHashMap<String, ReducedPpt> map;

    ReducedPptMap() {
        map = new LinkedHashMap<>();
    }

    ReducedPptMap(PptMap pptMap) {
        map = new LinkedHashMap<>(pptMap.size());

        for (PptTopLevel pptTopLevel : pptMap.pptIterable()) {
            if (pptTopLevel.num_samples() == 0)
                continue;
            List<String> invariants = getStringifiedInvariantsOf(pptTopLevel);
            if (invariants.size() == 0)
                continue;

            map.put(pptTopLevel.name, new ReducedPpt(pptTopLevel, invariants));
        }
    }

    public Iterable<ReducedPpt> pptIterable() {
        return map.values();
    }

    public boolean containsName(String name) {
        return map.containsKey(name);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (ReducedPpt ppt : map.values()) {
            builder.append(ppt + "\n");
        }
        return builder.toString();
    }

    private static List<String> getStringifiedInvariantsOf(PptTopLevel pptTopLevel) {
        return pptTopLevel.getInvariants().stream()
                .filter((invariant) -> {
                    return invariant.isWorthPrinting() && !invariant.hasUninterestingConstant();
                }).map((invariant) -> invariant.toString()).sorted()
                .collect(Collectors.toList());
    }
}
