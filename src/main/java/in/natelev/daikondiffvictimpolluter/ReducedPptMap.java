package in.natelev.daikondiffvictimpolluter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            if (pptTopLevel.num_samples() == 0 || pptTopLevel.is_enter())
                continue;

            List<ReducedInvariant> invariants = ReducedInvariant.getFromPptTopLevel(pptTopLevel);

            if (invariants.size() == 0)
                continue;

            if (pptTopLevel.is_class()) {
                // combine :::CLASS and :::OBJECT, because we don't need the distinction
                // ? are we losing some information by doing this?
                // NOTE: :::CLASS always comes before :::OBJECT. If the ordering gets changed,
                // this code will break
                map.put(pptTopLevel.name.substring(0, pptTopLevel.name.length() - 5) + "OBJECT",
                        new ReducedPpt(pptTopLevel, invariants));
            } else if (pptTopLevel.is_object()) {
                map.compute(pptTopLevel.name,
                        (_k, val) -> {
                            if (val == null) {
                                return new ReducedPpt(pptTopLevel, invariants);
                            } else {
                                return new ReducedPpt(pptTopLevel,
                                        Stream.concat(val.getInvariants().stream(), invariants.stream()).distinct()
                                                .collect(Collectors.toList()));
                            }
                        });
            } else {
                map.put(pptTopLevel.name, new ReducedPpt(pptTopLevel, invariants));
            }
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
}
