package edu.clemson.cs.r2jt.preprocessing;

/* Libraries */
import java.util.HashMap;
import java.util.Iterator;

import edu.clemson.cs.r2jt.absyn.ArrayTy;
import edu.clemson.cs.r2jt.absyn.CallStmt;
import edu.clemson.cs.r2jt.absyn.ChoiceItem;
import edu.clemson.cs.r2jt.absyn.ConceptBodyModuleDec;
import edu.clemson.cs.r2jt.absyn.ConditionItem;
import edu.clemson.cs.r2jt.absyn.Dec;
import edu.clemson.cs.r2jt.absyn.EnhancementBodyItem;
import edu.clemson.cs.r2jt.absyn.EnhancementBodyModuleDec;
import edu.clemson.cs.r2jt.absyn.EnhancementItem;
import edu.clemson.cs.r2jt.absyn.FacilityDec;
import edu.clemson.cs.r2jt.absyn.FacilityModuleDec;
import edu.clemson.cs.r2jt.absyn.FacilityOperationDec;
import edu.clemson.cs.r2jt.absyn.FacilityTypeDec;
import edu.clemson.cs.r2jt.absyn.FuncAssignStmt;
import edu.clemson.cs.r2jt.absyn.IfStmt;
import edu.clemson.cs.r2jt.absyn.InitItem;
import edu.clemson.cs.r2jt.absyn.IterateExitStmt;
import edu.clemson.cs.r2jt.absyn.IterateStmt;
import edu.clemson.cs.r2jt.absyn.ModuleArgumentItem;
import edu.clemson.cs.r2jt.absyn.NameTy;
import edu.clemson.cs.r2jt.absyn.ParameterVarDec;
import edu.clemson.cs.r2jt.absyn.ProcedureDec;
import edu.clemson.cs.r2jt.absyn.ProgramExp;
import edu.clemson.cs.r2jt.absyn.ProgramParamExp;
import edu.clemson.cs.r2jt.absyn.RecordTy;
import edu.clemson.cs.r2jt.absyn.RepresentationDec;
import edu.clemson.cs.r2jt.absyn.ResolveConceptualElement;
import edu.clemson.cs.r2jt.absyn.SelectionStmt;
import edu.clemson.cs.r2jt.absyn.Statement;
import edu.clemson.cs.r2jt.absyn.SwapStmt;
import edu.clemson.cs.r2jt.absyn.UsesItem;
import edu.clemson.cs.r2jt.absyn.VarDec;
import edu.clemson.cs.r2jt.absyn.VariableArrayExp;
import edu.clemson.cs.r2jt.absyn.VariableDotExp;
import edu.clemson.cs.r2jt.absyn.VariableExp;
import edu.clemson.cs.r2jt.absyn.VariableNameExp;
import edu.clemson.cs.r2jt.absyn.WhileStmt;
import edu.clemson.cs.r2jt.collections.List;
import edu.clemson.cs.r2jt.collections.Map;
import edu.clemson.cs.r2jt.data.Location;
import edu.clemson.cs.r2jt.data.PosSymbol;
import edu.clemson.cs.r2jt.data.Symbol;
import edu.clemson.cs.r2jt.treewalk.TreeWalkerStackVisitor;

/**
 * TODO: 
 * 1) Swap_Two_Entries calls in postSwapStmt 
 * 2) CallStmt -> P(A[i]) and P(S.Contents[S.Top])
 * 3) FuncAssignStmt -> Replica(A[i]) and Replica(S.Contents[S.Top])
 * 4) FuncAssignStmt -> "S.Contents[S.Top] := ..."
 */
public class PreProcessor extends TreeWalkerStackVisitor {

    // ===========================================================
    // Global Variables 
    // ===========================================================

    /* A counter used to keep track the number of variables created */
    private int myCounter;

    /* List of global/local variables and record types */
    private edu.clemson.cs.r2jt.collections.List<VarDec> myGlobalVarList;
    private edu.clemson.cs.r2jt.collections.List<VarDec> myLocalVarList;
    private edu.clemson.cs.r2jt.collections.List<FacilityTypeDec> myFacilityTypeList;
    private edu.clemson.cs.r2jt.collections.List<RepresentationDec> myRepresentationDecList;
    private edu.clemson.cs.r2jt.collections.List<VarDec> myParameterVarList;
    private Map<String, NameTy> myArrayFacilityMap;
    private edu.clemson.cs.r2jt.collections.List<CallStmt> mySwapList;

    // ===========================================================
    // Constructors
    // ===========================================================

    public PreProcessor() {
        myCounter = 1;
        myGlobalVarList = null;
        myLocalVarList = null;
        myFacilityTypeList = null;
        myRepresentationDecList = null;
        myParameterVarList = null;
        myArrayFacilityMap = new Map<String, NameTy>();
        mySwapList = new edu.clemson.cs.r2jt.collections.List<CallStmt>();
    }

    // -----------------------------------------------------------
    // Array Ty
    // -----------------------------------------------------------

