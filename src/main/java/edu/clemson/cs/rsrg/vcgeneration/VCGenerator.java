/*
 * VCGenerator.java
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
package edu.clemson.cs.rsrg.vcgeneration;

import edu.clemson.cs.r2jt.rewriteprover.immutableadts.ImmutableList;
import edu.clemson.cs.rsrg.absyn.clauses.AssertionClause;
import edu.clemson.cs.rsrg.absyn.declarations.facilitydecl.FacilityDec;
import edu.clemson.cs.rsrg.absyn.declarations.moduledecl.*;
import edu.clemson.cs.rsrg.absyn.declarations.operationdecl.OperationDec;
import edu.clemson.cs.rsrg.absyn.declarations.operationdecl.OperationProcedureDec;
import edu.clemson.cs.rsrg.absyn.declarations.operationdecl.ProcedureDec;
import edu.clemson.cs.rsrg.absyn.declarations.typedecl.AbstractTypeRepresentationDec;
import edu.clemson.cs.rsrg.absyn.declarations.typedecl.TypeFamilyDec;
import edu.clemson.cs.rsrg.absyn.declarations.variabledecl.ParameterVarDec;
import edu.clemson.cs.rsrg.absyn.declarations.variabledecl.VarDec;
import edu.clemson.cs.rsrg.absyn.expressions.Exp;
import edu.clemson.cs.rsrg.absyn.expressions.mathexpr.VarExp;
import edu.clemson.cs.rsrg.absyn.items.mathitems.SpecInitFinalItem;
import edu.clemson.cs.rsrg.absyn.items.programitems.EnhancementSpecRealizItem;
import edu.clemson.cs.rsrg.absyn.rawtypes.NameTy;
import edu.clemson.cs.rsrg.absyn.statements.*;
import edu.clemson.cs.rsrg.absyn.statements.MemoryStmt.StatementType;
import edu.clemson.cs.rsrg.init.CompileEnvironment;
import edu.clemson.cs.rsrg.init.flag.Flag;
import edu.clemson.cs.rsrg.init.flag.FlagDependencies;
import edu.clemson.cs.rsrg.parsing.data.Location;
import edu.clemson.cs.rsrg.parsing.data.LocationDetailModel;
import edu.clemson.cs.rsrg.parsing.data.PosSymbol;
import edu.clemson.cs.rsrg.statushandling.exception.SourceErrorException;
import edu.clemson.cs.rsrg.treewalk.TreeWalkerVisitor;
import edu.clemson.cs.rsrg.typeandpopulate.entry.*;
import edu.clemson.cs.rsrg.typeandpopulate.exception.NoSuchSymbolException;
import edu.clemson.cs.rsrg.typeandpopulate.programtypes.PTGeneric;
import edu.clemson.cs.rsrg.typeandpopulate.programtypes.PTType;
import edu.clemson.cs.rsrg.typeandpopulate.query.EntryTypeQuery;
import edu.clemson.cs.rsrg.typeandpopulate.symboltables.MathSymbolTable.FacilityStrategy;
import edu.clemson.cs.rsrg.typeandpopulate.symboltables.MathSymbolTable.ImportStrategy;
import edu.clemson.cs.rsrg.typeandpopulate.symboltables.MathSymbolTableBuilder;
import edu.clemson.cs.rsrg.typeandpopulate.symboltables.ModuleScope;
import edu.clemson.cs.rsrg.typeandpopulate.typereasoning.TypeGraph;
import edu.clemson.cs.rsrg.typeandpopulate.utilities.ModuleIdentifier;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.ProofRuleApplication;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.declaration.FacilityDeclRule;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.declaration.GenericTypeVariableDeclRule;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.declaration.ProcedureDeclRule;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.statement.*;
import edu.clemson.cs.rsrg.vcgeneration.sequents.Sequent;
import edu.clemson.cs.rsrg.vcgeneration.utilities.AssertiveCodeBlock;
import edu.clemson.cs.rsrg.vcgeneration.utilities.Utilities;
import edu.clemson.cs.rsrg.vcgeneration.utilities.VerificationCondition;
import edu.clemson.cs.rsrg.vcgeneration.utilities.VerificationContext;
import edu.clemson.cs.rsrg.vcgeneration.utilities.formaltoactual.InstantiatedFacilityDecl;
import edu.clemson.cs.rsrg.vcgeneration.utilities.helperstmts.FinalizeVarStmt;
import edu.clemson.cs.rsrg.vcgeneration.utilities.helperstmts.InitializeVarStmt;
import edu.clemson.cs.rsrg.vcgeneration.utilities.helperstmts.VCConfirmStmt;
import java.util.*;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

/**
 * <p>This class generates verification conditions (VCs) using the provided
 * RESOLVE abstract syntax tree. This visitor logic is implemented as
 * a {@link TreeWalkerVisitor}.</p>
 *
 * @author Heather Keown Harton
 * @author Yu-Shan Sun
 * @author Nighat Yasmin
 * @version 3.0
 */
public class VCGenerator extends TreeWalkerVisitor {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /** <p>The symbol table we are currently building.</p> */
    private final MathSymbolTableBuilder myBuilder;

    /**
     * <p>The current job's compilation environment
     * that stores all necessary objects and flags.</p>
     */
    private final CompileEnvironment myCompileEnvironment;

    /**
     * <p>The verification context for the file we are generating
     * {@code VCs} for.</p>
     */
    private VerificationContext myCurrentVerificationContext;

    /**
     * <p>The module scope for the file we are generating
     * {@code VCs} for.</p>
     */
    private ModuleScope myCurrentModuleScope;

    /**
     * <p>This is the math type graph that indicates relationship
     * between different math types.</p>
     */
    private final TypeGraph myTypeGraph;

    /** <p>The list of processed {@link InstantiatedFacilityDecl}. </p> */
    private final List<InstantiatedFacilityDecl> myProcessedInstFacilityDecls;

    // -----------------------------------------------------------
    // Operation Declaration-Related
    // -----------------------------------------------------------

    /**
     * <p>While walking a procedure, this stores all the local {@link VarDec VarDec's}
     * {@code finalization} specification item if we were able to generate one.</p>
     */
    private final Map<VarDec, SpecInitFinalItem> myVariableSpecFinalItems;

    // -----------------------------------------------------------
    // VC Generation-Related
    // -----------------------------------------------------------

    /**
     * <p>The current {@link AssertiveCodeBlock} that the inner declarations
     * will operate on.</p>
     */
    private AssertiveCodeBlock myCurrentAssertiveCodeBlock;

    /**
     * <p>All the completed {@link AssertiveCodeBlock AssertiveCodeBlocks}
     * that only contain the final {@link Sequent Sequents}.</p>
     */
    private final List<AssertiveCodeBlock> myFinalAssertiveCodeBlocks;

    /**
     * <p>All the {@link AssertiveCodeBlock AssertiveCodeBlocks} generated by
     * the various different declaration and statements that still needs more
     * proof rule applications.</p>
     */
    private final Deque<AssertiveCodeBlock> myIncompleteAssertiveCodeBlocks;

