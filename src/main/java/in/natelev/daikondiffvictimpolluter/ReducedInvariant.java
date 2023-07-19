package in.natelev.daikondiffvictimpolluter;

import java.util.List;
import java.util.stream.Collectors;

import daikon.PptTopLevel;
import daikon.VarInfo.VarFlags;

public class ReducedInvariant implements Comparable<ReducedInvariant> {
    private String value;
    private String[] variables;
    private String type;

    ReducedInvariant(String value, String[] variables, String type) {
        this.value = value;
        this.variables = variables;
        this.type = type;
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

    public String varNames() {
        return "(" + String.join(", ", variables) + ")";
    }

    public String firstVar() {
        if (variables.length > 0) {
            return variables[0];
        } else {
            return null;
        }
    }

    public String getType() {
        return type;
    }

    public static List<ReducedInvariant> getFromPptTopLevel(PptTopLevel pptTopLevel) {
        return pptTopLevel.getInvariants().stream()
                .filter((invariant) -> {
                    return invariant.isWorthPrinting() && !invariant.hasUninterestingConstant();
                })
                .map((invariant) -> {
                    String[] variables = new String[invariant.ppt.var_infos.length];
                    for (int i = 0; i < invariant.ppt.var_infos.length; i++) {
                        boolean isParam = invariant.ppt.var_infos[i].isDerivedParam();
                        String name = invariant.ppt.var_infos[i].name();

                        if (name.startsWith("orig")) {
                            variables[i] = name;
                            continue;
                        }

                        // "clean" the variables by removing the synthetic part:
                        // "name.getClass().getName()" -> "name", "name.toString()" -> "name"
                        // we do this because it allows the diffing step to understand that
                        // name.toString == "..." and name == null both refer to the same variable
                        if (invariant.ppt.var_infos[i].var_flags.contains(VarFlags.CLASSNAME)) {
                            name = name.substring(0, name.length() - ".getClass().getName()".length());
                        } else if (invariant.ppt.var_infos[i].var_flags.contains(VarFlags.TO_STRING)) {
                            if (name.endsWith("()")) {
                                name = name.substring(0, name.length() - ".getType()".length());
                            } else {
                                // the () are omitted purposefully
                                name = name.substring(0, name.length() - ".toString".length());
                            }
                        }

                        variables[i] = String.format(isParam ? "p(%s)" : "%s", name);
                    }
                    return new ReducedInvariant(
                            invariant.toString(),
                            variables, invariant.getClass().getName());
                })
                .sorted()
                .collect(Collectors.toList());
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

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