    @Override
    public void postArrayTy(ArrayTy data) {
        /* Variables */
        ResolveConceptualElement parent = this.getParent();
        String name = "";
        Location currentLocation = data.getLocation();
        NameTy newTy = null;
        NameTy tempTy = (NameTy) data.getEntryType();
        String varToBeAssign = null;

        /* Check if we have a FacilityTypeDec or VarDec */
        if (parent instanceof FacilityTypeDec) {
            varToBeAssign = ((FacilityTypeDec) parent).getName().getName();

            /* Create name in the format of "_(Name of Variable)_Array_Fac_(myCounter)" */
            name += ("_" + varToBeAssign + "_Array_Fac_" + myCounter++);

            /* Create newTy */
            newTy =
                    new NameTy(new PosSymbol(currentLocation, Symbol
                            .symbol(name)), new PosSymbol(currentLocation,
                            Symbol.symbol("Static_Array")));

            /* Set the Ty of the Parent */
            ((FacilityTypeDec) parent).setRepresentation(newTy);
        }
        else if (parent instanceof RepresentationDec) {
            varToBeAssign = ((RepresentationDec) parent).getName().getName();

            /* Create name in the format of "_(Name of Variable)_Array_Fac_(myCounter)" */
            name += ("_" + varToBeAssign + "_Array_Fac" + myCounter++);

            /* Create newTy */
            newTy =
                    new NameTy(new PosSymbol(currentLocation, Symbol
                            .symbol(name)), new PosSymbol(currentLocation,
                            Symbol.symbol("Static_Array")));

            /* Set the Ty of the Parent */
            ((RepresentationDec) parent).setRepresentation(newTy);
        }
        else if (parent instanceof VarDec) {
            varToBeAssign = ((VarDec) parent).getName().getName();

            /* Create name in the format of "_(Name of Variable)_Array_Fac_(myCounter)" */
            name += ("_" + varToBeAssign + "_Array_Fac_" + myCounter++);

            /* Create newTy */
            newTy =
                    new NameTy(new PosSymbol(currentLocation, Symbol
                            .symbol(name)), new PosSymbol(currentLocation,
                            Symbol.symbol("Static_Array")));

            /* Set the Ty of the Parent */
            ((VarDec) parent).setTy(newTy);
        }

        /* Create a FacilityDec */
        FacilityDec arrayFacilityDec = new FacilityDec();

        // Set the name
        arrayFacilityDec.setName(new PosSymbol(currentLocation, Symbol
                .symbol(name)));

        // Set the Concept to "Static_Array_Template
        arrayFacilityDec.setConceptName(new PosSymbol(currentLocation, Symbol
                .symbol("Static_Array_Template")));

        // Set the arguments passed to "Static_Array_Template"
        edu.clemson.cs.r2jt.collections.List<ModuleArgumentItem> listItem =
                new edu.clemson.cs.r2jt.collections.List<ModuleArgumentItem>();
        String typeName = ((NameTy) data.getEntryType()).getName().getName();

        // Add the type, Low and High for Arrays
        listItem.add(new ModuleArgumentItem(null, new PosSymbol(
                currentLocation, Symbol.symbol(typeName)), null));
        listItem.add(new ModuleArgumentItem(null, null, data.getLo()));
        listItem.add(new ModuleArgumentItem(null, null, data.getHi()));

        arrayFacilityDec.setConceptParams(listItem);

        // Set the Concept Realization to "Std_Array_Realiz */
        arrayFacilityDec.setBodyName(new PosSymbol(currentLocation, Symbol
                .symbol("Std_Array_Realiz")));
        arrayFacilityDec
                .setBodyParams(new edu.clemson.cs.r2jt.collections.List<ModuleArgumentItem>());

        // Set the Enhancement to empty
        arrayFacilityDec
                .setEnhancementBodies(new edu.clemson.cs.r2jt.collections.List<EnhancementBodyItem>());
        arrayFacilityDec
                .setEnhancements(new edu.clemson.cs.r2jt.collections.List<EnhancementItem>());

        /* Iterate through AST */
        Iterator<ResolveConceptualElement> it = this.getAncestorInterator();

        /* Add the arrayFacilityDec to the list of Decs where it belongs */
        addFacilityDec(it, arrayFacilityDec);

        /* For Future Use */
        myArrayFacilityMap.put(varToBeAssign, (NameTy) tempTy);
    }

    // -----------------------------------------------------------
    // CallStmt
    // -----------------------------------------------------------

