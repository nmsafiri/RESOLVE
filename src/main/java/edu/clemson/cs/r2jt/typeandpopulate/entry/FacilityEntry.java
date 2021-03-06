/*
 * FacilityEntry.java
 * ---------------------------------
 * Copyright (c) 2020
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.r2jt.typeandpopulate.entry;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.clemson.cs.r2jt.absyn.EnhancementBodyItem;
import edu.clemson.cs.r2jt.absyn.EnhancementItem;
import edu.clemson.cs.r2jt.absyn.FacilityDec;
import edu.clemson.cs.r2jt.data.Location;
import edu.clemson.cs.r2jt.typeandpopulate.ModuleIdentifier;
import edu.clemson.cs.r2jt.typeandpopulate.ModuleParameterization;
import edu.clemson.cs.r2jt.typeandpopulate.programtypes.PTType;
import edu.clemson.cs.r2jt.typeandpopulate.ScopeRepository;
import edu.clemson.cs.r2jt.typeandpopulate.SpecRealizationPairing;

public class FacilityEntry extends SymbolTableEntry {

    private final SpecRealizationPairing myType;

    private final List<ModuleParameterization> myEnhancements =
            new LinkedList<ModuleParameterization>();

    private final ScopeRepository mySourceRepository;

    private Map<ModuleParameterization, ModuleParameterization> myEnhancementRealizations =
            new HashMap<ModuleParameterization, ModuleParameterization>();

    public FacilityEntry(FacilityDec facility, ModuleIdentifier sourceModule,
            ScopeRepository sourceRepository) {
        super(facility.getName().getName(), facility, sourceModule);

        mySourceRepository = sourceRepository;

        ModuleParameterization spec = new ModuleParameterization(
                new ModuleIdentifier(facility.getConceptName().getName()),
                facility.getConceptParams(), this, mySourceRepository);

        ModuleParameterization realization = null;
        if (facility.getBodyName() != null) {
            realization = new ModuleParameterization(
                    new ModuleIdentifier(facility.getBodyName().getName()),
                    facility.getBodyParams(), this, mySourceRepository);
        }

        myType = new SpecRealizationPairing(spec, realization);

        // These are realized by the concept realization
        for (EnhancementItem realizationEnhancement : facility
                .getEnhancements()) {

            spec = new ModuleParameterization(
                    new ModuleIdentifier(
                            realizationEnhancement.getName().getName()),
                    realizationEnhancement.getParams(), this,
                    mySourceRepository);

            myEnhancements.add(spec);
            myEnhancementRealizations.put(spec, realization);
        }

        // These are realized by individual enhancement realizations
        for (EnhancementBodyItem enhancement : facility
                .getEnhancementBodies()) {

            spec = new ModuleParameterization(
                    new ModuleIdentifier(enhancement.getName().getName()),
                    enhancement.getParams(), this, mySourceRepository);

            realization = new ModuleParameterization(
                    new ModuleIdentifier(enhancement.getBodyName().getName()),
                    enhancement.getBodyParams(), this, mySourceRepository);

            myEnhancements.add(spec);
            myEnhancementRealizations.put(spec, realization);
        }
    }

    public SpecRealizationPairing getFacility() {
        return myType;
    }

    public List<ModuleParameterization> getEnhancements() {
        return Collections.unmodifiableList(myEnhancements);
    }

    /**
     * <p>
     * Do not assume that each enhancement is realized by a corresponding,
     * individual realization.
     * Some enhancements may be realized by the base concept, in which case this
     * method will return
     * that module.
     * </p>
     * 
     * <p>
     * For example, <code>Stack_Template</code> might have an enhancement
     * <code>Get_Nth_Ability</code>, which replicates and returns the
     * <em>n</em>th element from the
     * top of the stack. There may be an
     * <code>Obvious_Get_Nth_Realization</code> that does what you'd
     * expect: repeatedly pop some elements off the top to get to the requested
     * element, replicate it,
     * then push everything back on. Using this enhancement would be a fine
     * choice for extending
     * <code>Stack_Template</code> as realized by
     * <code>Pointer_Realization</code>. However, if we
     * happen to know that we're using <code>Array_Based_Realization</code>, we
     * can do much better.
     * For this purpose, <code>Array_Based_Realization</code> can directly
     * incorporate enhancements
     * and provide a direct realization that takes advantage of implementation
     * details. I.e.,
     * <code>Get_Nth()</code> would be included as a procedure inside
     * <code>Array_Based_Realization</code>. In such a case, we would declare a
     * facility like this:
     * </p>
     * 
     * <pre>
     * Facility Indexible_Stack is Stack_Template enhanced with Get_Nth_Ability
     *     realized by Array_Based_Realization;
     * </pre>
     * 
     * <p>
     * And, calling this method to ask the realization for
     * <code>Get_Nth_Ability</code> would return
     * <code>Array_Based_Realization</code>.
     * </p>
     * 
     * @param enhancement
     * @return
     */
    public ModuleParameterization
            getEnhancementRealization(ModuleParameterization enhancement) {

        return myEnhancementRealizations.get(enhancement);
    }

    @Override
    public FacilityEntry toFacilityEntry(Location l) {
        return this;
    }

    @Override
    public String getEntryTypeDescription() {
        return "a facility";
    }

    @Override
    public FacilityEntry instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilityEntry instantiatingFacility) {

        // TODO : This is probably wrong. One of the parameters to a module
        // used in the facility could be a generic, in which case it
        // should be replaced with the corresponding concrete type--but
        // how?
        return this;
    }
}
