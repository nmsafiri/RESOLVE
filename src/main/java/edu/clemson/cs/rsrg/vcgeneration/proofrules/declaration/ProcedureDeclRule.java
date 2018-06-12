/*
 * ProcedureDeclRule.java
 * ---------------------------------
 * Copyright (c) 2017
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.rsrg.vcgeneration.proofrules.declaration;

import edu.clemson.cs.rsrg.absyn.clauses.AffectsClause;
import edu.clemson.cs.rsrg.absyn.clauses.AssertionClause;
import edu.clemson.cs.rsrg.absyn.clauses.AssertionClause.ClauseType;
import edu.clemson.cs.rsrg.absyn.declarations.mathdecl.MathDefVariableDec;
import edu.clemson.cs.rsrg.absyn.declarations.moduledecl.ConceptRealizModuleDec;
import edu.clemson.cs.rsrg.absyn.declarations.operationdecl.ProcedureDec;
import edu.clemson.cs.rsrg.absyn.declarations.sharedstatedecl.SharedStateDec;
import edu.clemson.cs.rsrg.absyn.declarations.typedecl.TypeFamilyDec;
import edu.clemson.cs.rsrg.absyn.declarations.typedecl.TypeRepresentationDec;
import edu.clemson.cs.rsrg.absyn.declarations.variabledecl.MathVarDec;
import edu.clemson.cs.rsrg.absyn.declarations.variabledecl.ParameterVarDec;
import edu.clemson.cs.rsrg.absyn.declarations.variabledecl.VarDec;
import edu.clemson.cs.rsrg.absyn.expressions.Exp;
import edu.clemson.cs.rsrg.absyn.expressions.mathexpr.*;
import edu.clemson.cs.rsrg.absyn.expressions.mathexpr.EqualsExp.Operator;
import edu.clemson.cs.rsrg.absyn.rawtypes.NameTy;
import edu.clemson.cs.rsrg.absyn.statements.AssumeStmt;
import edu.clemson.cs.rsrg.absyn.statements.ConfirmStmt;
import edu.clemson.cs.rsrg.parsing.data.Location;
import edu.clemson.cs.rsrg.parsing.data.LocationDetailModel;
import edu.clemson.cs.rsrg.parsing.data.PosSymbol;
import edu.clemson.cs.rsrg.typeandpopulate.entry.*;
import edu.clemson.cs.rsrg.typeandpopulate.entry.ProgramParameterEntry.ParameterMode;
import edu.clemson.cs.rsrg.typeandpopulate.mathtypes.MTCartesian;
import edu.clemson.cs.rsrg.typeandpopulate.mathtypes.MTType;
import edu.clemson.cs.rsrg.typeandpopulate.programtypes.PTRepresentation;
import edu.clemson.cs.rsrg.typeandpopulate.symboltables.MathSymbolTableBuilder;
import edu.clemson.cs.rsrg.typeandpopulate.symboltables.ModuleScope;
import edu.clemson.cs.rsrg.typeandpopulate.typereasoning.TypeGraph;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.AbstractProofRuleApplication;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.ProofRuleApplication;
import edu.clemson.cs.rsrg.vcgeneration.utilities.AssertiveCodeBlock;
import edu.clemson.cs.rsrg.vcgeneration.utilities.Utilities;
import edu.clemson.cs.rsrg.vcgeneration.utilities.VerificationContext;
import edu.clemson.cs.rsrg.vcgeneration.utilities.formaltoactual.InstantiatedFacilityDecl;
import edu.clemson.cs.rsrg.vcgeneration.utilities.helperstmts.FinalizeVarStmt;
import java.util.*;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

/**
 * <p>This class contains the logic for the {@code procedure}
 * declaration rule.</p>
 *
 * @author Yu-Shan Sun
 * @version 1.0
 */