    @Override
    public void postCallStmt(CallStmt stmt) {
        /* Variables */
        Location currentLocation = stmt.getName().getLocation();

        /* Iterate through argument list */
        edu.clemson.cs.r2jt.collections.List<ProgramExp> argList =
                stmt.getArguments();
        for (int i = 0; i < argList.size(); i++) {
            /* Temp variable */
            ProgramExp temp = argList.get(i);

            /* Check if it is a VariableArrayExp */
            if (temp instanceof VariableArrayExp) {
                /* Variable */
                VariableNameExp retval =
                        checkForVariableArrayExp(currentLocation,
                                (VariableArrayExp) temp);
                argList.set(i, retval);
            }
            /* Check if it is a VariableDotExp */
            else if (temp instanceof VariableDotExp) {
                /* Variables */
                VariableExp lastElement;

                /* Get list of segments */
                edu.clemson.cs.r2jt.collections.List<VariableExp> segList =
                        ((VariableDotExp) temp).getSegments();

                /* Check if the last element is an VariableArrayExp */
                lastElement = segList.get(segList.size() - 1);
                if (lastElement instanceof VariableArrayExp) {
                    /* Create new VariableNameExp */
                    PosSymbol newName =
                            new PosSymbol(currentLocation, Symbol
                                    .symbol("_Array_"
                                            + ((VariableArrayExp) lastElement)
                                                    .getName().getName() + "_"
                                            + myCounter++));
                    VariableNameExp newVar =
                            new VariableNameExp(currentLocation, null, newName);
                    argList.set(i, newVar);

                    /* Create a new VarDec */
                    VarDec newVarDec = new VarDec();
                    newVarDec.setName(newName);

                    /* Check Local Variable List for Ty */
                    VarDec tempVarDec = null;
                    if (myLocalVarList != null) {
                        tempVarDec =
                                iterateFindVarDec(((VariableNameExp) segList
                                        .get(0)).getName(), myLocalVarList);
                    }

                    /* Check Global Variable List for Ty */
                    if (tempVarDec == null && myGlobalVarList != null) {
                        tempVarDec =
                                iterateFindVarDec(((VariableNameExp) segList
                                        .get(0)).getName(), myGlobalVarList);
                    }

                    /* Check Parameter Variable List for Ty */
                    if (tempVarDec == null && myParameterVarList != null) {
                        tempVarDec =
                                iterateFindVarDec(((VariableNameExp) segList
                                        .get(0)).getName(), myParameterVarList);
                    }

                    /** TODO: Throw error message */
                    if (tempVarDec != null) {
                        /* Check if we are in an EnhancementRealization or ConceptRealization */
                        if (myRepresentationDecList != null) {
                            Iterator<RepresentationDec> it =
                                    myRepresentationDecList.iterator();

                            /* Loop Through */
                            while (it.hasNext()) {
                                RepresentationDec tempRepDec = it.next();

                                /* Check name of tempRepDec against ty of tempVarDec */
                                if (tempRepDec.getName().getName().equals(
                                        ((NameTy) tempVarDec.getTy()).getName()
                                                .getName())) {
                                    RecordTy tempTy =
                                            (RecordTy) tempRepDec
                                                    .getRepresentation();

                                    /* Get list of fields */
                                    VarDec varInRecord =
                                            iterateFindVarDec(
                                                    ((VariableArrayExp) lastElement)
                                                            .getName(), tempTy
                                                            .getFields());

                                    /** TODO: Error */
                                    if (varInRecord != null) {
                                        /* Get the Ty of the Array from the Map*/
                                        String nameOfArray =
                                                ((NameTy) varInRecord.getTy())
                                                        .getQualifier()
                                                        .getName();
                                        newVarDec.setTy(myArrayFacilityMap
                                                .get(nameOfArray));
                                    }
                                }
                            }
                        }
                        /* We must be in a Facility */
                        else {
                            Iterator<FacilityTypeDec> it =
                                    myFacilityTypeList.iterator();

                            /* Loop Through */
                            while (it.hasNext()) {
                                FacilityTypeDec tempRepDec = it.next();

                                /* Check name of tempRepDec against ty of tempVarDec */
                                if (tempRepDec.getName().getName().equals(
                                        ((NameTy) tempVarDec.getTy()).getName()
                                                .getName())) {
                                    RecordTy tempTy =
                                            (RecordTy) tempRepDec
                                                    .getRepresentation();

                                    /* Get list of fields */
                                    VarDec varInRecord =
                                            iterateFindVarDec(
                                                    ((VariableArrayExp) lastElement)
                                                            .getName(), tempTy
                                                            .getFields());

                                    /** TODO: Error */
                                    if (varInRecord != null) {
                                        /* Get the Ty of the Array from the Map*/
                                        String nameOfArray =
                                                ((NameTy) varInRecord.getTy())
                                                        .getQualifier()
                                                        .getName();
                                        newVarDec.setTy(myArrayFacilityMap
                                                .get(nameOfArray));
                                    }
                                }
                            }
                        }
                    }

                    /* Add it to our local variable list */
                    myLocalVarList.add(newVarDec);

                    edu.clemson.cs.r2jt.collections.List<ProgramExp> expList;
                    expList =
                            new edu.clemson.cs.r2jt.collections.List<ProgramExp>();

                    /* Create the argument list and add the arguments necessary for Swap_Entry in Static_Array_Template */
                    expList.add(new VariableNameExp(currentLocation, null,
                            ((VariableArrayExp) lastElement).getName()));
                    expList.add(newVar);
                    expList.add(((VariableArrayExp) lastElement).getArgument());

                    /* Create a CallStmt */
                    CallStmt swapEntryStmt =
                            new CallStmt(null, new PosSymbol(currentLocation,
                                    Symbol.symbol("Swap_Entry")), expList);
                    mySwapList.add(swapEntryStmt);
                }
            }
        }

        /* Check if we have any swap statements we need to add */
        if (mySwapList.size() != 0) {
            Iterator<ResolveConceptualElement> it = this.getAncestorInterator();
            while (it.hasNext()) {
                /* Obtain a temp from it */
                ResolveConceptualElement temp = it.next();

                /* Look for FacilityOperationDec */
                if (temp instanceof FacilityOperationDec) {
                    edu.clemson.cs.r2jt.collections.List<Statement> stmtList =
                            ((FacilityOperationDec) temp).getStatements();

                    /* Loop through the list */
                    for (int i = 0; i < stmtList.size(); i++) {
                        if (stmtList.get(i) instanceof CallStmt
                                && ((CallStmt) stmtList.get(i)).getName()
                                        .getLocation() == currentLocation) {
                            /* Add the swap statements after the call statement */
                            for (int j = 0; j < mySwapList.size(); j++) {
                                CallStmt newCallSwapStmt = mySwapList.get(j);
                                stmtList.add(i + 1, newCallSwapStmt);
                            }

                            /* Add the swap statements before the call statement */
                            for (int j = mySwapList.size() - 1; j >= 0; j--) {
                                CallStmt newCallSwapStmt = mySwapList.get(j);
                                stmtList.add(i, newCallSwapStmt);
                            }

                            break;
                        }
                    }
                }
                else if (temp instanceof ProcedureDec) {
                    edu.clemson.cs.r2jt.collections.List<Statement> stmtList =
                            ((ProcedureDec) temp).getStatements();

                    /* Loop through the list */
                    for (int i = 0; i < stmtList.size(); i++) {
                        if (stmtList.get(i) instanceof CallStmt
                                && ((CallStmt) stmtList.get(i)).getName()
                                        .getLocation() == currentLocation) {
                            /* Add the swap statements after the call statement */
                            for (int j = 0; j < mySwapList.size(); j++) {
                                CallStmt newCallSwapStmt = mySwapList.get(j);
                                stmtList.add(i + 1, newCallSwapStmt);
                            }

                            /* Add the swap statements before the call statement */
                            for (int j = mySwapList.size() - 1; j >= 0; j--) {
                                CallStmt newCallSwapStmt = mySwapList.get(j);
                                stmtList.add(i, newCallSwapStmt);
                            }

                            break;
                        }
                    }
                }
            }

            /* Clear the list */
            mySwapList.clear();
        }
    }

    // -----------------------------------------------------------
    // ConceptBodyModuleDec
    // -----------------------------------------------------------

    @Override
    public void preConceptBodyModuleDec(ConceptBodyModuleDec dec) {
        /* Get the list of Decs inside this ConceptBodyModuleDec */
        edu.clemson.cs.r2jt.collections.List<Dec> decList = dec.getDecs();

        /* Invoke private method (in last section) to find any FacilityTypeDec
         * or global variables */
        initVarDecList(decList);
    }

    @Override
    public void postConceptBodyModuleDec(ConceptBodyModuleDec dec) {
        /* Set myFacilityTypeList to null */
        if (myRepresentationDecList != null) {
            myRepresentationDecList = null;
        }

        /* Set myGlobalVarList to null */
        if (myGlobalVarList != null) {
            myGlobalVarList = null;
        }
    }

    // -----------------------------------------------------------
    // EnhancementBodyModuleDec
    // -----------------------------------------------------------

    @Override
    public void preEnhancementBodyModuleDec(EnhancementBodyModuleDec dec) {
        /* Get the list of Decs inside this EnhancementBodyModuleDec */
        edu.clemson.cs.r2jt.collections.List<Dec> decList = dec.getDecs();

        /* Invoke private method (in last section) to find any FacilityTypeDec
         * or global variables */
        initVarDecList(decList);
    }

    @Override
    public void postEnhancementBodyModuleDec(EnhancementBodyModuleDec dec) {
        /* Set myRepresentationDecList to null */
        if (myRepresentationDecList != null) {
            myRepresentationDecList = null;
        }

        /* Set myGlobalVarList to null */
        if (myGlobalVarList != null) {
            myGlobalVarList = null;
        }
    }

    // -----------------------------------------------------------
    // FacilityModuleDec
    // -----------------------------------------------------------

    @Override
    public void preFacilityModuleDec(FacilityModuleDec dec) {
        /* Get the list of Decs inside this FacilityModuleDec */
        edu.clemson.cs.r2jt.collections.List<Dec> decList = dec.getDecs();

        /* Invoke private method (in last section) to find any FacilityTypeDec
         * or global variables */
        initVarDecList(decList);
    }

