/*
 * InitializeVarStmtRule.java
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
package edu.clemson.cs.rsrg.vcgeneration.proofrules.statement;

import edu.clemson.cs.rsrg.absyn.declarations.variabledecl.VarDec;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.AbstractProofRuleApplication;
import edu.clemson.cs.rsrg.vcgeneration.proofrules.ProofRuleApplication;
import edu.clemson.cs.rsrg.vcgeneration.utilities.AssertiveCodeBlock;
import edu.clemson.cs.rsrg.vcgeneration.utilities.helperstmts.InitializeVarStmt;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

/**
 * <p>This class contains the logic for applying variable declaration and
 * initialization logic for the {@link VarDec} stored inside a {@link InitializeVarStmt}.</p>
 *
 * @author Yu-Shan Sun
 * @version 1.0
 */
public class InitializeVarStmtRule extends AbstractProofRuleApplication
        implements
            ProofRuleApplication {

    // ===========================================================
    // Member Fields
    // ===========================================================

    /** <p>The {@link InitializeVarStmt} we are applying the rule to.</p> */
    private final InitializeVarStmt myInitVarStmt;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * <p>This creates an application rule that deals with
     * {@link InitializeVarStmt}.</p>
     *
     * @param initVarStmt The {@link InitializeVarStmt} we are applying
     *                    the rule to.
     * @param block The assertive code block that the subclasses are
     *              applying the rule to.
     * @param stGroup The string template group we will be using.
     * @param blockModel The model associated with {@code block}.
     */
    public InitializeVarStmtRule(InitializeVarStmt initVarStmt,
            AssertiveCodeBlock block, STGroup stGroup, ST blockModel) {
        super(block, stGroup, blockModel);
        myInitVarStmt = initVarStmt;
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    /**
     * <p>This method applies the {@code Proof Rule}.</p>
     */
    @Override
    public final void applyRule() {
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
        return "Variable Declaration/Initialization Rule";
    }

}