    // -----------------------------------------------------------
    // Output-Related
    // -----------------------------------------------------------

    /** <p>String template for the each of the assertive code blocks.</p> */
    private final Map<AssertiveCodeBlock, ST> myAssertiveCodeBlockModels;

    /** <p>String template groups for storing all the VC generation details.</p> */
    private final STGroup mySTGroup;

    /** <p>String template for the VC generation details model.</p> */
    private final ST myVCGenDetailsModel;

    // ===========================================================
    // Flag Strings
    // ===========================================================

    private static final String FLAG_SECTION_NAME = "VCGenerator";
    private static final String FLAG_DESC_VERIFY_VC = "Generate VCs.";
    private static final String FLAG_DESC_PERF_VC = "Generate Performance VCs";
    private static final String FLAG_DESC_ADD_CONSTRAINT =
            "Add constraints as givens.";

    // ===========================================================
    // Flags
    // ===========================================================

    /**
     * <p>Tells the compiler to generate VCs.</p>
     */
    public static final Flag FLAG_VERIFY_VC =
            new Flag(FLAG_SECTION_NAME, "VCs", FLAG_DESC_VERIFY_VC);

    /**
     * <p>Tells the compiler to generate performance VCs.</p>
     */
    private static final Flag FLAG_PVCS_VC =
            new Flag(FLAG_SECTION_NAME, "PVCs", FLAG_DESC_PERF_VC);

    /**
     * <p>Tells the compiler to generate VCs.</p>
     */
    public static final Flag FLAG_ADD_CONSTRAINT =
            new Flag(FLAG_SECTION_NAME, "addConstraints",
                    FLAG_DESC_ADD_CONSTRAINT);

    /**
     * <p>Add all the required and implied flags for the {@code VCGenerator}.</p>
     */
    public static void setUpFlags() {
        FlagDependencies.addImplies(FLAG_PVCS_VC, FLAG_VERIFY_VC);

        // Make sure we have one of these on.
        Flag[] dependencies = { FLAG_VERIFY_VC, FLAG_PVCS_VC };
        FlagDependencies.addRequires(FLAG_ADD_CONSTRAINT, dependencies);
    }

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * <p>This creates an object that overrides methods to generate VCs from
     * a {@link ModuleDec}.</p>
     *
     * @param builder A scope builder for a symbol table.
     * @param compileEnvironment The current job's compilation environment
     *                           that stores all necessary objects and flags.
     */
    public VCGenerator(MathSymbolTableBuilder builder, CompileEnvironment compileEnvironment) {
        myAssertiveCodeBlockModels = new LinkedHashMap<>();
        myBuilder = builder;
        myCompileEnvironment = compileEnvironment;
        myFinalAssertiveCodeBlocks = new LinkedList<>();
        myIncompleteAssertiveCodeBlocks = new LinkedList<>();
        myProcessedInstFacilityDecls = new LinkedList<>();
        mySTGroup = new STGroupFile("templates/VCGenVerboseOutput.stg");
        myTypeGraph = myBuilder.getTypeGraph();
        myVariableSpecFinalItems = new LinkedHashMap<>();
        myVCGenDetailsModel = mySTGroup.getInstanceOf("outputVCGenDetails");
    }

    // ===========================================================
    // Visitor Methods
    // ===========================================================

    // -----------------------------------------------------------
    // Module Declarations
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed before visiting a {@link ModuleDec}.</p>
     *
     * @param dec A module declaration.
     */
    @Override
    public final void preModuleDec(ModuleDec dec) {
        // Set the current module scope
        try {
            myCurrentModuleScope =
                    myBuilder.getModuleScope(new ModuleIdentifier(dec));
            myCurrentVerificationContext =
                    new VerificationContext(dec.getName(), myCurrentModuleScope,
                            myBuilder, myCompileEnvironment);

            // Apply the facility declaration rule to imported facility declarations.
            List<FacilityEntry> results =
                    myCurrentModuleScope
                            .query(new EntryTypeQuery<>(
                                    FacilityEntry.class,
                                    ImportStrategy.IMPORT_NAMED,
                                    FacilityStrategy.FACILITY_INSTANTIATE));

            for (SymbolTableEntry s : results) {
                // YS: Only deal with imported facility declarations right now.
                //     The facility declarations from this module will be handled in
                //     postFacilityDec.
                if (s.getSourceModuleIdentifier().compareTo(
                        myCurrentModuleScope.getModuleIdentifier()) != 0) {
                    // Do all the facility declaration logic, but don't add this
                    // to our incomplete assertive code stack. We shouldn't need to
                    // verify facility declarations that are imported.
                    FacilityDec facDec =
                            (FacilityDec) s.toFacilityEntry(dec.getLocation())
                                    .getDefiningElement();

                    // Create a new model for this assertive code block
                    ST blockModel = mySTGroup.getInstanceOf("outputAssertiveCodeBlock");
                    blockModel.add("blockName", dec.getName());

                    FacilityDeclRule ruleApplication =
                            new FacilityDeclRule(facDec, false,
                                    myCurrentConceptDeclaredTypes,
                                    myLocalRepresentationTypeDecs,
                                    myProcessedInstFacilityDecls,
                                    myBuilder, myCurrentModuleScope,
                                    new AssertiveCodeBlock(facDec.getName(), facDec, myTypeGraph),
                                    mySTGroup, blockModel);
                    ruleApplication.applyRule();

                    // Store this facility's InstantiatedFacilityDecl for future use
                    myProcessedInstFacilityDecls.add(ruleApplication.getInstantiatedFacilityDecl());

                    // Store all requires/constraint from the imported concept
                    PosSymbol conceptName = facDec.getConceptName();
                    ModuleIdentifier coId = new ModuleIdentifier(conceptName.getName());
                    myCurrentVerificationContext.storeConceptAssertionClauses(
                            conceptName.getLocation(), coId, true);

                    // Store all requires/constraint from the imported concept realization
                    // if it is not externally realized
                    if (!facDec.getExternallyRealizedFlag()) {
                        PosSymbol conceptRealizName = facDec.getConceptRealizName();
                        ModuleIdentifier coRealizId = new ModuleIdentifier(conceptRealizName.getName());
                        myCurrentVerificationContext.storeConceptRealizAssertionClauses(
                                conceptRealizName.getLocation(), coRealizId, true);
                    }

                    for (EnhancementSpecRealizItem specRealizItem : facDec.getEnhancementRealizPairs()) {
                        // Store all requires/constraint from the imported enhancement(s)
                        PosSymbol enhancementName = specRealizItem.getEnhancementName();
                        ModuleIdentifier enId = new ModuleIdentifier(enhancementName.getName());
                        myCurrentVerificationContext.storeEnhancementAssertionClauses(
                                enhancementName.getLocation(), enId, true);

                        // Store all requires/constraint from the imported enhancement realization(s)
                        PosSymbol enhancementRealizName = specRealizItem.getEnhancementRealizName();
                        ModuleIdentifier enRealizId = new ModuleIdentifier(enhancementRealizName.getName());
                        myCurrentVerificationContext.storeEnhancementRealizAssertionClauses(
                                enhancementRealizName.getLocation(), enRealizId, true);
                    }
                }
            }
        }
        catch (NoSuchSymbolException e) {
            Utilities.noSuchModule(dec.getLocation());
        }
    }