    @Override
    public void postFacilityModuleDec(FacilityModuleDec dec) {
        /* Set myFacilityTypeList to null */
        if (myFacilityTypeList != null) {
            myFacilityTypeList = null;
        }

        /* Set myGlobalVarList to null */
        if (myGlobalVarList != null) {
            myGlobalVarList = null;
        }

        /* Add Static_Array_Template and Location_Linking_Template_1 to the uses list */
        List<UsesItem> temp = dec.getUsesItems();
        if (temp == null) {
            temp = new List<UsesItem>();
        }
        temp.add(new UsesItem(new PosSymbol(dec.getName().getLocation(), Symbol
                .symbol("Location_Linking_Template_1"))));
        temp.add(new UsesItem(new PosSymbol(dec.getName().getLocation(), Symbol
                .symbol("Static_Array_Template"))));
        dec.setUsesItems(temp);
    }

    // -----------------------------------------------------------
    // FacilityOperationDec
    // -----------------------------------------------------------

    @Override
    public void preFacilityOperationDec(FacilityOperationDec dec) {
        /* Get list of local variables */
        myLocalVarList = dec.getVariables();

        /* Get list of parameter variables */
        myParameterVarList = new edu.clemson.cs.r2jt.collections.List<VarDec>();
        Iterator<ParameterVarDec> it = dec.getParameters().iterator();
        while (it.hasNext()) {
            ParameterVarDec temp = it.next();
            VarDec newVarDec = new VarDec(temp.getName(), temp.getTy());
            myParameterVarList.add(newVarDec);
        }
    }

    @Override
    public void postFacilityOperationDec(FacilityOperationDec dec) {
        /* Put the modified variable list into dec */
        dec.setVariables(myLocalVarList);

        /* Set myLocalVarList to null */
        myLocalVarList = null;
        myParameterVarList = null;
    }

    // -----------------------------------------------------------
    // Function Assignment Statement
    // -----------------------------------------------------------

