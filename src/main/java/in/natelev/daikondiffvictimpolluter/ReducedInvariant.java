package in.natelev.daikondiffvictimpolluter;

import java.util.List;
import java.util.stream.Collectors;

import daikon.PptTopLevel;

public class ReducedInvariant implements Comparable<ReducedInvariant> {
    private String value;
    private String[] variables;

    ReducedInvariant(String value, String[] variables) {
        this.value = value;
        this.variables = variables;
    }

    public boolean hasSameFirstVariableAs(ReducedInvariant other) {
        return other.variables.length > 0 && variables.length > 0 && variables[0].equals(other.variables[0]);
    }

    public boolean hasSameVariablesAs(ReducedInvariant other) {
        if (variables.length != other.variables.length)
            return false;
        for (int i = 0; i < variables.length; i++) {
            if (!variables[i].equals(other.variables[i]))
                return false;
        }
        return true;
    }

    public static List<ReducedInvariant> getFromPptTopLevel(PptTopLevel pptTopLevel) {
        return pptTopLevel.getInvariants().stream()
                .filter((invariant) -> {
                    return invariant.isWorthPrinting() && !invariant.hasUninterestingConstant();
                })
                .map((invariant) -> {
                    String[] variables = new String[invariant.ppt.var_infos.length];
                    for (int i = 0; i < invariant.ppt.var_infos.length; i++) {
                        variables[i] = invariant.ppt.var_infos[i].name();
                    }
                    return new ReducedInvariant(
                            invariant.toString(),
                            variables);
                })
                .sorted()
                .collect(Collectors.toList());
    }

    public String varNames() {
        return "(" + String.join(", ", variables) + ")";
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(ReducedInvariant other) {
        return this.toString().compareTo(other.toString());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ReducedInvariant) {
            return this.toString().equals(other.toString());
        }
        return false;
    }
}