    /**
     * <p>Code that gets executed after visiting a {@link ModuleDec}.</p>
     *
     * @param dec A module declaration.
     */
    @Override
    public final void postModuleDec(ModuleDec dec) {
        // Loop through our incomplete assertive code blocks until it is empty
        while (!myIncompleteAssertiveCodeBlocks.isEmpty()) {
            // Use the first assertive code block in the incomplete blocks list
            // as our current assertive code block.
            myCurrentAssertiveCodeBlock =
                    myIncompleteAssertiveCodeBlocks.removeFirst();

            applyStatementRules(myCurrentAssertiveCodeBlock);

            // Render the assertive block model
            ST blockModel =
                    myAssertiveCodeBlockModels
                            .remove(myCurrentAssertiveCodeBlock);
            myVCGenDetailsModel.add("assertiveCodeBlocks", blockModel.render());

            // Add this to our final assertive code block list
            myFinalAssertiveCodeBlocks.add(myCurrentAssertiveCodeBlock);

            // Set the current assertive code block to null
            myCurrentAssertiveCodeBlock = null;
        }

        // Assign a name to all of the VCs
        int blockCount = 0;
        for (AssertiveCodeBlock block : myFinalAssertiveCodeBlocks) {
            // Obtain the final list of vcs
            int vcCount = 1;
            List<VerificationCondition> vcs = block.getVCs();
            List<VerificationCondition> namedVCs = new ArrayList<>(vcs.size());
            for (VerificationCondition vc : vcs) {
                namedVCs.add(new VerificationCondition(vc.getLocation(),
                        blockCount + "_" + vcCount,
                        vc.getSequent(), vc.getHasImpactingReductionFlag(),
                        vc.getLocationDetailModel()));
                vcCount++;
            }

            // Store the named VCs and increase the block number
            block.setVCs(namedVCs);
            blockCount++;
        }
    }

    // -----------------------------------------------------------
    // Concept Module
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed before visiting a {@link ConceptModuleDec}.</p>
     *
     * @param concept A concept module declaration.
     */
    @Override
    public final void preConceptModuleDec(ConceptModuleDec concept) {
        PosSymbol conceptName = concept.getName();

        // Store the concept requires and constraint clauses
        myCurrentVerificationContext.storeConceptAssertionClauses(conceptName
                .getLocation(), new ModuleIdentifier(conceptName.getName()),
                false);

        // Add to VC detail model
        ST header =
                mySTGroup.getInstanceOf("outputConceptHeader").add(
                        "conceptName", conceptName.getName());
        myVCGenDetailsModel.add("fileHeader", header.render());
    }

    // -----------------------------------------------------------
    // Enhancement Realization Module
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed before visiting an {@link EnhancementRealizModuleDec}.</p>
     *
     * @param enhancementRealization An enhancement realization module declaration.
     */
    @Override
    public final void preEnhancementRealizModuleDec(
            EnhancementRealizModuleDec enhancementRealization) {
        PosSymbol enhancementRealizName = enhancementRealization.getName();

        // Store the enhancement realization requires clause
        myCurrentVerificationContext.storeEnhancementAssertionClauses(
                enhancementRealizName.getLocation(), new ModuleIdentifier(
                        enhancementRealizName.getName()), false);

        // Store all requires/constraint from the imported concept
        PosSymbol conceptName = enhancementRealization.getConceptName();
        ModuleIdentifier coId = new ModuleIdentifier(conceptName.getName());
        myCurrentVerificationContext.storeConceptAssertionClauses(conceptName
                .getLocation(), coId, false);

        // Store all the type families declared in the concept
        myCurrentVerificationContext.storeConceptTypeFamilyDecs(conceptName
                .getLocation(), coId);

        // Store all requires/constraint from the imported enhancement
        PosSymbol enhancementName = enhancementRealization.getEnhancementName();
        ModuleIdentifier enId = new ModuleIdentifier(enhancementName.getName());
        myCurrentVerificationContext.storeEnhancementAssertionClauses(
                enhancementName.getLocation(), enId, false);

        // Add to VC detail model
        ST header =
                mySTGroup.getInstanceOf("outputEnhancementRealizHeader").add(
                        "realizName", enhancementRealizName.getName()).add(
                        "enhancementName", enhancementName.getName()).add(
                        "conceptName", conceptName.getName());
        myVCGenDetailsModel.add("fileHeader", header.render());
    }

    // -----------------------------------------------------------
    // Facility Module
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed before visiting a {@link FacilityModuleDec}.</p>
     *
     * @param facility A concept module declaration.
     */
    @Override
    public final void preFacilityModuleDec(FacilityModuleDec facility) {
        PosSymbol facilityName = facility.getName();

        // Store the facility requires clause
        myCurrentVerificationContext.storeFacilityModuleAssertionClauses(
                facilityName.getLocation(), new ModuleIdentifier(facilityName
                        .getName()));

        // Add to VC detail model
        ST header =
                mySTGroup.getInstanceOf("outputFacilityHeader").add(
                        "facilityName", facilityName.getName());
        myVCGenDetailsModel.add("fileHeader", header.render());
    }

    // -----------------------------------------------------------
    // Facility-Related
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed after visiting a {@link FacilityDec}.</p>
     *
     * @param dec A facility declaration.
     */
    @Override
    public final void postFacilityDec(FacilityDec dec) {
        // Create a new assertive code block
        myCurrentAssertiveCodeBlock =
                new AssertiveCodeBlock(dec.getName(), dec, myTypeGraph);

        // Create the top most level assume statement and
        // add it to the assertive code block as the first statement
        AssumeStmt topLevelAssumeStmt =
                new AssumeStmt(dec.getLocation().clone(),
                        myCurrentVerificationContext
                                .createTopLevelAssumeExpFromContext(dec
                                        .getLocation()), false);
        myCurrentAssertiveCodeBlock.addStatement(topLevelAssumeStmt);

        // Create a new model for this assertive code block
        ST blockModel = mySTGroup.getInstanceOf("outputAssertiveCodeBlock");
        blockModel.add("blockName", dec.getName());

        // Apply facility declaration rule
        FacilityDeclRule declRule =
                new FacilityDeclRule(dec, true, myCurrentConceptDeclaredTypes,
                        myLocalRepresentationTypeDecs,
                        myProcessedInstFacilityDecls, myBuilder,
                        myCurrentModuleScope, myCurrentAssertiveCodeBlock,
                        mySTGroup, blockModel);
        declRule.applyRule();

        // Store this facility's InstantiatedFacilityDecl for future use
        myProcessedInstFacilityDecls
                .add(declRule.getInstantiatedFacilityDecl());

        // Update the current assertive code block and its associated block model.
        myCurrentAssertiveCodeBlock =
                declRule.getAssertiveCodeBlocks().getFirst();
        myAssertiveCodeBlockModels.put(myCurrentAssertiveCodeBlock, declRule
                .getBlockModel());

        // Add this as a new incomplete assertive code block
        myIncompleteAssertiveCodeBlocks.add(myCurrentAssertiveCodeBlock);
    }