    @Override
    public void postFuncAssignStmt(FuncAssignStmt stmt) {
        /* Make sure that the right expression is not a VariableArrayExp or
         * a VariableDotExp that contains a VariableArrayExp 
         */
        /** TODO: If it is throw an error and exit the compiler */

        /* Variables */
        Location currentLocation = stmt.getLocation();
        edu.clemson.cs.r2jt.collections.List<ProgramExp> expList;

        /* Ancestor Iterator */
        Iterator<ResolveConceptualElement> it = this.getAncestorInterator();

        /* If Variable on the left is a VariableArrayExp */
        if (stmt.getVar() instanceof VariableArrayExp) {
            expList = new edu.clemson.cs.r2jt.collections.List<ProgramExp>();

            /* Create the argument list and add the arguments necessary for Swap_Entry in Static_Array_Template */
            expList.add(new VariableNameExp(stmt.getLocation(), null,
                    ((VariableArrayExp) stmt.getVar()).getName()));
            expList.add(stmt.getAssign());
            expList.add(((VariableArrayExp) stmt.getVar()).getArgument());

            /* Create a CallStmt */
            CallStmt swapEntryStmt =
                    new CallStmt(null, new PosSymbol(currentLocation, Symbol
                            .symbol("Swap_Entry")), expList);

            /* Add the created swap stmt to the right place */
            replaceStmt(it, stmt, swapEntryStmt);
        }
        /* If Variable on the left is a VariableDotExp */
        else if (stmt.getVar() instanceof VariableDotExp) {
            edu.clemson.cs.r2jt.collections.List<VariableExp> variableExpList =
                    ((VariableDotExp) stmt.getVar()).getSegments();

            /* If this VariableDotExp is really a VariableArrayExp */
            VariableExp lastExp =
                    variableExpList.get(variableExpList.size() - 1);
            if (lastExp instanceof VariableArrayExp) {
                expList =
                        new edu.clemson.cs.r2jt.collections.List<ProgramExp>();

                /* Construct Name */
                String name = "";
                for (int i = 0; i < variableExpList.size() - 1; i++) {
                    /* Create a temp holder */
                    VariableExp temp = variableExpList.get(i);

                    /* Temp instanceof VariableNameExp */
                    if (temp instanceof VariableNameExp) {
                        name +=
                                (((VariableNameExp) temp).getName().getName() + ".");
                    }
                    /* Temp instanceof VariableArrayExp */
                    else {
                        name +=
                                (((VariableArrayExp) temp).getName().getName()
                                        + "["
                                        + ((VariableArrayExp) temp)
                                                .getArgument().toString() + "].");
                    }
                }

                /* Create the argument list and add the arguments necessary for Swap_Entry in Static_Array_Template */
                expList.add(new VariableNameExp(stmt.getLocation(), null,
                        new PosSymbol(currentLocation, Symbol.symbol(name
                                + ((VariableArrayExp) lastExp).getName()
                                        .getName()))));
                expList.add(stmt.getAssign());
                expList.add(((VariableArrayExp) lastExp).getArgument());

                /* Create a CallStmt */
                CallStmt swapEntryStmt =
                        new CallStmt(null, new PosSymbol(currentLocation,
                                Symbol.symbol("Swap_Entry")), expList);

                /* Add the created swap stmt to the right place */
                replaceStmt(it, stmt, swapEntryStmt);
            }
        }

        /* Check if we have a Function Assignment */
        if (stmt.getAssign() instanceof ProgramParamExp) {
            /* Iterate through argument list */
            edu.clemson.cs.r2jt.collections.List<ProgramExp> argList =
                    ((ProgramParamExp) stmt.getAssign()).getArguments();
            for (int i = 0; i < argList.size(); i++) {
                /* Temp variable */
                ProgramExp temp = argList.get(i);

                /* Check if it is a VariableArrayExp */
                if (temp instanceof VariableArrayExp) {
                    /* Variable */
                    VariableNameExp retval =
                            checkForVariableArrayExp(currentLocation,
                                    (VariableArrayExp) temp);
                    argList.set(i, retval);
                }
                /* Check if it is a VariableDotExp */
                else if (temp instanceof VariableDotExp) {
                    /* Variables */
                    VariableExp lastElement;

                    /* Get list of segments */
                    edu.clemson.cs.r2jt.collections.List<VariableExp> segList =
                            ((VariableDotExp) temp).getSegments();

                    /* Check if the last element is an VariableArrayExp */
                    lastElement = segList.get(segList.size() - 1);
                    if (lastElement instanceof VariableArrayExp) {
                        /* Create new VariableNameExp */
                        PosSymbol newName =
                                new PosSymbol(
                                        currentLocation,
                                        Symbol
                                                .symbol("_Array_"
                                                        + ((VariableArrayExp) lastElement)
                                                                .getName()
                                                                .getName()
                                                        + "_" + myCounter++));
                        VariableNameExp newVar =
                                new VariableNameExp(currentLocation, null,
                                        newName);
                        argList.set(i, newVar);

                        /* Create a new VarDec */
                        VarDec newVarDec = new VarDec();
                        newVarDec.setName(newName);

                        /* Check Local Variable List for Ty */
                        VarDec tempVarDec = null;
                        if (myLocalVarList != null) {
                            tempVarDec =
                                    iterateFindVarDec(
                                            ((VariableNameExp) segList.get(0))
                                                    .getName(), myLocalVarList);
                        }

                        /* Check Global Variable List for Ty */
                        if (tempVarDec == null && myGlobalVarList != null) {
                            tempVarDec =
                                    iterateFindVarDec(
                                            ((VariableNameExp) segList.get(0))
                                                    .getName(), myGlobalVarList);
                        }

                        /* Check Parameter Variable List for Ty */
                        if (tempVarDec == null && myParameterVarList != null) {
                            tempVarDec =
                                    iterateFindVarDec(
                                            ((VariableNameExp) segList.get(0))
                                                    .getName(),
                                            myParameterVarList);
                        }

                        /** TODO: Throw error message */
                        if (tempVarDec != null) {
                            /* Check if we are in an EnhancementRealization or ConceptRealization */
                            if (myRepresentationDecList != null) {
                                Iterator<RepresentationDec> it2 =
                                        myRepresentationDecList.iterator();

                                /* Loop Through */
                                while (it2.hasNext()) {
                                    RepresentationDec tempRepDec = it2.next();

                                    /* Check name of tempRepDec against ty of tempVarDec */
                                    if (tempRepDec.getName().getName().equals(
                                            ((NameTy) tempVarDec.getTy())
                                                    .getName().getName())) {
                                        RecordTy tempTy =
                                                (RecordTy) tempRepDec
                                                        .getRepresentation();

                                        /* Get list of fields */
                                        VarDec varInRecord =
                                                iterateFindVarDec(
                                                        ((VariableArrayExp) lastElement)
                                                                .getName(),
                                                        tempTy.getFields());

                                        /** TODO: Error */
                                        if (varInRecord != null) {
                                            /* Get the Ty of the Array from the Map*/
                                            String nameOfArray =
                                                    ((NameTy) varInRecord
                                                            .getTy())
                                                            .getQualifier()
                                                            .getName();
                                            newVarDec.setTy(myArrayFacilityMap
                                                    .get(nameOfArray));
                                        }
                                    }
                                }
                            }
                            /* We must be in a Facility */
                            else {
                                Iterator<FacilityTypeDec> it3 =
                                        myFacilityTypeList.iterator();

                                /* Loop Through */
                                while (it3.hasNext()) {
                                    FacilityTypeDec tempRepDec = it3.next();

                                    /* Check name of tempRepDec against ty of tempVarDec */
                                    if (tempRepDec.getName().getName().equals(
                                            ((NameTy) tempVarDec.getTy())
                                                    .getName().getName())) {
                                        RecordTy tempTy =
                                                (RecordTy) tempRepDec
                                                        .getRepresentation();

                                        /* Get list of fields */
                                        VarDec varInRecord =
                                                iterateFindVarDec(
                                                        ((VariableArrayExp) lastElement)
                                                                .getName(),
                                                        tempTy.getFields());

                                        /** TODO: Error */
                                        if (varInRecord != null) {
                                            /* Get the Ty of the Array from the Map*/
                                            String nameOfArray =
                                                    ((NameTy) varInRecord
                                                            .getTy())
                                                            .getQualifier()
                                                            .getName();
                                            newVarDec.setTy(myArrayFacilityMap
                                                    .get(nameOfArray));
                                        }
                                    }
                                }
                            }
                        }

                        /* Add it to our local variable list */
                        myLocalVarList.add(newVarDec);

                        edu.clemson.cs.r2jt.collections.List<ProgramExp> expList2;
                        expList2 =
                                new edu.clemson.cs.r2jt.collections.List<ProgramExp>();

                        /* Create the argument list and add the arguments necessary for Swap_Entry in Static_Array_Template */
                        expList2.add(new VariableNameExp(currentLocation, null,
                                ((VariableArrayExp) temp).getName()));
                        expList2.add(newVar);
                        expList2.add(((VariableArrayExp) temp).getArgument());

                        /* Create a CallStmt */
                        CallStmt swapEntryStmt =
                                new CallStmt(null, new PosSymbol(
                                        currentLocation, Symbol
                                                .symbol("Swap_Entry")),
                                        expList2);
                        mySwapList.add(swapEntryStmt);
                    }
                }
            }

            /* Check if we have any swap statements we need to add */
            if (mySwapList.size() != 0) {
                Iterator<ResolveConceptualElement> it4 =
                        this.getAncestorInterator();
                while (it4.hasNext()) {
                    /* Obtain a temp from it */
                    ResolveConceptualElement temp = it4.next();

                    /* Look for FacilityOperationDec */
                    if (temp instanceof FacilityOperationDec) {
                        edu.clemson.cs.r2jt.collections.List<Statement> stmtList =
                                ((FacilityOperationDec) temp).getStatements();

                        /* Loop through the list */
                        for (int i = 0; i < stmtList.size(); i++) {
                            if (stmtList.get(i) instanceof FuncAssignStmt
                                    && ((FuncAssignStmt) stmtList.get(i))
                                            .getLocation() == currentLocation) {
                                /* Add the swap statements after the call statement */
                                for (int j = 0; j < mySwapList.size(); j++) {
                                    CallStmt newCallSwapStmt =
                                            mySwapList.get(j);
                                    stmtList.add(i + 1, newCallSwapStmt);
                                }

                                /* Add the swap statements before the call statement */
                                for (int j = mySwapList.size() - 1; j >= 0; j--) {
                                    CallStmt newCallSwapStmt =
                                            mySwapList.get(j);
                                    stmtList.add(i, newCallSwapStmt);
                                }

                                break;
                            }
                        }
                    }
                    else if (temp instanceof ProcedureDec) {
                        edu.clemson.cs.r2jt.collections.List<Statement> stmtList =
                                ((ProcedureDec) temp).getStatements();

                        /* Loop through the list */
                        for (int i = 0; i < stmtList.size(); i++) {
                            if (stmtList.get(i) instanceof FuncAssignStmt
                                    && ((FuncAssignStmt) stmtList.get(i))
                                            .getLocation() == currentLocation) {
                                /* Add the swap statements after the call statement */
                                for (int j = 0; j < mySwapList.size(); j++) {
                                    CallStmt newCallSwapStmt =
                                            mySwapList.get(j);
                                    stmtList.add(i + 1, newCallSwapStmt);
                                }

                                /* Add the swap statements before the call statement */
                                for (int j = mySwapList.size() - 1; j >= 0; j--) {
                                    CallStmt newCallSwapStmt =
                                            mySwapList.get(j);
                                    stmtList.add(i, newCallSwapStmt);
                                }

                                break;
                            }
                        }
                    }
                }

                /* Clear the list */
                mySwapList.clear();
            }
        }
    }

