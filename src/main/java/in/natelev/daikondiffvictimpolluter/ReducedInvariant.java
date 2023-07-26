package in.natelev.daikondiffvictimpolluter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import daikon.PptTopLevel;
import daikon.VarInfo;
import daikon.VarInfo.VarFlags;
import daikon.inv.Equality;
import daikon.inv.unary.string.OneOfString;
import daikon.inv.unary.string.SingleString;

public class ReducedInvariant implements Comparable<ReducedInvariant> {
    private String value;
    private String[] variables;
    private String[] eqValues = null;
    private String type;

    ReducedInvariant(String value, String[] variables, String type, String[] eqValues) {
        this.value = value;
        this.variables = variables;
        this.type = type;
        this.eqValues = eqValues;
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

    public void setValue(String value) {
        this.value = value;
    }

    public String getUniquelyHadIfNeeded(List<ReducedInvariant> otherList) {
        if (!getType().contains("OneOf"))
            return toString();

        boolean uniquesFound = false;
        StringBuilder uniquelyHad = new StringBuilder(variables[0] + " uniquely had { ");
        loopOverEqValues: for (String eqValue : eqValues) {
            if (eqValue == null)
                continue;

            for (ReducedInvariant otherInv : otherList) {
                if (hasSameFirstVariableAs(otherInv) && otherInv.eqValues != null) {
                    for (String otherEqValue : otherInv.eqValues) {
                        if (otherEqValue != null && eqValue.equals(otherEqValue)) {
                            if (!uniquesFound)
                                uniquesFound = true;

                            continue loopOverEqValues;
                        }
                    }
                }

                uniquelyHad.append("\"" + eqValue + "\"").append(", ");
            }
        }

        return uniquesFound ? uniquelyHad + "} (" + value + ")" : value;
    }

    public static List<ReducedInvariant> getFromPptTopLevel(PptTopLevel pptTopLevel) {
        return pptTopLevel.getInvariants().stream()
                .filter((invariant) -> {
                    return invariant.isWorthPrinting() && !invariant.hasUninterestingConstant();
                })
                .map((invariant) -> {
                    String[] variables = new String[invariant.ppt.var_infos.length];
                    for (int i = 0; i < invariant.ppt.var_infos.length; i++) {
                        VarInfo varInfo = invariant.ppt.var_infos[i];
                        boolean isParam = varInfo.isDerivedParam();
                        String name = varInfo.name();

                        if (varInfo.isPrestate()) {
                            variables[i] = name;
                            continue;
                        }

                        // "clean" the variables by removing the synthetic part:
                        // "name.getClass().getName()" -> "name", "name.toString()" -> "name"
                        // we do this because it allows the diffing step to understand that
                        // name.toString == "..." and name == null both refer to the same variable
                        if (varInfo.var_flags.contains(VarFlags.CLASSNAME)) {
                            name = name.substring(0, name.length() - ".getClass().getName()".length());
                        } else if (varInfo.var_flags.contains(VarFlags.TO_STRING)) {
                            if (name.endsWith(".getType()")) {
                                name = name.substring(0, name.length() - ".getType()".length());
                            } else if (name.endsWith(".toString")) {
                                // the () are omitted purposefully
                                name = name.substring(0, name.length() - ".toString".length());
                            } else if (name.endsWith(".mapInfo")) {
                                // the () are omitted purposefully
                                name = name.substring(0, name.length() - ".mapInfo".length());
                            }
                        }

                        variables[i] = String.format(isParam ? "p(%s)" : "%s", name);
                    }
                    String[] eqValues = null;
                    if (invariant instanceof OneOfString) {
                        eqValues = ((OneOfString) invariant).getElts();
                    }
                    return new ReducedInvariant(
                            invariant.toString(),
                            variables, invariant.getClass().getName(), eqValues);
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