    // -----------------------------------------------------------
    // Operation-Related
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed before visiting an {@link OperationProcedureDec}.</p>
     *
     * @param dec A local operation with procedure declaration.
     */
    @Override
    public final void preOperationProcedureDec(OperationProcedureDec dec) {
        // Store the associated OperationEntry for future use
        List<PTType> argTypes = new LinkedList<>();
        for (ParameterVarDec p : dec.getWrappedOpDec().getParameters()) {
            argTypes.add(p.getTy().getProgramType());
        }
        OperationEntry correspondingOperation =
                Utilities.searchOperation(dec.getLocation(), null, dec
                        .getName(), argTypes, myCurrentModuleScope);

        // TODO: Add the performance logic
        // Obtain the performance duration clause
        /*if (myInstanceEnvironment.flags.isFlagSet(FLAG_ALTPVCS_VC)) {
            myCurrentOperationProfileEntry =
                    Utilities.searchOperationProfile(dec.getLocation(), null,
                            dec.getName(), argTypes, myCurrentModuleScope);
        }*/

        // Create a new assertive code block
        if (dec.getRecursive()) {
            // Store any decreasing clauses for future use
            myCurrentAssertiveCodeBlock =
                    new AssertiveCodeBlock(dec.getName(), dec, correspondingOperation,
                            dec.getDecreasing().getAssertionExp(), myTypeGraph);
        }
        else {
            myCurrentAssertiveCodeBlock =
                    new AssertiveCodeBlock(dec.getName(), dec, correspondingOperation,
                            myTypeGraph);
        }

        // Create the top most level assume statement, replace any facility formal
        // with actual and add it to the assertive code block as the first statement.
        Exp topLevelAssumeExp =
                createTopLevelAssumeExpForProcedureDec(dec.getLocation(),
                        myCurrentAssertiveCodeBlock, correspondingOperation,
                        myProcessedInstFacilityDecls, myCompileEnvironment.flags.isFlagSet(FLAG_ADD_CONSTRAINT),
                        true);
        AssumeStmt topLevelAssumeStmt =
                new AssumeStmt(dec.getLocation().clone(), topLevelAssumeExp, false);
        myCurrentAssertiveCodeBlock.addStatement(topLevelAssumeStmt);

        // Create Remember statement
        MemoryStmt rememberStmt = new MemoryStmt(dec.getLocation().clone(), StatementType.REMEMBER);
        myCurrentAssertiveCodeBlock.addStatement(rememberStmt);

        // TODO: NY - Add any procedure duration clauses

        // Create a new model for this assertive code block
        ST blockModel = mySTGroup.getInstanceOf("outputAssertiveCodeBlock");
        blockModel.add("blockName", dec.getName());
        ST stepModel = mySTGroup.getInstanceOf("outputVCGenStep");
        stepModel.add("proofRuleName", "Procedure Declaration Rule (Part 1)").add(
                "currentStateOfBlock", myCurrentAssertiveCodeBlock);
        blockModel.add("vcGenSteps", stepModel.render());
        myAssertiveCodeBlockModels.put(myCurrentAssertiveCodeBlock, blockModel);
    }

    /**
     * <p>Code that gets executed after visiting an {@link OperationProcedureDec}.</p>
     *
     * @param dec A local operation with procedure declaration.
     */
    @Override
    public final void postOperationProcedureDec(OperationProcedureDec dec) {
        // Apply procedure declaration rule
        // TODO: Recheck logic to make sure everything still works!
        OperationDec wrappedOpDec = dec.getWrappedOpDec();
        ProcedureDec procedureDec =
                new ProcedureDec(dec.getName(), wrappedOpDec.getParameters(),
                        wrappedOpDec.getReturnTy(), wrappedOpDec
                                .getAffectedVars(), dec.getDecreasing(), dec
                                .getFacilities(), dec.getVariables(), dec
                                .getStatements(), dec.getRecursive());
        ProofRuleApplication declRule =
                new ProcedureDeclRule(procedureDec, myVariableSpecFinalItems,
                        myCurrentConceptDeclaredTypes,
                        myLocalRepresentationTypeDecs,
                        myProcessedInstFacilityDecls, myBuilder,
                        myCurrentModuleScope, myCurrentAssertiveCodeBlock,
                        mySTGroup, myAssertiveCodeBlockModels
                                .remove(myCurrentAssertiveCodeBlock));
        declRule.applyRule();

        // Update the current assertive code block and its associated block model.
        myCurrentAssertiveCodeBlock =
                declRule.getAssertiveCodeBlocks().getFirst();
        myAssertiveCodeBlockModels.put(myCurrentAssertiveCodeBlock, declRule
                .getBlockModel());

        // Add this as a new incomplete assertive code block
        myIncompleteAssertiveCodeBlocks.add(myCurrentAssertiveCodeBlock);

        myVariableSpecFinalItems.clear();
        myCurrentAssertiveCodeBlock = null;
    }