    // -----------------------------------------------------------
    // ProcedureDec
    // -----------------------------------------------------------

    @Override
    public void preProcedureDec(ProcedureDec dec) {
        /* Get list of local variables */
        myLocalVarList = dec.getVariables();

        /* Get list of parameter variables */
        myParameterVarList = new edu.clemson.cs.r2jt.collections.List<VarDec>();
        Iterator<ParameterVarDec> it = dec.getParameters().iterator();
        while (it.hasNext()) {
            ParameterVarDec temp = it.next();
            VarDec newVarDec = new VarDec(temp.getName(), temp.getTy());
            myParameterVarList.add(newVarDec);
        }
    }

    @Override
    public void postProcedureDec(ProcedureDec dec) {
        /* Put the modified variable list into dec */
        dec.setVariables(myLocalVarList);

        /* Set myLocalVarList to null */
        myLocalVarList = null;
        myParameterVarList = null;
    }

    // -----------------------------------------------------------
    // Swap Statement
    // ----------------------------------------------------------- 

    @Override
    public void postSwapStmt(SwapStmt stmt) {
        /* Ancestor Iterator */
        Iterator<ResolveConceptualElement> it = this.getAncestorInterator();

        /* Variables */
        Location currentLocation = stmt.getLocation();
        edu.clemson.cs.r2jt.collections.List<ProgramExp> expList;

        /* Check if both left hand side and right hand side is a VariableArrayExp */
        if (stmt.getLeft() instanceof VariableArrayExp
                && stmt.getRight() instanceof VariableArrayExp) {
            /* Check if the names of the array is the same */
            VariableArrayExp left = (VariableArrayExp) stmt.getLeft();
            VariableArrayExp right = (VariableArrayExp) stmt.getRight();
            if (left.getName().getName().equals(right.getName().getName())) {
                /* Variables */
                expList =
                        new edu.clemson.cs.r2jt.collections.List<ProgramExp>();

                /* Create the arguments */
                expList.add(new VariableNameExp(stmt.getLocation(), null, left
                        .getName()));
                expList.add(left.getArgument());
                expList.add(right.getArgument());

                /* Create a CallStmt */
                CallStmt swapEntryStmt =
                        new CallStmt(null, new PosSymbol(currentLocation,
                                Symbol.symbol("Swap_Two_Entries")), expList);

                /* Add the created swap stmt to the right place */
                replaceStmt(it, stmt, swapEntryStmt);
            }
            /** TODO: Throw error message */
        } /* Check if left hand side or right hand side is a VariableArrayExp */
        else if (stmt.getLeft() instanceof VariableArrayExp
                || stmt.getRight() instanceof VariableArrayExp) {
            /* Variables */
            expList = new edu.clemson.cs.r2jt.collections.List<ProgramExp>();

            /* Create the argument list with the stmt's left and right */
            if (stmt.getLeft() instanceof VariableArrayExp) {
                /* Add the arguments necessary for Swap_Entry in Static_Array_Template */
                expList.add(new VariableNameExp(stmt.getLocation(), null,
                        ((VariableArrayExp) stmt.getLeft()).getName()));
                expList.add(stmt.getRight());
                expList.add(((VariableArrayExp) stmt.getLeft()).getArgument());
            }
            else {
                /* Add the arguments necessary for Swap_Entry in Static_Array_Template */
                expList.add(new VariableNameExp(stmt.getLocation(), null,
                        ((VariableArrayExp) stmt.getRight()).getName()));
                expList.add(stmt.getLeft());
                expList.add(((VariableArrayExp) stmt.getRight()).getArgument());
            }

            /* Create a CallStmt */
            CallStmt swapEntryStmt =
                    new CallStmt(null, new PosSymbol(currentLocation, Symbol
                            .symbol("Swap_Entry")), expList);

            /* Add the created swap stmt to the right place */
            replaceStmt(it, stmt, swapEntryStmt);

        } /* Check if left hand side and right hand side is a VariableDotExp */
        else if (stmt.getLeft() instanceof VariableDotExp
                && stmt.getRight() instanceof VariableDotExp) {
            /** TODO: Implement Swap_Two_Entries */
        } /* Check if left hand side or right hand side is a VariableDotExp */
        else if (stmt.getLeft() instanceof VariableDotExp
                || stmt.getRight() instanceof VariableDotExp) {
            /* Variables */
            boolean isLeft;
            edu.clemson.cs.r2jt.collections.List<VariableExp> variableExpList;
            expList = new edu.clemson.cs.r2jt.collections.List<ProgramExp>();

            if (stmt.getLeft() instanceof VariableDotExp) {
                variableExpList =
                        ((VariableDotExp) stmt.getLeft()).getSegments();
                isLeft = true;
            }
            else {
                variableExpList =
                        ((VariableDotExp) stmt.getRight()).getSegments();
                isLeft = false;
            }

            /* If this VariableDotExp is really a VariableArrayExp */
            VariableExp lastExp =
                    variableExpList.get(variableExpList.size() - 1);
            if (lastExp instanceof VariableArrayExp) {
                /* Create a new VariableNameExp */
                variableExpList.set(variableExpList.size() - 1,
                        new VariableNameExp(currentLocation, null,
                                ((VariableArrayExp) lastExp).getName()));

                /* Check if it is left or right */
                if (isLeft == true) {
                    VariableDotExp oldDotExp =
                            ((VariableDotExp) stmt.getLeft());
                    oldDotExp.setSegments(variableExpList);
                    expList.add(oldDotExp);
                    expList.add(stmt.getRight());
                }
                else {
                    VariableDotExp oldDotExp =
                            ((VariableDotExp) stmt.getRight());
                    oldDotExp.setSegments(variableExpList);
                    expList.add(oldDotExp);
                    expList.add(stmt.getLeft());
                }

                expList.add(((VariableArrayExp) lastExp).getArgument());

                /* Create a CallStmt */
                CallStmt swapEntryStmt =
                        new CallStmt(null, new PosSymbol(currentLocation,
                                Symbol.symbol("Swap_Entry")), expList);

                /* Add the created swap stmt to the right place */
                replaceStmt(it, stmt, swapEntryStmt);
            }
        }
    }

