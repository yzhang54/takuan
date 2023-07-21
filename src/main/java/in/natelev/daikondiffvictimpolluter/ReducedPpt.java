package in.natelev.daikondiffvictimpolluter;

import java.util.List;
import java.util.Set;

import daikon.PptTopLevel;
import daikon.VarInfo;
import daikon.PptTopLevel.PptType;

import static in.natelev.daikondiffvictimpolluter.Colors.*;

public class ReducedPpt {
    public String name;
    private String nameWithVars;
    private List<ReducedInvariant> invariants;
    private PptType type;

    ReducedPpt(String name, PptTopLevel pptTopLevel, List<ReducedInvariant> invariants) {
        this.name = name;
        this.nameWithVars = addVarsToPptName(name, pptTopLevel);
        this.type = pptTopLevel.type;
        this.invariants = invariants;
    }

    private static String addVarsToPptName(String name, PptTopLevel pptTopLevel) {
        if (!pptTopLevel.is_exit())
            return name;

        Set<VarInfo> paramVars = pptTopLevel.getParamVars();
        VarInfo[] vars = paramVars.toArray(new VarInfo[paramVars.size()]);
        if (vars.length <= 1)
            return name;

        StringBuilder modifiedName = new StringBuilder(name);

        int lastCheckedIndex = modifiedName.indexOf("(");
        for (int i = 1; i < vars.length - 1; i++) {
            lastCheckedIndex = modifiedName.indexOf(",", lastCheckedIndex);
            modifiedName.insert(lastCheckedIndex, " " + vars[i]);
        }

        modifiedName.insert(modifiedName.indexOf(")", lastCheckedIndex), " " +
                vars[vars.length - 1]);

        return modifiedName.toString();
    }

    public List<ReducedInvariant> getInvariants() {
        return invariants;
    }

    public PptType getType() {
        return type;
    }

    public String prettyName() {
        int indexOfColon = nameWithVars.indexOf(":");
        String typeStr = MAGENTA + nameWithVars.substring(indexOfColon) + RESET;

        if (type == PptType.EXIT) {
            int indexOfParen = nameWithVars.indexOf("(");
            return YELLOW + nameWithVars.substring(0, indexOfParen) + CYAN
                    + nameWithVars.substring(indexOfParen, indexOfColon)
                    + typeStr;
        }

        return YELLOW + nameWithVars.substring(0, indexOfColon) + typeStr;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(
                BLUE + "===========================================================================" + RESET + "\n");
        builder.append(prettyName() + "\n");
        for (ReducedInvariant invariant : invariants) {
            builder.append("* " + invariant + "\n");
        }
        return builder.toString();
    }
}