    /**
     * <p>Code that gets executed before visiting a {@link ProcedureDec}.</p>
     *
     * @param dec A procedure declaration.
     */
    @Override
    public final void preProcedureDec(ProcedureDec dec) {
        // Store the associated OperationEntry for future use
        List<PTType> argTypes = new LinkedList<>();
        for (ParameterVarDec p : dec.getParameters()) {
            argTypes.add(p.getTy().getProgramType());
        }
        OperationEntry correspondingOperation =
                Utilities.searchOperation(dec.getLocation(), null, dec
                        .getName(), argTypes, myCurrentModuleScope);

        // TODO: Add the performance logic
        // Obtain the performance duration clause
        /*if (myInstanceEnvironment.flags.isFlagSet(FLAG_ALTPVCS_VC)) {
            myCurrentOperationProfileEntry =
                    Utilities.searchOperationProfile(dec.getLocation(), null,
                            dec.getName(), argTypes, myCurrentModuleScope);
        }*/

        // Check to see if this a local operation
        boolean isLocal =
                Utilities.isLocationOperation(dec.getName().getName(),
                        myCurrentModuleScope);

        // Create a new assertive code block
        if (dec.getRecursive()) {
            // Store any decreasing clauses for future use
            myCurrentAssertiveCodeBlock =
                    new AssertiveCodeBlock(dec.getName(), dec, correspondingOperation,
                            dec.getDecreasing().getAssertionExp(), myTypeGraph);
        }
        else {
            myCurrentAssertiveCodeBlock =
                    new AssertiveCodeBlock(dec.getName(), dec, correspondingOperation,
                            myTypeGraph);
        }

        // Create the top most level assume statement, replace any facility formal
        // with actual and add it to the assertive code block as the first statement.
        // TODO: Add convention/correspondence if we are in a concept realization and it isn't local
        Exp topLevelAssumeExp =
                createTopLevelAssumeExpForProcedureDec(dec.getLocation(),
                        myCurrentAssertiveCodeBlock, correspondingOperation,
                        myProcessedInstFacilityDecls,
                        myCompileEnvironment.flags.isFlagSet(FLAG_ADD_CONSTRAINT), isLocal);
        AssumeStmt topLevelAssumeStmt =
                new AssumeStmt(dec.getLocation().clone(), topLevelAssumeExp, false);
        myCurrentAssertiveCodeBlock.addStatement(topLevelAssumeStmt);

        // Create Remember statement
        MemoryStmt rememberStmt = new MemoryStmt(dec.getLocation().clone(), StatementType.REMEMBER);
        myCurrentAssertiveCodeBlock.addStatement(rememberStmt);

        // TODO: NY - Add any procedure duration clauses

        // Create a new model for this assertive code block
        ST blockModel = mySTGroup.getInstanceOf("outputAssertiveCodeBlock");
        blockModel.add("blockName", dec.getName());
        ST stepModel = mySTGroup.getInstanceOf("outputVCGenStep");
        stepModel.add("proofRuleName", "Procedure Declaration Rule (Part 1)").add(
                "currentStateOfBlock", myCurrentAssertiveCodeBlock);
        blockModel.add("vcGenSteps", stepModel.render());
        myAssertiveCodeBlockModels.put(myCurrentAssertiveCodeBlock, blockModel);
    }

    /**
     * <p>Code that gets executed after visiting a {@link ProcedureDec}.</p>
     *
     * @param dec A procedure declaration.
     */
    @Override
    public final void postProcedureDec(ProcedureDec dec) {
        // Apply procedure declaration rule
        // TODO: Recheck logic to make sure everything still works!
        ProofRuleApplication declRule =
                new ProcedureDeclRule(dec, myVariableSpecFinalItems,
                        myCurrentConceptDeclaredTypes,
                        myLocalRepresentationTypeDecs,
                        myProcessedInstFacilityDecls, myBuilder,
                        myCurrentModuleScope, myCurrentAssertiveCodeBlock,
                        mySTGroup, myAssertiveCodeBlockModels
                                .remove(myCurrentAssertiveCodeBlock));
        declRule.applyRule();

        // Update the current assertive code block and its associated block model.
        myCurrentAssertiveCodeBlock =
                declRule.getAssertiveCodeBlocks().getFirst();
        myAssertiveCodeBlockModels.put(myCurrentAssertiveCodeBlock, declRule
                .getBlockModel());

        // Add this as a new incomplete assertive code block
        myIncompleteAssertiveCodeBlocks.add(myCurrentAssertiveCodeBlock);

        myVariableSpecFinalItems.clear();
        myCurrentAssertiveCodeBlock = null;
    }

    // -----------------------------------------------------------
    // Type Realization-Related
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed after visiting an {@link AbstractTypeRepresentationDec}.</p>
     *
     * @param dec A type representation declaration.
     */
    @Override
    public final void postAbstractTypeRepresentationDec(
            AbstractTypeRepresentationDec dec) {
        myCurrentVerificationContext.storeLocalTypeRepresentationDec(dec);
    }

    /**
     * <p>Code that gets executed after visiting a {@link TypeFamilyDec}.</p>
     *
     * @param dec A type family declared in a {@code Concept}.
     */
    @Override
    public final void postTypeFamilyDec(TypeFamilyDec dec) {
        myCurrentVerificationContext.storeConceptTypeFamilyDec(dec);
    }

    // -----------------------------------------------------------
    // Variable Declaration-Related
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed after visiting a {@link VarDec}.</p>
     *
     * @param dec A variable declaration.
     */
    @Override
    public final void postVarDec(VarDec dec) {
        // Ty should always be a NameTy
        if (dec.getTy() instanceof NameTy) {
            NameTy nameTy = (NameTy) dec.getTy();

            // Query for the type entry in the symbol table
            SymbolTableEntry ste =
                    Utilities.searchProgramType(nameTy.getLocation(), nameTy
                            .getQualifier(), nameTy.getName(),
                            myCurrentModuleScope);
            ProgramTypeEntry typeEntry;
            if (ste instanceof ProgramTypeEntry) {
                typeEntry = ste.toProgramTypeEntry(nameTy.getLocation());
            }
            else {
                typeEntry =
                        ste.toTypeRepresentationEntry(nameTy.getLocation())
                                .getDefiningTypeEntry();
            }

            // Check to see if the variable's type is known or it is generic.
            ST blockModel =
                    myAssertiveCodeBlockModels
                            .remove(myCurrentAssertiveCodeBlock);
            if (typeEntry.getDefiningElement() instanceof TypeFamilyDec) {
                // YS: Simply create the proper variable initialization statement that
                //     allow us to deal with generating question mark variables
                //     and duration logic when we backtrack through the code.
                myCurrentAssertiveCodeBlock.addStatement(new InitializeVarStmt(
                        dec));

                // Variable declaration rule for known types
                /*TypeFamilyDec type =
                        (TypeFamilyDec) typeEntry.getDefiningElement();
                AssertionClause initEnsures =
                        type.getInitialization().getEnsures();
                AssertionClause modifiedInitEnsures =
                        Utilities.getTypeInitEnsuresClause(initEnsures, dec
                                .getLocation(), null, dec.getName(), type
                                .getExemplar(), typeEntry.getModelType(), null);

                // TODO: Logic for types in concept realizations

                declRule =
                        new KnownTypeVariableDeclRule(dec, modifiedInitEnsures,
                                myCurrentAssertiveCodeBlock, mySTGroup,
                                myAssertiveCodeBlockModels
                                        .remove(myCurrentAssertiveCodeBlock));

                // Store the variable's finalization item for
                // future use.
                AffectsClause finalAffects =
                        type.getFinalization().getAffectedVars();
                AssertionClause finalEnsures =
                        type.getFinalization().getEnsures();
                if (!VarExp.isLiteralTrue(finalEnsures.getAssertionExp())) {
                    myVariableSpecFinalItems.put(dec, new SpecInitFinalItem(
                            type.getFinalization().getLocation(), type
                                    .getFinalization().getClauseType(),
                            finalAffects, Utilities.getTypeFinalEnsuresClause(
                                    finalEnsures, dec.getLocation(), null, dec
                                            .getName(), type.getExemplar(),
                                    typeEntry.getModelType(), null)));
                }*/

                // NY YS
                // TODO: Initialization duration for this variable

                // Update the associated block model.
                myAssertiveCodeBlockModels.put(myCurrentAssertiveCodeBlock,
                        blockModel);
            }
            else {
                // Variable declaration rule for generic types
                ProofRuleApplication declRule =
                        new GenericTypeVariableDeclRule(dec,
                                myCurrentAssertiveCodeBlock, mySTGroup,
                                blockModel);
                declRule.applyRule();

                // Update the current assertive code block and its associated block model.
                myCurrentAssertiveCodeBlock =
                        declRule.getAssertiveCodeBlocks().getFirst();
                myAssertiveCodeBlockModels.put(myCurrentAssertiveCodeBlock,
                        declRule.getBlockModel());
            }
        }
        else {
            // Shouldn't be possible but just in case it ever happens
            // by accident.
            Utilities.tyNotHandled(dec.getTy(), dec.getLocation());
        }
    }