public class ProcedureDeclRule extends AbstractProofRuleApplication
        implements
            ProofRuleApplication {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /**
     * <p>All the shared variables affected by the associated
     * {@link OperationEntry} and the current {@link ProcedureDec}.</p>
     */
    private final Set<Exp> myAffectedExps;

    /**
     * <p>The module scope for the file we are generating
     * {@code VCs} for.</p>
     */
    private final ModuleScope myCurrentModuleScope;

    /**
     * <p>If we are in a {@code Procedure} and it is an recursive
     * operation implementation, then this stores the decreasing clause
     * expression.</p>
     */
    private final Exp myCurrentProcedureDecreasingExp;

    /**
     * <p>The {@link OperationEntry} associated with this {@code If}
     * statement if we are inside a {@code ProcedureDec}.</p>
     */
    private final OperationEntry myCurrentProcedureOperationEntry;

    /** <p>The {@link ProcedureDec} we are applying the rule to.</p> */
    private final ProcedureDec myProcedureDec;

    /**
     * <p>While walking a procedure, this stores all the local {@link VarDec VarDec's}
     * program type entry.</p>
     */
    private final Map<VarDec, SymbolTableEntry> myVariableTypeEntries;

    /**
     * <p>This is the math type graph that indicates relationship
     * between different math types.</p>
     */
    private final TypeGraph myTypeGraph;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * <p>This creates a new application for the {@code procedure}
     * declaration rule.</p>
     *
     * @param procedureDec The {@link ProcedureDec} we are applying
     *                     the rule to.
     * @param procVarTypeEntries The local variable declaration's
     *                           program type entries.
     * @param symbolTableBuilder The current symbol table.
     * @param moduleScope The current module scope we are visiting.
     * @param block The assertive code block that the subclasses are
     *              applying the rule to.
     * @param context The verification context that contains all
     *                the information we have collected so far.
     * @param stGroup The string template group we will be using.
     * @param blockModel The model associated with {@code block}.
     */
    public ProcedureDeclRule(ProcedureDec procedureDec,
            Map<VarDec, SymbolTableEntry> procVarTypeEntries,
            MathSymbolTableBuilder symbolTableBuilder, ModuleScope moduleScope,
            AssertiveCodeBlock block, VerificationContext context,
            STGroup stGroup, ST blockModel) {
        super(block, context, stGroup, blockModel);
        myAffectedExps = new LinkedHashSet<>();
        myCurrentModuleScope = moduleScope;
        myCurrentProcedureDecreasingExp =
                myCurrentAssertiveCodeBlock
                        .getCorrespondingOperationDecreasingExp();
        myCurrentProcedureOperationEntry =
                myCurrentAssertiveCodeBlock.getCorrespondingOperation();
        myTypeGraph = symbolTableBuilder.getTypeGraph();
        myProcedureDec = procedureDec;
        myVariableTypeEntries = procVarTypeEntries;

        // Build a set of shared variables being affected
        // from the OperationEntry.
        AffectsClause operationAffectsClause = myCurrentProcedureOperationEntry.getAffectsClause();
        if (operationAffectsClause != null) {
            for (Exp exp : operationAffectsClause.getAffectedExps()) {
                if (!Utilities.containsEquivalentExp(myAffectedExps, exp)) {
                    myAffectedExps.add(exp.clone());
                }
            }
        }

        // Build a set of shared variables being affected
        // by the current procedure declaration.
        AffectsClause affectsClause = myProcedureDec.getAffectedVars();
        if (affectsClause != null) {
            for (Exp exp : affectsClause.getAffectedExps()) {
                if (!Utilities.containsEquivalentExp(myAffectedExps, exp)) {
                    myAffectedExps.add(exp.clone());
                }
            }
        }
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * <p>This method applies the {@code Proof Rule}.</p>
     */
    @Override
    public final void applyRule() {
        // Check to see if this a local operation
        boolean isLocal =
                Utilities.isLocationOperation(myProcedureDec.getName()
                        .getName(), myCurrentModuleScope);

        // Check to see if we are in a concept realization
        boolean inConceptRealiz =
                myCurrentModuleScope.getDefiningElement() instanceof ConceptRealizModuleDec;

        // Check to see if this is a recursive procedure.
        // If yes, we will need to add an additional assume clause
        // (P_Val = <decreasing clause>).
        if (myCurrentProcedureDecreasingExp != null) {
            // Create P_Val and add it as a free variable
            VarExp pValExp =
                    Utilities.createPValExp(myCurrentProcedureDecreasingExp
                            .getLocation().clone(), myCurrentModuleScope);
            myCurrentAssertiveCodeBlock.addFreeVar(pValExp);

            // Generate progress metric recursive call: P_Val = P_Exp
            EqualsExp equalsExp =
                    new EqualsExp(myCurrentProcedureDecreasingExp.getLocation()
                            .clone(), pValExp.clone(), null, Operator.EQUAL,
                            myCurrentProcedureDecreasingExp.clone());
            equalsExp.setMathType(myTypeGraph.BOOLEAN);

            // Store the location detail for the recursive operation's
            // progress metric expression.
            equalsExp.setLocationDetailModel(new LocationDetailModel(
                    myCurrentProcedureDecreasingExp.getLocation().clone(),
                    myCurrentProcedureDecreasingExp.getLocation().clone(),
                    "Progress Metric for Recursive Procedure"));

            // Add this expression as something we can assume to be true.
            AssumeStmt progressMetricAssume =
                    new AssumeStmt(myCurrentProcedureDecreasingExp
                            .getLocation().clone(), equalsExp, false);
            myCurrentAssertiveCodeBlock.addStatement(progressMetricAssume);
        }

        // Add all the statements
        myCurrentAssertiveCodeBlock.addStatements(myProcedureDec
                .getStatements());

        // YS: Simply create a finalization statement for each variable that
        //     allow us to deal with generating question mark variables
        //     and duration logic when we backtrack through the code.
        List<VarDec> varDecs = myProcedureDec.getVariables();
        for (VarDec dec : varDecs) {
            // Only need to finalize non-generic type variables.
            if (myVariableTypeEntries.containsKey(dec)) {
                myCurrentAssertiveCodeBlock.addStatement(new FinalizeVarStmt(
                        dec, myVariableTypeEntries.remove(dec)));
            }

            // TODO: Add the finalization duration ensures (if any)
        }

        // Only applies to non-local operations in concept realizations.
        if (inConceptRealiz && !isLocal) {
            // Make sure that the convention still holds.
            Exp correctOpHyp = createCorrectOpHypExp();

            // Replace any facility declaration instantiation arguments
            // in the correct operation hypothesis expression.
            correctOpHyp =
                    Utilities
                            .replaceFacilityFormalWithActual(correctOpHyp,
                                    myProcedureDec.getParameters(),
                                    myCurrentModuleScope.getDefiningElement()
                                            .getName(),
                                    myCurrentVerificationContext);

            // Use the expression to create a confirm statement if it is not "true"
            ConfirmStmt correctOpHypStmt =
                    new ConfirmStmt(myProcedureDec.getLocation().clone(),
                            correctOpHyp, true);
            myCurrentAssertiveCodeBlock.addStatement(correctOpHypStmt);

            // Add an assume with the correspondences. Rather than doing direct replacement,
            // we leave that logic to the parsimonious vc step. A replacement
            // will occur if this is a correspondence function or an implies
            // will be formed if this is a correspondence relation.
            Exp correspondenceExp = createCorrespondenceExp();

            // Replace any facility declaration instantiation arguments
            // in the correct operation hypothesis expression.
            correspondenceExp =
                    Utilities
                            .replaceFacilityFormalWithActual(correspondenceExp,
                                    myProcedureDec.getParameters(),
                                    myCurrentModuleScope.getDefiningElement()
                                            .getName(),
                                    myCurrentVerificationContext);

            // Use the expression to create an assume statement if it is not "true"
            AssumeStmt correspondenceStmt =
                    new AssumeStmt(myProcedureDec.getLocation().clone(),
                            correspondenceExp, false);
            myCurrentAssertiveCodeBlock.addStatement(correspondenceStmt);
        }

        // Create the final confirm expression
        Exp finalConfirmExp = createFinalConfirmExp(inConceptRealiz, isLocal);

        // Replace any facility declaration instantiation arguments
        // in the ensures clause.
        finalConfirmExp =
                Utilities.replaceFacilityFormalWithActual(finalConfirmExp,
                        myProcedureDec.getParameters(), myCurrentModuleScope
                                .getDefiningElement().getName(),
                        myCurrentVerificationContext);

        // Use the ensures clause to create a final confirm statement
        ConfirmStmt finalConfirmStmt =
                new ConfirmStmt(myProcedureDec.getLocation().clone(),
                        finalConfirmExp, VarExp.isLiteralTrue(finalConfirmExp));
        myCurrentAssertiveCodeBlock.addStatement(finalConfirmStmt);

        // Add the different details to the various different output models
        ST stepModel = mySTGroup.getInstanceOf("outputVCGenStep");
        stepModel.add("proofRuleName", getRuleDescription()).add(
                "currentStateOfBlock", myCurrentAssertiveCodeBlock);
        myBlockModel.add("vcGenSteps", stepModel.render());
    }

    /**
     * <p>This method returns a description associated with
     * the {@code Proof Rule}.</p>
     *
     * @return A string.
     */
    @Override
    public final String getRuleDescription() {
        return "Procedure Declaration Rule (Part 2)";
    }

    // ===========================================================
    // Private Methods
    // ===========================================================

    /**
     * <p>An helper method for adding the appropriate substitutions
     * for the different versions of {@code varExp}.</p>
     *
     * @param varExp A variable expression.
     * @param oldVarExp The incoming version of {@code varExp}.
     * @param concVarExp The conceptual version of {@code varExp}.
     * @param substitutionMap The substitution map where these expressions
     *                        are needed.
     *
     * @return An updated map.
     */
    private Map<Exp, Exp> addConceptualVariables(VarExp varExp,
            OldExp oldVarExp, DotExp concVarExp, Map<Exp, Exp> substitutionMap) {
        // Create an incoming version of the conceptual variable
        OldExp oldConcVarExp =
                new OldExp(concVarExp.getLocation().clone(), concVarExp.clone());
        oldConcVarExp.setMathType(concVarExp.getMathType());

        // Add these to our substitution map
        substitutionMap.put(varExp, concVarExp);
        substitutionMap.put(oldVarExp, oldConcVarExp);

        return substitutionMap;
    }

    /**
     * <p>An helper method that uses the {@code Shared Variable} and any parameter type
     * {@code conventions} to build the appropriate expression that must be established
     * for a {@code Concept Realization} operation to be correct.</p>
     *
     * @return The correct operation hypothesis expression.
     */
    private Exp createCorrectOpHypExp() {
        // Add all shared state realization conventions
        Location loc = myProcedureDec.getLocation().clone();
        Exp retExp =
                myCurrentVerificationContext
                        .createSharedStateRealizConventionExp(loc);
        LocationDetailModel model = retExp.getLocationDetailModel().clone();
        retExp.setLocationDetailModel(new LocationDetailModel(model
                .getSourceLoc(), model.getDestinationLoc(), model
                .getDetailMessage()
                + " Generated by " + myProcedureDec.getName().getName()));

        // Add the conventions of parameters that have representation types.
        for (ParameterVarDec parameterVarDec : myProcedureDec.getParameters()) {
            // Check to see if we have a representation type.
            NameTy nameTy = (NameTy) parameterVarDec.getTy();
            if (nameTy.getProgramType() instanceof PTRepresentation) {
                // Query for the type entry in the symbol table
                PTRepresentation representationType =
                        (PTRepresentation) nameTy.getProgramType();
                SymbolTableEntry ste =
                        Utilities.searchProgramType(loc, nameTy.getQualifier(),
                                nameTy.getName(), myCurrentModuleScope);

                // Only process this if it is TypeRepresentationDec
                // (Might need to add more logic if it is not).
                if (ste.getDefiningElement() instanceof TypeRepresentationDec) {
                    AssertionClause conventionClause =
                            Utilities.getTypeConventionClause(
                                    ((TypeRepresentationDec) ste
                                            .getDefiningElement())
                                            .getConvention(), loc.clone(),
                                    parameterVarDec.getName(), new PosSymbol(
                                            loc.clone(), representationType
                                                    .getFamily().getExemplar()
                                                    .getName()), nameTy
                                            .getMathType(), null);
                    LocationDetailModel conventionDetailModel =
                            new LocationDetailModel(loc.clone(), loc.clone(),
                                    "Type Convention for "
                                            + nameTy.getName().getName()
                                            + " Generated by "
                                            + myProcedureDec.getName()
                                                    .getName());

                    if (VarExp.isLiteralTrue(retExp)) {
                        retExp =
                                Utilities
                                        .formConjunct(loc, null,
                                                conventionClause,
                                                conventionDetailModel);
                    }
                    else {
                        retExp =
                                Utilities
                                        .formConjunct(loc, retExp,
                                                conventionClause,
                                                conventionDetailModel);
                    }
                }
            }
        }

        return retExp;
    }

    /**
     * <p>An helper method that uses the {@code Shared Variable} and any parameter type
     * {@code correspondence} to build the appropriate expression.</p>
     *
     * @return An {@link Exp}.
     */
    private Exp createCorrespondenceExp() {
        // Add all shared state realization correspondence
        Location loc = myProcedureDec.getLocation().clone();
        Exp retExp =
                myCurrentVerificationContext
                        .createSharedStateRealizCorrespondenceExp(loc);
        LocationDetailModel model = retExp.getLocationDetailModel().clone();
        retExp.setLocationDetailModel(new LocationDetailModel(model
                .getSourceLoc(), model.getDestinationLoc(), model
                .getDetailMessage()
                + " Generated by " + myProcedureDec.getName().getName()));

        // Add the correspondence of parameters that have representation types.
        for (ParameterVarDec parameterVarDec : myProcedureDec.getParameters()) {
            // Check to see if we have a representation type.
            NameTy nameTy = (NameTy) parameterVarDec.getTy();
            if (nameTy.getProgramType() instanceof PTRepresentation) {
                // Query for the type entry in the symbol table
                PTRepresentation representationType =
                        (PTRepresentation) nameTy.getProgramType();
                SymbolTableEntry ste =
                        Utilities.searchProgramType(loc, nameTy.getQualifier(),
                                nameTy.getName(), myCurrentModuleScope);

                // Only process this if it is TypeRepresentationDec
                // (Might need to add more logic if it is not).
                if (ste.getDefiningElement() instanceof TypeRepresentationDec) {
                    AssertionClause correspondenceClause =
                            Utilities.getTypeCorrespondenceClause(
                                    ((TypeRepresentationDec) ste
                                            .getDefiningElement())
                                            .getCorrespondence(), loc.clone(),
                                    parameterVarDec.getName(), nameTy,
                                    new PosSymbol(loc.clone(),
                                            representationType.getFamily()
                                                    .getExemplar().getName()),
                                    nameTy, nameTy.getMathType(), null,
                                    myTypeGraph.BOOLEAN);
                    LocationDetailModel correspondenceDetailModel =
                            new LocationDetailModel(loc.clone(), loc.clone(),
                                    "Type Correspondence for "
                                            + nameTy.getName().getName()
                                            + " Generated by "
                                            + myProcedureDec.getName()
                                                    .getName());

                    if (VarExp.isLiteralTrue(retExp)) {
                        retExp =
                                Utilities.formConjunct(loc, null,
                                        correspondenceClause,
                                        correspondenceDetailModel);
                    }
                    else {
                        retExp =
                                Utilities.formConjunct(loc, retExp,
                                        correspondenceClause,
                                        correspondenceDetailModel);
                    }
                }
            }
        }

        return retExp;
    }

    /**
     * <p>An helper method that uses the {@code ensures} clause from the operation entry
     * and adds in additional {@code ensures} clauses for different parameter modes
     * and builds the appropriate {@code ensures} clause that will be an
     * {@link AssertiveCodeBlock AssertiveCodeBlock's} final {@code confirm} statement.</p>
     *
     * @param inConceptRealiz A flag that indicates whether or not this {@link ProcedureDec}
     *                        is inside a {@code Concept Realization}.
     * @param isLocal A flag that indicates whether or not this is a local operation.
     *
     * @return The final confirm expression.
     */
    private Exp createFinalConfirmExp(boolean inConceptRealiz, boolean isLocal) {
        Exp retExp;
        Location procedureLoc = myProcedureDec.getLocation();

        // Create a replacement map for substituting parameter
        // variables with representation types.
        Map<Exp, Exp> substitutionParamToConc = new LinkedHashMap<>();

        // Loop through each of the parameters in the operation entry.
        Iterator<ProgramParameterEntry> specParamVarDecIt =
                myCurrentProcedureOperationEntry.getParameters().iterator();
        Iterator<ParameterVarDec> realizParamVarDecIt =
                myProcedureDec.getParameters().iterator();
        Exp paramEnsuresExp =
                VarExp.getTrueVarExp(procedureLoc.clone(), myTypeGraph);
        while (specParamVarDecIt.hasNext())  {
            // Information from the operation specification
            ProgramParameterEntry entry = specParamVarDecIt.next();
            ParameterVarDec parameterVarDec =
                    (ParameterVarDec) entry.getDefiningElement();
            ParameterMode parameterMode = entry.getParameterMode();
            NameTy nameTy = (NameTy) parameterVarDec.getTy();

            // Obtain the corresponding parameter declaration from
            // operation realization
            ParameterVarDec realizParamVarDec = realizParamVarDecIt.next();

            // Parameter variable and incoming parameter variable
            VarExp parameterExp =
                    Utilities.createVarExp(parameterVarDec.getLocation().clone(),
                            null, parameterVarDec.getName().clone(),
                            nameTy.getMathTypeValue(), null);
            OldExp oldParameterExp =
                    new OldExp(parameterVarDec.getLocation().clone(), parameterExp.clone());
            oldParameterExp.setMathType(nameTy.getMathTypeValue());

            // Query for the type entry in the symbol table
            SymbolTableEntry ste =
                    Utilities.searchProgramType(procedureLoc, nameTy.getQualifier(),
                            nameTy.getName(), myCurrentModuleScope);

            ProgramTypeEntry typeEntry;
            if (ste instanceof ProgramTypeEntry) {
                typeEntry = ste.toProgramTypeEntry(nameTy.getLocation());
            } else {
                typeEntry =
                        ste.toTypeRepresentationEntry(nameTy.getLocation())
                                .getDefiningTypeEntry();
            }

            // The restores mode adds an additional ensures
            // that the outgoing value is equal to the incoming value.
            // Ex: w = #w
            if (parameterMode == ParameterMode.RESTORES) {
                // Set the details for the new location
                Location restoresLoc = realizParamVarDec.getLocation().clone();

                // Need to ensure here that the everything inside the type family
                // is restored at the end of the operation.
                Exp restoresConditionExp = null;
                if (typeEntry.getModelType() instanceof MTCartesian) {
                    MTCartesian cartesian =
                            (MTCartesian) typeEntry.getModelType();
                    List<MTType> elementTypes =
                            cartesian.getComponentTypes();

                    for (int i = 0; i < cartesian.size(); i++) {
                        // Create an Exp for the Cartesian product element
                        VarExp elementExp = Utilities.createVarExp(restoresLoc.clone(), null,
                                new PosSymbol(restoresLoc.clone(), cartesian.getTag(i)),
                                elementTypes.get(i), null);

                        // Create a list of segments. The first element should be the original
                        // parameterExp and oldParameterExp and the second element the cartesian product element.
                        List<Exp> segments = new ArrayList<>();
                        List<Exp> oldSegments = new ArrayList<>();
                        segments.add(parameterExp);
                        oldSegments.add(oldParameterExp);
                        segments.add(elementExp);
                        oldSegments.add(elementExp.clone());

                        // Create the dotted expressions
                        DotExp elementDotExp = new DotExp(restoresLoc.clone(), segments);
                        elementDotExp.setMathType(elementExp.getMathType());
                        DotExp oldElementDotExp = new DotExp(restoresLoc.clone(), oldSegments);
                        oldElementDotExp.setMathType(elementExp.getMathType());

                        // Create an equality expression
                        EqualsExp equalsExp = new EqualsExp(restoresLoc.clone(), elementDotExp, null,
                                Operator.EQUAL, oldElementDotExp);
                        equalsExp.setMathType(myTypeGraph.BOOLEAN);

                        // Add this to our final equals expression
                        if (restoresConditionExp == null) {
                            restoresConditionExp = equalsExp;
                        }
                        else {
                            restoresConditionExp = InfixExp.formConjunct(restoresLoc.clone(),
                                    restoresConditionExp, equalsExp);
                        }
                    }
                }
                else {
                    // Construct an expression using the expression and it's
                    // old expression equivalent.
                    restoresConditionExp =
                            new EqualsExp(restoresLoc.clone(), parameterExp.clone(), null,
                                    Operator.EQUAL, oldParameterExp.clone());
                    restoresConditionExp.setMathType(myTypeGraph.BOOLEAN);
                }

                // Generate the restores parameter ensures clause and
                // store the new location detail.
                if (restoresConditionExp != null) {
                    restoresConditionExp.setLocationDetailModel(new LocationDetailModel(
                            parameterVarDec.getLocation().clone(), realizParamVarDec.getLocation().clone(),
                            "Ensures Clause of " + myCurrentProcedureOperationEntry.getName()
                                    + " (Condition from \"" + parameterMode + "\" parameter mode)"));

                    // Form a conjunct if needed.
                    if (VarExp.isLiteralTrue(paramEnsuresExp)) {
                        paramEnsuresExp = restoresConditionExp;
                    }
                    else {
                        paramEnsuresExp =
                                InfixExp.formConjunct(paramEnsuresExp.getLocation(),
                                        paramEnsuresExp, restoresConditionExp);
                    }
                }
            }
            // The clears mode adds an additional ensures
            // that the outgoing value is the initial value.
            else if (parameterMode == ParameterMode.CLEARS) {
                AssertionClause modifiedInitEnsures;
                if (typeEntry.getDefiningElement() instanceof TypeFamilyDec) {
                    // Parameter variable with known program type
                    TypeFamilyDec type =
                            (TypeFamilyDec) typeEntry.getDefiningElement();
                    AssertionClause initEnsures =
                            type.getInitialization().getEnsures();
                    modifiedInitEnsures =
                            Utilities.getTypeInitEnsuresClause(initEnsures,
                                    procedureLoc.clone(), null,
                                    parameterVarDec.getName(), type.getExemplar(),
                                    typeEntry.getModelType(), null);
                }
                else if (typeEntry.getDefiningElement() instanceof TypeRepresentationDec) {
                    // Obtain the type family declaration and obtain it's initialization ensures.
                    PTRepresentation representationType = (PTRepresentation) typeEntry.getProgramType();
                    TypeFamilyDec type = (TypeFamilyDec) representationType.getFamily().getDefiningElement();
                    AssertionClause initEnsures =
                            type.getInitialization().getEnsures();
                    modifiedInitEnsures =
                            Utilities.getTypeInitEnsuresClause(initEnsures,
                                    procedureLoc.clone(), null,
                                    parameterVarDec.getName(), type.getExemplar(),
                                    typeEntry.getModelType(), null);
                }
                else {
                    VarDec parameterAsVarDec =
                            new VarDec(parameterVarDec.getName(), parameterVarDec.getTy());
                    modifiedInitEnsures =
                            new AssertionClause(procedureLoc.clone(), ClauseType.ENSURES,
                                    Utilities.createInitExp(parameterAsVarDec, myTypeGraph.BOOLEAN));
                }

                // Generate the clears parameter ensures clause and
                // store the new location detail.
                Exp clearsConditionExp = modifiedInitEnsures.getAssertionExp().clone();
                clearsConditionExp.setLocationDetailModel(new LocationDetailModel(
                        parameterVarDec.getLocation().clone(), realizParamVarDec.getLocation().clone(),
                        "Ensures Clause of " + myCurrentProcedureOperationEntry.getName()
                                + " (Condition from \"" + parameterMode + "\" parameter mode)"));

                // Form a conjunct if needed.
                if (VarExp.isLiteralTrue(paramEnsuresExp)) {
                    paramEnsuresExp = clearsConditionExp;
                }
                else {
                    paramEnsuresExp =
                            InfixExp.formConjunct(paramEnsuresExp.getLocation(),
                                    paramEnsuresExp, clearsConditionExp);
                }
            }

            // If the type is a type representation, then our ensures clause
            // should really say something about the conceptual type and not
            // the variable. Must also be in a concept realization and not a
            // local operation.
            if (ste.getDefiningElement() instanceof TypeRepresentationDec && inConceptRealiz && !isLocal) {
                DotExp concVarExp =
                        Utilities.createConcVarExp(
                                new VarDec(realizParamVarDec.getName(), realizParamVarDec.getTy()),
                                parameterVarDec.getMathType(), myTypeGraph.BOOLEAN);
                substitutionParamToConc =
                        addConceptualVariables(parameterExp, oldParameterExp,
                                concVarExp, substitutionParamToConc);
            }
        }

        // Add the operation's ensures clause
        AssertionClause ensuresClause =
                myCurrentProcedureOperationEntry.getEnsuresClause();
        Exp ensuresExp = ensuresClause.getAssertionExp().clone();
        ensuresExp.setLocationDetailModel(new LocationDetailModel(
                ensuresExp.getLocation().clone(), procedureLoc.clone(),
                "Ensures Clause of " + myCurrentProcedureOperationEntry.getName()));

        // Form a conjunct if needed.
        if (VarExp.isLiteralTrue(ensuresExp)) {
            retExp = paramEnsuresExp;
        }
        else {
            if (VarExp.isLiteralTrue(paramEnsuresExp)) {
                retExp = ensuresExp;
            } else {
                retExp =
                        InfixExp.formConjunct(ensuresExp.getLocation(),
                                ensuresExp, paramEnsuresExp);
            }
        }

        // If we are in a concept or enhancement realization,
        // loop through all shared variable declared from the
        // associated concept.
        // Note: If we are in a facility, "getConceptSharedVars()" will return
        //       an empty list.
        List<SharedStateDec> sharedStateDecs =
                myCurrentVerificationContext.getConceptSharedVars();
        for (SharedStateDec stateDec : sharedStateDecs) {
            for (MathVarDec mathVarDec : stateDec.getAbstractStateVars()) {
                // Convert the math variables to variable expressions
                VarExp stateVarExp =
                        Utilities.createVarExp(procedureLoc.clone(), null,
                                mathVarDec.getName(), mathVarDec.getMathType(), null);
                OldExp oldStateVarExp = new OldExp(procedureLoc.clone(), stateVarExp);
                oldStateVarExp.setMathType(stateVarExp.getMathType());

                // Add a "restores" mode to any shared variables not being affected
                if (!Utilities.containsEquivalentExp(myAffectedExps, stateVarExp)) {
                    retExp = createRestoresExpForSharedVars(procedureLoc,
                            stateVarExp, oldStateVarExp, retExp);

                    // If we are in a concept realization, our non-local procedure's
                    // ensures clause should say something about the conceptual
                    // shared variables.
                    if (inConceptRealiz && !isLocal) {
                        // Create the appropriate conceptual versions of the shared variables
                        // and add them to our substitution maps.
                        DotExp concVarExp =
                                Utilities.createConcVarExp(
                                        new VarDec(mathVarDec.getName(), mathVarDec.getTy()),
                                        mathVarDec.getMathType(), myTypeGraph.BOOLEAN);
                        substitutionParamToConc =
                                addConceptualVariables(stateVarExp, oldStateVarExp,
                                        concVarExp, substitutionParamToConc);
                    }
                }
            }
        }

        // Loop through all concept declared types and generate a "restores" ensures
        // clause for non-affected definition variables.
        for (TypeFamilyDec typeFamilyDec : myCurrentVerificationContext.getConceptDeclaredTypes()) {
            for (MathDefVariableDec mathDefVariableDec : typeFamilyDec.getDefinitionVarList()) {
                // Convert the math definition variables to variable expressions
                MathVarDec mathVarDec = mathDefVariableDec.getVariable();
                VarExp defVarExp =
                        Utilities.createVarExp(procedureLoc.clone(), null,
                                mathVarDec.getName(), mathVarDec.getMathType(), null);
                OldExp oldDefVarExp = new OldExp(procedureLoc.clone(), defVarExp);
                oldDefVarExp.setMathType(defVarExp.getMathType());

                // Add a "restores" mode to any definition variables not being affected
                if (!Utilities.containsEquivalentExp(myAffectedExps, defVarExp)) {
                    retExp = createRestoresExpForDefVars(procedureLoc,
                            defVarExp, oldDefVarExp, retExp);

                    // If we are in a concept realization, our non-local procedure's
                    // ensures clause should say something about the conceptual
                    // definition variables.
                    if (inConceptRealiz && !isLocal) {
                        // Create the appropriate conceptual versions of the definition variables
                        // and add them to our substitution maps.
                        DotExp concVarExp =
                                Utilities.createConcVarExp(
                                        new VarDec(mathVarDec.getName(), mathVarDec.getTy()),
                                        mathVarDec.getMathType(), myTypeGraph.BOOLEAN);
                        substitutionParamToConc =
                                addConceptualVariables(defVarExp, oldDefVarExp,
                                        concVarExp, substitutionParamToConc);
                    }
                }
            }
        }

        // Loop through all instantiated facility's shared variables and
        // generate a "restores" ensures clause for non-affected shared variables.
        for (InstantiatedFacilityDecl facilityDecl :
                myCurrentVerificationContext.getProcessedInstFacilityDecls()) {
            for (SharedStateDec stateDec : facilityDecl.getConceptSharedStates()) {
                for (MathVarDec mathVarDec : stateDec.getAbstractStateVars()) {
                    // Convert the math variables to variable expressions
                    VarExp stateVarExp =
                            Utilities.createVarExp(procedureLoc.clone(),
                                    facilityDecl.getInstantiatedFacilityName(),
                                    mathVarDec.getName(), mathVarDec.getMathType(), null);
                    OldExp oldStateVarExp = new OldExp(procedureLoc.clone(), stateVarExp);
                    oldStateVarExp.setMathType(stateVarExp.getMathType());

                    // Add a "restores" mode to any shared variables not being affected
                    if (!Utilities.containsEquivalentExp(myAffectedExps, stateVarExp)) {
                        retExp = createRestoresExpForSharedVars(procedureLoc,
                                stateVarExp, oldStateVarExp, retExp);
                    }
                }
            }

            // TODO: Do something about any definition variables from the importing concept.
        }

        // Apply any substitution and return the modified expression
        return retExp.substitute(substitutionParamToConc);
    }

    /**
     * <p>An helper method that creates a new {@code ensures} expression
     * that includes a {@code restores} mode clause for the given
     * math definition variable.</p>
     *
     * @param procedureLoc The current procedure's location.
     * @param defVarExp A math definition variable as a {@link VarExp}.
     * @param oldDefVarExp The incoming value of {@code defVarExp}.
     * @param ensuresExp The current ensures clause we are building.
     *
     * @return A modified {@code ensures} clause with the new
     * {@code restores} expression.
     */
    private Exp createRestoresExpForDefVars(Location procedureLoc,
            VarExp defVarExp, OldExp oldDefVarExp, Exp ensuresExp) {
        // Construct an expression using the expression and it's
        // old expression equivalent.
        Exp restoresConditionExp =
                new EqualsExp(procedureLoc.clone(), defVarExp.clone(), null,
                        Operator.EQUAL, oldDefVarExp.clone());
        restoresConditionExp.setMathType(myTypeGraph.BOOLEAN);
        restoresConditionExp
                .setLocationDetailModel(new LocationDetailModel(procedureLoc
                        .clone(), procedureLoc.clone(), "Ensures Clause of "
                        + myCurrentProcedureOperationEntry.getName()
                        + " (Condition from Non-Affected Definition Variable)"));

        // Form a conjunct if needed.
        Exp retExp;
        if (VarExp.isLiteralTrue(ensuresExp)) {
            retExp = restoresConditionExp;
        }
        else {
            retExp =
                    InfixExp.formConjunct(ensuresExp.getLocation(), ensuresExp,
                            restoresConditionExp);
        }

        return retExp;
    }

    /**
     * <p>An helper method that creates a new {@code ensures} expression
     * that includes a {@code restores} mode clause for the given
     * global state variable.</p>
     *
     * @param procedureLoc The current procedure's location.
     * @param stateVarExp A global state variable as a {@link VarExp}.
     * @param oldStateVarExp The incoming value of {@code stateVarExp}.
     * @param ensuresExp The current ensures clause we are building.
     *
     * @return A modified {@code ensures} clause with the new
     * {@code restores} expression.
     */
    private Exp createRestoresExpForSharedVars(Location procedureLoc,
            VarExp stateVarExp, OldExp oldStateVarExp, Exp ensuresExp) {
        // Construct an expression using the expression and it's
        // old expression equivalent.
        Exp restoresConditionExp =
                new EqualsExp(procedureLoc.clone(), stateVarExp.clone(), null,
                        Operator.EQUAL, oldStateVarExp.clone());
        restoresConditionExp.setMathType(myTypeGraph.BOOLEAN);
        restoresConditionExp.setLocationDetailModel(new LocationDetailModel(
                procedureLoc.clone(), procedureLoc.clone(),
                "Ensures Clause of "
                        + myCurrentProcedureOperationEntry.getName()
                        + " (Condition from Non-Affected Shared Variable)"));

        // Form a conjunct if needed.
        Exp retExp;
        if (VarExp.isLiteralTrue(ensuresExp)) {
            retExp = restoresConditionExp;
        }
        else {
            retExp =
                    InfixExp.formConjunct(ensuresExp.getLocation(), ensuresExp,
                            restoresConditionExp);
        }

        return retExp;
    }
}