    //    // -----------------------------------------------------------
    //    // Variable Array Expression
    //    // ----------------------------------------------------------- 
    //    
    //    @Override
    //    public void preVariableArrayExp(VariableArrayExp exp) {       
    //        /* Variables */
    //    	Location currentLocation = exp.getLocation();
    //    	
    //    	/* Iterate through AST */
    //    	Iterator<ResolveConceptualElement> it = this.getAncestorInterator();
    //    	
    //    	while (it.hasNext()) {
    //    		/* Check if my parents looking for either a CallStmt or a ProgramParamExp */
    //    		ResolveConceptualElement temp = it.next();
    //    		
    //    		/* Callstmt */
    //    		if (temp instanceof CallStmt){
    //    			System.out.println("Swap Statement and Variable Declaration Needed At Line: " + currentLocation.toString());
    //    			System.out.println("CallStmt: " + ((CallStmt)temp).getName().getName());
    //    			System.out.println("VariableArrayExp: " + exp.toString());
    //    			
    //    			/* Find were we want to add the variable declaration */
    //    			Iterator<ResolveConceptualElement> it2 = this.getAncestorInterator();
    //    			while (it2.hasNext()) {
    //    				ResolveConceptualElement innerTemp = it.next();
    //    				
    //    				/* ProcedureDec */
    //    				if (innerTemp instanceof ProcedureDec) {
    //    					
    //    				} 
    //    				/* FacilityOperationDec */
    //    				else if (innerTemp instanceof FacilityOperationDec) {
    //    					
    //    				}
    //    			}
    //    		} else if (temp instanceof ProgramParamExp) {
    //    			System.out.println("Swap Statement and Variable Declaration Needed At Line: " + currentLocation.toString());
    //    			System.out.println("Function Call: " + ((ProgramParamExp)temp).getName().getName());
    //    		}
    //    	}
    //    }

    // -----------------------------------------------------------
    // Local Private Methods
    // ----------------------------------------------------------- 

    /** TODO: Comment */
    private void initVarDecList(
            edu.clemson.cs.r2jt.collections.List<Dec> decList) {
        /* Iterate through list of Decs looking for any FacilityTypeDec */
        Iterator<Dec> it = decList.iterator();
        while (it.hasNext()) {
            /* Temporary holder for the current item */
            Dec current = it.next();

            /* Check if it is a FacilityTypeDec or not */
            if (current instanceof FacilityTypeDec) {
                /* Create the list if null */
                if (myFacilityTypeList == null) {
                    myFacilityTypeList =
                            new edu.clemson.cs.r2jt.collections.List<FacilityTypeDec>();
                }

                /* Add current to our list */
                myFacilityTypeList.add((FacilityTypeDec) current);
            }
            /* Check if it is a global variable or not */
            else if (current instanceof VarDec) {
                /* Create the list if null */
                if (myGlobalVarList == null) {
                    myGlobalVarList =
                            new edu.clemson.cs.r2jt.collections.List<VarDec>();
                }

                /* Add current to our list */
                myGlobalVarList.add((VarDec) current);
            }
            /* Check if it is a RepresentationDec or not */
            else if (current instanceof RepresentationDec) {
                /* Create the list if null */
                if (myRepresentationDecList == null) {
                    myRepresentationDecList =
                            new edu.clemson.cs.r2jt.collections.List<RepresentationDec>();
                }

                /* Add current to our list */
                myRepresentationDecList.add((RepresentationDec) current);
            }
        }
    }

    /** TODO: Comment */
    private void addFacilityDec(Iterator<ResolveConceptualElement> it,
            FacilityDec newDec) {
        while (it.hasNext()) {
            /* Obtain a temp from it */
            ResolveConceptualElement temp = it.next();

            /* Check to see if it is an instance of FacilityModuleDec, FacilityOperationDec, 
             * ConceptBodyModuleDec, ProcedureDec or EnhancementBodyModuleDec */
            if (temp instanceof FacilityModuleDec) {
                /* Obtain a list of Decs from FacilityModuleDec */
                edu.clemson.cs.r2jt.collections.List<Dec> decList =
                        ((FacilityModuleDec) temp).getDecs();

                /* Add the FacilityDec created to decList */
                decList.add(0, newDec);

                /* Reinsert the modified list back into FacilityModuleDec */
                ((FacilityModuleDec) temp)
                        .setDecs((edu.clemson.cs.r2jt.collections.List<Dec>) decList);

                break;
            }
            else if (temp instanceof FacilityOperationDec) {
                /* Obtain a list of Decs from FacilityOperationDec */
                edu.clemson.cs.r2jt.collections.List<FacilityDec> decList =
                        ((FacilityOperationDec) temp).getFacilities();

                /* Add the FacilityDec created to decList */
                decList.add(0, newDec);

                /* Reinsert the modified list back into FacilityOperationDec */
                ((FacilityOperationDec) temp)
                        .setFacilities((edu.clemson.cs.r2jt.collections.List<FacilityDec>) decList);
                break;
            }
            else if (temp instanceof ConceptBodyModuleDec) {
                /* Obtain a list of Decs from ConceptBodyModuleDec */
                edu.clemson.cs.r2jt.collections.List<Dec> decList =
                        ((ConceptBodyModuleDec) temp).getDecs();

                /* Add the FacilityDec created to decList */
                decList.add(0, newDec);

                /* Reinsert the modified list back into ConceptBodyModuleDec */
                ((ConceptBodyModuleDec) temp)
                        .setDecs((edu.clemson.cs.r2jt.collections.List<Dec>) decList);

                break;
            }
            else if (temp instanceof ProcedureDec) {
                /* Obtain a list of FacilityDecs from ProcedureDec */
                edu.clemson.cs.r2jt.collections.List<FacilityDec> decList =
                        ((ProcedureDec) temp).getFacilities();

                /* Add the FacilityDec created to decList */
                decList.add(0, newDec);

                /* Reinsert the modified list back into ProcedureDec */
                ((ProcedureDec) temp)
                        .setFacilities((edu.clemson.cs.r2jt.collections.List<FacilityDec>) decList);
                break;
            }
            else if (temp instanceof EnhancementBodyModuleDec) {
                /* Obtain a list of FacilityDecs from EnhancementBodyModuleDec */
                edu.clemson.cs.r2jt.collections.List<Dec> decList =
                        ((EnhancementBodyModuleDec) temp).getDecs();

                /* Add the FacilityDec created to decList */
                decList.add(0, newDec);

                /* Reinsert the modified list back into EnhancementBodyModuleDec */
                ((EnhancementBodyModuleDec) temp)
                        .setDecs((edu.clemson.cs.r2jt.collections.List<Dec>) decList);

                break;
            }
        }
    }

