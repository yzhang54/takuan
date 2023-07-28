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
            if (pptTopLevel.num_samples() == 0)
                continue;

            List<ReducedInvariant> invariants = ReducedInvariant.getFromPptTopLevel(pptTopLevel);

            if (invariants.size() == 0)
                continue;

            if (pptTopLevel.is_class()) {
                // combine :::CLASS and :::OBJECT, because we don't need the distinction
                // ? are we losing some information by doing this?
                // NOTE: :::CLASS always comes before :::OBJECT. If the ordering gets changed,
                // this code will break
                String name = pptTopLevel.name.substring(0, pptTopLevel.name.length() - 5) + "OBJECT";
                map.put(name,
                        new ReducedPpt(name, pptTopLevel, invariants));
            } else if (pptTopLevel.is_object()) {
                map.compute(pptTopLevel.name,
                        (name, val) -> {
                            if (val == null) {
                                return new ReducedPpt(name, pptTopLevel, invariants);
                            } else {
                                return new ReducedPpt(name, pptTopLevel,
                                        Stream.concat(val.getInvariants().stream(), invariants.stream()).distinct()
                                                .collect(Collectors.toList()));
                            }
                        });
            } else {
                map.put(pptTopLevel.name, new ReducedPpt(pptTopLevel.name, pptTopLevel, invariants));
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
