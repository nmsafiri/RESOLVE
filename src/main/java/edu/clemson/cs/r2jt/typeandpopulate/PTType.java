package edu.clemson.cs.r2jt.typeandpopulate;

import edu.clemson.cs.r2jt.typeandpopulate.entry.FacilityEntry;
import java.util.Map;

import edu.clemson.cs.r2jt.typereasoning.TypeGraph;

public abstract class PTType {

    private final TypeGraph myTypeGraph;

    public PTType(TypeGraph g) {
        myTypeGraph = g;
    }

    public final TypeGraph getTypeGraph() {
        return myTypeGraph;
    }

    public abstract MTType toMath();

    public abstract PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilityEntry instantiatingFacility);
}