    /** TODO: Comment */
    private void replaceStmt(Iterator<ResolveConceptualElement> it,
            Statement oldStmt, Statement newStmt) {
        /* Variables */
        edu.clemson.cs.r2jt.collections.List<Statement> stmtList = null;
        boolean found = false;
        ResolveConceptualElement parent;

        while (!found && it.hasNext()) {
            parent = it.next();

            /* Parent = FacilityOperationDec */
            if (parent instanceof FacilityOperationDec) {
                /* Get our list of statements */
                stmtList = ((FacilityOperationDec) parent).getStatements();
            }
            /* Parent = ProcedureDec */
            else if (parent instanceof ProcedureDec) {
                /* Get our list of statements */
                stmtList = ((ProcedureDec) parent).getStatements();
            }
            /* Parent = InitItem */
            else if (parent instanceof InitItem) {
                /* Get our list of statements */
                stmtList = ((InitItem) parent).getStatements();
            }
            /* Parent = IfStmt */
            else if (parent instanceof IfStmt) {
                /* Get our list of statements */
                stmtList = ((IfStmt) parent).getThenclause();

                /* Loop through until we find this current SwapStmt that we are on */
                for (int i = 0; i < stmtList.size() && found == false; i++) {
                    if (stmtList.get(i) == oldStmt) {
                        /* Replace SwapStmt with the newly created swapEntryStmt call */
                        stmtList.set(i, newStmt);
                        found = true;
                    }
                }

                /* Handle the Else Clause (oldStmt might be here) */
                if (((IfStmt) parent).getElseclause() != null && found != true) {
                    stmtList = ((IfStmt) parent).getElseclause();
                }
            }
            /* Parent = ConditionStmt */
            else if (parent instanceof ConditionItem) {
                /* Get our list of statements */
                stmtList = ((ConditionItem) parent).getThenclause();
            }
            /* Parent = IterativeStmt */
            else if (parent instanceof IterateStmt) {
                /* Get our list of statements */
                stmtList = ((IterateStmt) parent).getStatements();
            }
            /* Parent = IterativeExitStmt */
            else if (parent instanceof IterateExitStmt) {
                /* Get our list of statements */
                stmtList = ((IterateExitStmt) parent).getStatements();
            }
            /* Parent = SelectionStmt */
            else if (parent instanceof SelectionStmt) {
                /* Get our list of statements */
                stmtList = ((SelectionStmt) parent).getDefaultclause();
            }
            /* Parent = ChoiceItem */
            else if (parent instanceof ChoiceItem) {
                /* Get our list of statements */
                stmtList = ((ChoiceItem) parent).getThenclause();
            }
            /* Parent = WhileStmt */
            else if (parent instanceof WhileStmt) {
                /* Get our list of statements */
                stmtList = ((WhileStmt) parent).getStatements();
            }

            /* Make sure the list is not null */
            if (stmtList != null) {
                /* Loop through until we find this current SwapStmt that we are on */
                for (int i = 0; i < stmtList.size() && found == false; i++) {
                    if (stmtList.get(i) == oldStmt) {
                        /* Replace SwapStmt with the newly created swapEntryStmt call */
                        stmtList.set(i, newStmt);
                        found = true;
                    }
                }
            }
        }
    }

    private VariableNameExp checkForVariableArrayExp(Location currentLocation,
            VariableArrayExp temp) {
        /* Create new VariableNameExp */
        PosSymbol newName =
                new PosSymbol(currentLocation, Symbol.symbol("_Array_"
                        + ((VariableArrayExp) temp).getName().getName() + "_"
                        + myCounter++));
        VariableNameExp newVar =
                new VariableNameExp(currentLocation, null, newName);

        /* Create a new VarDec */
        VarDec arrayVarDec = iterateFindVarDec(temp.getName(), myLocalVarList);

        if (arrayVarDec == null) {
            arrayVarDec = iterateFindVarDec(temp.getName(), myParameterVarList);
        }

        if (arrayVarDec == null) {
            arrayVarDec = iterateFindVarDec(temp.getName(), myGlobalVarList);
        }

        NameTy ty = myArrayFacilityMap.get(arrayVarDec.getName().getName());
        if (ty == null) {
            ty =
                    myArrayFacilityMap.get(((NameTy) arrayVarDec.getTy())
                            .getName().getName());
        }

        VarDec newVarDec = new VarDec(newName, ty);

        /* Add it to our local variable list */
        myLocalVarList.add(newVarDec);

        edu.clemson.cs.r2jt.collections.List<ProgramExp> expList;
        expList = new edu.clemson.cs.r2jt.collections.List<ProgramExp>();

        /* Create the argument list and add the arguments necessary for Swap_Entry in Static_Array_Template */
        expList.add(new VariableNameExp(currentLocation, null,
                ((VariableArrayExp) temp).getName()));
        expList.add(newVar);
        expList.add(((VariableArrayExp) temp).getArgument());

        /* Create a CallStmt */
        CallStmt swapEntryStmt =
                new CallStmt(null, new PosSymbol(currentLocation, Symbol
                        .symbol("Swap_Entry")), expList);
        mySwapList.add(swapEntryStmt);

        /* Return created variable */
        return newVar;
    }

    private VariableNameExp createVariableNameExp(VariableArrayExp old) {
        /* Create a copy of temp and modify it's name */
        VariableNameExp modified = new VariableNameExp();
        PosSymbol oldName = modified.getName();
        PosSymbol newName =
                new PosSymbol(modified.getLocation(), Symbol.symbol("_RepArg_"
                        + oldName.getName() + "_" + myCounter++));
        modified.setName(newName);

        return modified;
    }

    private VarDec iterateFindVarDec(PosSymbol name,
            edu.clemson.cs.r2jt.collections.List<VarDec> list) {
        /* Variables */
        VarDec nextVarDec = null;

        /* Iterate through list of Local Variables */
        Iterator<VarDec> it = list.iterator();
        while (it.hasNext()) {
            /* Obtain nextDec from the iterator */
            nextVarDec = it.next();

            /* We found it */
            if (nextVarDec.getName().getName().compareTo(name.getName()) == 0) {
                break;
            }
        }

        return nextVarDec;
    }
}