    // -----------------------------------------------------------
    // Other
    // -----------------------------------------------------------

    /**
     * <p>Code that gets executed after visiting an {@link AssertionClause}.</p>
     *
     * @param clause An assertion clause declaration.
     */
    @Override
    public final void postAssertionClause(AssertionClause clause) {
        if (clause.getWhichEntailsExp() != null) {
            // Create a new assertive code block
            PosSymbol name =
                    new PosSymbol(clause.getWhichEntailsExp().getLocation()
                            .clone(), "Which_Entails Expression Located at "
                            + clause.getWhichEntailsExp().getLocation());
            AssertiveCodeBlock block =
                    new AssertiveCodeBlock(name, clause, myTypeGraph);

            // Make a copy of the clause expression and add
            // the location detail associated with it.
            Exp clauseExp = clause.getAssertionExp().clone();
            Location clauseLoc = clause.getWhichEntailsExp().getLocation();
            clauseExp.setLocationDetailModel(new LocationDetailModel(clauseLoc
                    .clone(), clauseLoc.clone(), clause.getClauseType().name()
                    + " Clause Located at " + clauseLoc.clone()));

            // Make a copy of the which_entails clause and add
            // the location detail associated with it.
            Exp whichEntailsExp = clause.getWhichEntailsExp().clone();
            Location entailsLoc = clause.getWhichEntailsExp().getLocation();
            whichEntailsExp.setLocationDetailModel(new LocationDetailModel(
                    entailsLoc.clone(), entailsLoc.clone(), name.getName()));

            // Apply the rule
            block.addStatement(new AssumeStmt(clause.getLocation().clone(),
                    clauseExp, false));
            block.addStatement(new ConfirmStmt(clause.getLocation().clone(),
                    whichEntailsExp, false));

            // Create a new model for this assertive code block
            ST blockModel = mySTGroup.getInstanceOf("outputAssertiveCodeBlock");
            blockModel.add("blockName", name);
            ST stepModel = mySTGroup.getInstanceOf("outputVCGenStep");
            stepModel.add("proofRuleName", "Which_Entails Declaration Rule")
                    .add("currentStateOfBlock", block);
            blockModel.add("vcGenSteps", stepModel.render());
            myAssertiveCodeBlockModels.put(block, blockModel);

            // Add this as a new incomplete assertive code block
            myIncompleteAssertiveCodeBlocks.add(block);
        }
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * <p>This method returns the final {@link AssertiveCodeBlock AssertiveCodeBlocks}
     * containing the generated {@link Sequent Sequents}.</p>
     *
     * @return A list containing {@link AssertiveCodeBlock AssertiveCodeBlocks}.
     */
    public final List<AssertiveCodeBlock> getFinalAssertiveCodeBlocks() {
        return myFinalAssertiveCodeBlocks;
    }

    /**
     * <p>This method returns the verbose mode output with how we generated
     * the {@code VCs} for this {@link ModuleDec}.</p>
     *
     * @return A string containing lots of details.
     */
    public final String getVerboseModeOutput() {
        return myVCGenDetailsModel.render();
    }

    // ===========================================================
    // Private Methods
    // ===========================================================

    /**
     * <p>An helper method that adds the operation's type constraints.</p>
     *
     * @param loc The location in the AST that we are
     *            currently visiting.
     * @param exp The top level assume expression we have built so far.
     * @param scope The module scope to start our search.
     * @param currentBlock The current {@link AssertiveCodeBlock} we are currently generating.
     * @param entries List of operation's parameter entries.
     * @param processedInstFacDecs List of {@link InstantiatedFacilityDecl} we have processed
     *                             so far.
     *
     * @return The original {@code exp} plus any operation parameter's type constraints.
     */
    private Exp addParamTypeConstraints(Location loc, Exp exp,
            ModuleScope scope, AssertiveCodeBlock currentBlock,
            ImmutableList<ProgramParameterEntry> entries,
            List<InstantiatedFacilityDecl> processedInstFacDecs) {
        Exp retExp = exp;

        // Loop through each of the parameters in the operation entry.
        for (ProgramParameterEntry entry : entries) {
            ParameterVarDec parameterVarDec =
                    (ParameterVarDec) entry.getDefiningElement();
            PTType declaredType = entry.getDeclaredType();
            ProgramParameterEntry.ParameterMode parameterMode =
                    entry.getParameterMode();

            // Only deal with actual types and don't deal
            // with entry types passed in to the concept realization
            if (!(declaredType instanceof PTGeneric)) {
                // Query for the type entry in the symbol table
                NameTy nameTy = (NameTy) parameterVarDec.getTy();
                SymbolTableEntry ste =
                        Utilities.searchProgramType(loc, nameTy.getQualifier(),
                                nameTy.getName(), scope);

                ProgramTypeEntry typeEntry;
                if (ste instanceof ProgramTypeEntry) {
                    typeEntry = ste.toProgramTypeEntry(nameTy.getLocation());
                }
                else {
                    typeEntry =
                            ste.toTypeRepresentationEntry(nameTy.getLocation())
                                    .getDefiningTypeEntry();
                }

                // Obtain the original dec from the AST
                TypeFamilyDec typeFamilyDec =
                        (TypeFamilyDec) typeEntry.getDefiningElement();

                // Other than the replaces mode, constraints for the
                // other parameter modes needs to be added
                // to the requires clause as conjuncts.
                if (parameterMode != ProgramParameterEntry.ParameterMode.REPLACES) {
                    if (!VarExp.isLiteralTrue(typeFamilyDec.getConstraint()
                            .getAssertionExp())) {
                        AssertionClause constraintClause =
                                typeFamilyDec.getConstraint();
                        AssertionClause modifiedConstraintClause =
                                Utilities.getTypeConstraintClause(
                                        constraintClause, loc, null,
                                        parameterVarDec.getName(),
                                        typeFamilyDec.getExemplar(), typeEntry
                                                .getModelType(), null);

                        // Replace any facility formal with actual
                        Exp constraintExp =
                                modifiedConstraintClause.getAssertionExp();
                        constraintExp =
                                Utilities
                                        .replaceFacilityFormalWithActual(
                                                constraintExp,
                                                Collections
                                                        .singletonList(parameterVarDec),
                                                scope.getDefiningElement()
                                                        .getName(),
                                                myCurrentVerificationContext
                                                        .getConceptDeclaredTypes(),
                                                myCurrentVerificationContext
                                                        .getLocalTypeRepresentationDecs(),
                                                processedInstFacDecs);

                        Exp whichEntailsExp =
                                modifiedConstraintClause.getWhichEntailsExp();
                        if (whichEntailsExp != null) {
                            whichEntailsExp =
                                    Utilities
                                            .replaceFacilityFormalWithActual(
                                                    whichEntailsExp,
                                                    Collections
                                                            .singletonList(parameterVarDec),
                                                    scope.getDefiningElement()
                                                            .getName(),
                                                    myCurrentVerificationContext
                                                            .getConceptDeclaredTypes(),
                                                    myCurrentVerificationContext
                                                            .getLocalTypeRepresentationDecs(),
                                                    processedInstFacDecs);
                        }

                        modifiedConstraintClause =
                                new AssertionClause(modifiedConstraintClause
                                        .getLocation().clone(),
                                        modifiedConstraintClause
                                                .getClauseType(),
                                        constraintExp, whichEntailsExp);

                        // Form a conjunct with the modified constraint clause and add
                        // the location detail associated with it.
                        Location constraintLoc =
                                modifiedConstraintClause.getAssertionExp()
                                        .getLocation();
                        retExp =
                                Utilities.formConjunct(loc, retExp,
                                        modifiedConstraintClause,
                                        new LocationDetailModel(constraintLoc
                                                .clone(),
                                                constraintLoc.clone(),
                                                "Constraint Clause of "
                                                        + parameterVarDec
                                                                .getName()));
                    }
                }

                // TODO: Handle type representations from concept realizations
                /*
                // If the type is a type representation, then our requires clause
                // should really say something about the conceptual type and not
                // the variable
                if (ste instanceof RepresentationTypeEntry && !isLocal) {
                    requires =
                            Utilities.replace(requires, parameterExp,
                                    Utilities
                                            .createConcVarExp(opLocation,
                                                    parameterExp,
                                                    parameterExp
                                                            .getMathType(),
                                                    BOOLEAN));
                    requires.setLocation((Location) opLocation.clone());
                }

                // If the type is a type representation, then we need to add
                // all the type constraints from all the variable declarations
                // in the type representation.
                if (ste instanceof RepresentationTypeEntry) {
                    Exp repConstraintExp = null;
                    Set<VarExp> keys =
                            myRepresentationConstraintMap.keySet();
                    for (VarExp varExp : keys) {
                        if (varExp.getQualifier() == null
                                && varExp.getName().getName().equals(
                                pNameTy.getName().getName())) {
                            if (repConstraintExp == null) {
                                repConstraintExp =
                                        myRepresentationConstraintMap
                                                .get(varExp);
                            }
                            else {
                                Utilities.ambiguousTy(pNameTy, pNameTy
                                        .getLocation());
                            }
                        }
                    }

                    // Only do the following if the expression is not simply true
                    if (!repConstraintExp.isLiteralTrue()) {
                        // Replace the exemplar with the actual parameter variable expression
                        repConstraintExp =
                                Utilities.replace(repConstraintExp,
                                        exemplar, parameterExp);

                        // Add this to our requires clause
                        requires =
                                myTypeGraph.formConjunct(requires,
                                        repConstraintExp);
                        requires.setLocation((Location) opLocation.clone());
                    }
                }*/
            }

            // Add the current variable to our list of free variables
            currentBlock.addFreeVar(Utilities.createVarExp(parameterVarDec
                    .getLocation(), null, parameterVarDec.getName(),
                    declaredType.toMath(), null));

        }

        return retExp;
    }

    /**
     * <p>Applies each of the statement proof rules. After this call, we are
     * done processing {@code assertiveCodeBlock}.</p>
     *
     * @param assertiveCodeBlock An assertive block that we are trying apply
     *                           the proof rules to the various
     *                           {@link Statement Statements}.
     */
    private void applyStatementRules(AssertiveCodeBlock assertiveCodeBlock) {
        // Obtain the assertive code block model
        ST blockModel = myAssertiveCodeBlockModels.remove(assertiveCodeBlock);

        // Apply a statement proof rule to each of the assertions.
        while (assertiveCodeBlock.hasMoreStatements()) {
            // Work our way from the last statement
            Statement statement = assertiveCodeBlock.removeLastSatement();

            // Generate one of the statement proof rule applications
            ProofRuleApplication ruleApplication;
            if (statement instanceof AssumeStmt) {
                // Generate a new assume rule application.
                ruleApplication =
                        new AssumeStmtRule((AssumeStmt) statement,
                                assertiveCodeBlock, mySTGroup, blockModel);
            }
            else if (statement instanceof CallStmt) {
                // Generate a new call rule application.
                ruleApplication =
                        new CallStmtRule((CallStmt) statement,
                                myCurrentConceptDeclaredTypes,
                                myLocalRepresentationTypeDecs,
                                myProcessedInstFacilityDecls, myBuilder,
                                myCurrentModuleScope, assertiveCodeBlock,
                                mySTGroup, blockModel);
            }
            else if (statement instanceof ChangeStmt) {
                // Generate a new change rule application.
                ruleApplication =
                        new ChangeStmtRule((ChangeStmt) statement,
                                assertiveCodeBlock, mySTGroup, blockModel);
            }
            else if (statement instanceof ConfirmStmt) {
                // Generate a new confirm rule application.
                ruleApplication =
                        new ConfirmStmtRule((ConfirmStmt) statement,
                                assertiveCodeBlock, mySTGroup, blockModel);
            }
            else if (statement instanceof FinalizeVarStmt) {
                // Generate a new variable finalization rule application.
                ruleApplication =
                        new FinalizeVarStmtRule((FinalizeVarStmt) statement,
                                assertiveCodeBlock, mySTGroup, blockModel);
            }
            else if (statement instanceof FuncAssignStmt) {
                // Generate a new function assignment rule application.
                ruleApplication =
                        new FuncAssignStmtRule((FuncAssignStmt) statement,
                                myCurrentConceptDeclaredTypes,
                                myLocalRepresentationTypeDecs,
                                myProcessedInstFacilityDecls, myBuilder,
                                myCurrentModuleScope, assertiveCodeBlock,
                                mySTGroup, blockModel);
            }
            else if (statement instanceof IfStmt) {
                // Generate a new if-else rule application.
                ruleApplication =
                        new IfStmtRule((IfStmt) statement,
                                myCurrentConceptDeclaredTypes,
                                myLocalRepresentationTypeDecs,
                                myProcessedInstFacilityDecls, myBuilder,
                                myCurrentModuleScope, assertiveCodeBlock,
                                mySTGroup, blockModel);
            }
            else if (statement instanceof InitializeVarStmt) {
                // Generate a new variable declaration/initialization rule application.
                ruleApplication =
                        new InitializeVarStmtRule(
                                (InitializeVarStmt) statement,
                                assertiveCodeBlock, mySTGroup, blockModel);
            }
            else if (statement instanceof MemoryStmt) {
                if (((MemoryStmt) statement).getStatementType() == StatementType.REMEMBER) {
                    // Generate a new remember rule application.
                    ruleApplication =
                            new RememberStmtRule(assertiveCodeBlock, mySTGroup,
                                    blockModel);
                }
                else {
                    throw new SourceErrorException(
                            "[VCGenerator] Forget statements are not handled.",
                            statement.getLocation());
                }
            }
            else if (statement instanceof PresumeStmt) {
                // Generate a new presume rule application.
                ruleApplication =
                        new PresumeStmtRule((PresumeStmt) statement,
                                assertiveCodeBlock, mySTGroup, blockModel);
            }
            else if (statement instanceof SwapStmt) {
                // Generate a new swap rule application.
                ruleApplication =
                        new SwapStmtRule((SwapStmt) statement,
                                myCurrentModuleScope, assertiveCodeBlock,
                                mySTGroup, blockModel);
            }
            else if (statement instanceof VCConfirmStmt) {
                // Generate a new VCConfirm rule application.
                ruleApplication =
                        new VCConfirmStmtRule((VCConfirmStmt) statement,
                                assertiveCodeBlock, mySTGroup, blockModel);
            }
            else if (statement instanceof WhileStmt) {
                // Generate a new while rule application
                ruleApplication =
                        new WhileStmtRule((WhileStmt) statement,
                                myCurrentModuleScope, myTypeGraph,
                                assertiveCodeBlock, mySTGroup, blockModel);
            }
            else {
                throw new SourceErrorException(
                        "[VCGenerator] Statement type not handled: "
                                + statement.getClass().getSimpleName(),
                        statement.getLocation());
            }

            // Apply the proof rule
            ruleApplication.applyRule();

            // Some of the proof rules might generate more than more
            // than one assertive code block. The first one is always
            // the one we passed in to the rule. We add the rest to the
            // front of the incomplete stack.
            Deque<AssertiveCodeBlock> resultingBlocks =
                    ruleApplication.getAssertiveCodeBlocks();
            assertiveCodeBlock = resultingBlocks.removeFirst();
            while (!resultingBlocks.isEmpty()) {
                myIncompleteAssertiveCodeBlocks.addFirst(resultingBlocks
                        .removeLast());
            }

            // Store any new block models
            myAssertiveCodeBlockModels.putAll(ruleApplication
                    .getNewAssertiveCodeBlockModels());

            // Update our block model
            blockModel = ruleApplication.getBlockModel();
        }

        // If this block contains any branching conditions, add it
        // to our block model.
        Deque<String> branchingConditions =
                assertiveCodeBlock.getBranchingConditions();
        if (!branchingConditions.isEmpty()) {
            ST branchingModel =
                    mySTGroup.getInstanceOf("outputBranchingConditions");
            ST test = branchingModel.add("conditions", branchingConditions);
            blockModel.add("branchingConditions", test.render());
        }

        myAssertiveCodeBlockModels.put(assertiveCodeBlock, blockModel);
    }

    /**
     * <p>An helper method that uses all the {@code requires} and {@code constraint}
     * clauses from the various different sources (see below for complete list)
     * and builds the appropriate {@code assume} clause that goes at the
     * beginning an {@link AssertiveCodeBlock}.</p>
     *
     * <p>List of different places where clauses can originate from:</p>
     * <ul>
     *     <li>{@code Concept}'s {@code requires} clause.</li>
     *     <li>{@code Concept}'s module {@code constraint} clause.</li>
     *     <li>{@code Shared Variables}' {@code constraint} clause.</li>
     *     <li>{@code Concept Realization}'s {@code requires} clause.</li>
     *     <li>{@code Shared Variables}' {@code convention} clause.</li>
     *     <li>{@code Shared Variables}' {@code correspondence} clause.</li>
     *     <li>{@code constraint} clauses for all the parameters with the
     *     appropriate substitutions made.</li>
     *     <li>The {@code operation}'s {@code requires} clause with the following
     *     change if it is an implementation for a {@code concept}'s operation:</li>
     *     <li>
     *         <ul>
     *             <li>Substitute the parameter name with {@code Conc.<name>} if this
     *             is the type we are implementing in a {@code concept realization}.</li>
     *         </ul>
     *     </li>
     *     <li>Any {@code which_entails} expressions that originated from any of the
     *     clauses above.</li>
     * </ul>
     *
     * <p>See the {@code Procedure} declaration rule for more detail.</p>
     *
     * @param loc The location in the AST that we are
     *            currently visiting.
     * @param currentBlock The current {@link AssertiveCodeBlock} we are currently generating.
     * @param correspondingOperationEntry The corresponding {@link OperationEntry}.
     * @param processedInstFacDecs List of {@link InstantiatedFacilityDecl} we have processed
     *                             so far.
     * @param isLocalOperation {@code true} if it is a local operation, {@code false} otherwise.
     *
     * @return The top-level assumed expression.
     */
    private Exp createTopLevelAssumeExpForProcedureDec(Location loc,
            AssertiveCodeBlock currentBlock,
            OperationEntry correspondingOperationEntry,
            List<InstantiatedFacilityDecl> processedInstFacDecs,
            boolean addConstraints, boolean isLocalOperation) {
        // Add all the expressions we can assume from the current context
        Exp retExp =
                myCurrentVerificationContext
                        .createTopLevelAssumeExpFromContext(loc);

        // Add the operation's requires clause (and any which_entails clause)
        AssertionClause requiresClause =
                correspondingOperationEntry.getRequiresClause();
        Exp requiresExp = requiresClause.getAssertionExp().clone();
        if (!VarExp.isLiteralTrue(requiresExp)) {
            // Form a conjunct with the requires clause and add
            // the location detail associated with it.
            retExp =
                    Utilities.formConjunct(loc, retExp, requiresClause,
                            new LocationDetailModel(requiresClause
                                    .getLocation().clone(), requiresClause
                                    .getLocation().clone(),
                                    "Requires Clause of "
                                            + correspondingOperationEntry
                                                    .getName()));
        }

        // Add the operation parameter's type constraints.
        // YS: We are not adding these automatically. Most of the time, these
        //     constraints wouldn't really help us prove any of the VCs. If you
        //     are ever interested in adding these to the givens list, use the
        //     "addConstraints" flag. Note that these constraints still need to be
        //     processed by the parsimonious step, so there is no guarantee that they
        //     will show up in all of the VCs.
        if (addConstraints) {
            retExp =
                    addParamTypeConstraints(loc, retExp, myCurrentModuleScope,
                            currentBlock, correspondingOperationEntry
                                    .getParameters(), processedInstFacDecs);
        }

        return retExp;
    }

}