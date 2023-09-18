package in.natelev.daikondiffvictimpolluter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import daikon.PptTopLevel;
import daikon.VarInfo;
import daikon.VarInfo.VarFlags;
import daikon.inv.unary.scalar.OneOfScalar;
import daikon.inv.unary.string.OneOfString;

public class ReducedInvariant implements Comparable<ReducedInvariant> {
    private String value;
    private String[] variables;
    private String[] eqValues = null;
    private String fullFirstVariable; // includes postfix
    private String type;

    ReducedInvariant(String value, String[] variables, String type, String[] eqValues, String fullFirstVariable) {
        this.value = value;
        this.variables = variables;
        this.type = type;
        this.eqValues = eqValues;
        this.fullFirstVariable = fullFirstVariable;
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

    public String fullFirstVar() {
        return fullFirstVariable;
    }

    public String getType() {
        return type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String[] getEqValues() {
        return eqValues;
    }

    public String getUniquelyHadIfNeeded(List<ReducedInvariant> otherList) {
        List<String> uniquelyHad = diffEqValues(otherList);

        if (uniquelyHad == null)
            return toString();

        return firstVar() + " uniquely had { " + uniquelyHad.stream().map((val) -> {
            return "\"" + val + "\"";
        }).collect(Collectors.joining(", ")) + " } (" + value + ")";
    }

    public List<String> diffEqValues(List<ReducedInvariant> otherList) {
        if (eqValues == null || !getType().contains("OneOf"))
            return null;

        boolean uniquesFound = false;
        List<String> uniquelyHad = new ArrayList<>();
        loopOverEqValues: for (String eqValue : eqValues) {
            if (eqValue == null)
                continue;

            for (ReducedInvariant otherInv : otherList) {
                if (hasSameFirstVariableAs(otherInv) && otherInv.eqValues != null) {
                    for (String otherEqValue : otherInv.eqValues) {
                        if (otherInv == null)
                            continue;
                        if (otherEqValue != null && eqValue.equals(otherEqValue)) {
                            if (!uniquesFound)
                                uniquesFound = true;

                            continue loopOverEqValues;
                        }
                    }
                }

                uniquelyHad.add(eqValue);
            }
        }

        return uniquesFound ? uniquelyHad : null;
    }

    public static List<ReducedInvariant> getFromPptTopLevel(PptTopLevel pptTopLevel) {
        return pptTopLevel.getInvariants().stream()
                .filter((invariant) -> {
                    // we might want to consider adding `&& !invariant.hasUninterestingConstant()`,
                    // if it gives us better results. it was removed to work better on the
                    // toy-flaky-tests repo
                    return invariant.isWorthPrinting();
                })
                .map((invariant) -> {
                    String[] variables = new String[invariant.ppt.var_infos.length];
                    for (int i = 0; i < invariant.ppt.var_infos.length; i++) {
                        VarInfo varInfo = invariant.ppt.var_infos[i];
                        boolean isParam = varInfo.isDerivedParam() && !varInfo.name().startsWith("this.");
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
                    } else if (invariant instanceof OneOfScalar) {
                        eqValues = Arrays.stream(((OneOfScalar) invariant).getElts()).mapToObj(String::valueOf)
                                .toArray(String[]::new);
                    }

                    String firstVar = invariant.ppt.var_infos.length > 0 ? invariant.ppt.var_infos[0].name() : null;

                    return new ReducedInvariant(
                            invariant.toString(),
                            variables, invariant.getClass().getName(), eqValues, firstVar);
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
