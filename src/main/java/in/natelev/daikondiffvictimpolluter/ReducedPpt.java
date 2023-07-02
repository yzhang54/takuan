package in.natelev.daikondiffvictimpolluter;

import java.util.List;

import daikon.PptTopLevel;
import daikon.PptTopLevel.PptType;

import static in.natelev.daikondiffvictimpolluter.Colors.*;

public class ReducedPpt {
    public String name;
    private List<ReducedInvariant> invariants;
    private PptType type;

    ReducedPpt(PptTopLevel pptTopLevel, List<ReducedInvariant> invariants) {
        this.name = pptTopLevel.name;
        this.type = pptTopLevel.type;
        this.invariants = invariants;
    }

    public List<ReducedInvariant> getInvariants() {
        return invariants;
    }

    public PptType getType() {
        return type;
    }

    public String prettyName() {
        int indexOfColon = name.indexOf(":");
        String typeStr = MAGENTA + name.substring(indexOfColon) + RESET;

        if (type == PptType.EXIT) {
            int indexOfParen = name.indexOf("(");
            return YELLOW + name.substring(0, indexOfParen) + CYAN + name.substring(indexOfParen, indexOfColon)
                    + typeStr;
        }

        return YELLOW + name.substring(0, indexOfColon) + typeStr;
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
