/**
 * Registry.java
 * ---------------------------------
 * Copyright (c) 2015
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.r2jt.congruenceclassprover;

import edu.clemson.cs.r2jt.typeandpopulate.MTFunction;
import edu.clemson.cs.r2jt.typeandpopulate.MTType;
import edu.clemson.cs.r2jt.typereasoning.TypeGraph;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by mike on 4/3/2014.
 */
public class Registry {

    public final String m_ccFormat = "¢c%03d";
    public final String m_cvFormat = "¢v%03d";
    public TreeMap<String, Integer> m_symbolToIndex;
    public Map<MTType, TreeSet<String>> m_typeToSetOfOperators;
    public ArrayList<String> m_indexToSymbol;
    public ArrayList<MTType> m_indexToType;
    public ArrayList<Integer> m_symbolIndexParentArray;
    public Stack<Integer> m_unusedIndices;
    private int m_uniqueCounter = 0;
    protected TypeGraph m_typeGraph;
    protected Map<String, Set<Integer>> m_appliedTheoremDependencyGraph;
    protected Set<String> m_lambda_names;
    protected Set<String> m_partTypes;
    protected Map<Integer, ArrayList<Integer>> m_partTypeParentArray;
    protected Set<String> m_commutative_operators;
    protected Map<String, Boolean> m_cached_isSubtype;
    public static enum Usage {

        LITERAL, FORALL, SINGULAR_VARIABLE, CREATED, HASARGS_SINGULAR,
        HASARGS_FORALL
    };

    protected final Map<String, Usage> m_symbolToUsage;
    private final Set<String> m_foralls;
    protected Map<String, MTType> m_typeDictionary;

    public Registry(TypeGraph g) {
        m_symbolToIndex = new TreeMap<String, Integer>();
        m_typeToSetOfOperators = new HashMap<MTType, TreeSet<String>>();
        m_indexToSymbol = new ArrayList<String>();
        m_indexToType = new ArrayList<MTType>();
        m_symbolIndexParentArray = new ArrayList<Integer>();
        m_unusedIndices = new Stack<Integer>();
        m_symbolToUsage = new HashMap<String, Usage>(2048, .5f); // entries won't change
        m_foralls = new HashSet<String>();
        m_typeGraph = g;
        m_typeDictionary = new TreeMap<String, MTType>();
        addSymbol("=", new MTFunction(g, g.BOOLEAN, g.ENTITY, g.ENTITY),
                Usage.LITERAL); // = as a predicate function, not as an assertion
        addSymbol("true", g.BOOLEAN, Usage.LITERAL);
        addSymbol("false", g.BOOLEAN, Usage.LITERAL);
        addSymbol("not", new MTFunction(g, g.BOOLEAN, g.BOOLEAN),
                Usage.HASARGS_SINGULAR);
        assert (getIndexForSymbol("=") == 0);
        m_appliedTheoremDependencyGraph = new HashMap<String, Set<Integer>>();
        m_lambda_names = new HashSet<String>();
        m_partTypes = new HashSet<String>();
        m_partTypeParentArray = new HashMap<Integer, ArrayList<Integer>>();
        // could look for these in theorems instead
        m_commutative_operators = new HashSet<String>();
        m_commutative_operators.add("+");
        m_commutative_operators.add("=");
        m_commutative_operators.add("and");
        m_commutative_operators.add("or");
        m_cached_isSubtype = new HashMap<String, Boolean>();
    }

    public boolean isSubtype(MTType a, MTType b) {
        String catKey = a.toString() + "," + b.toString();
        if (m_cached_isSubtype.containsKey(catKey)) {
            return m_cached_isSubtype.get(catKey);
        }
        else {
            boolean is = a.isSubtypeOf(b);
            m_cached_isSubtype.put(catKey, is);
            return is;
        }
    }

    public Usage getUsage(String symbol) {
        return m_symbolToUsage.get(symbol);
    }

    public Set<String> getSetMatchingType(MTType t) {
        assert t != null : "request for null type";
        Set<String> rSet = new HashSet<String>();
        Set<MTType> allTypesInSet = m_typeToSetOfOperators.keySet();
        assert !m_typeToSetOfOperators.isEmpty() : "empty m_typeToSetOfOperator.keySet()";
        assert allTypesInSet != null : "null set in Registry.getSetMatchingType";
        // if there are subtypes of t, return those too
        for (MTType m : allTypesInSet) {
            assert m != null : "null entry in allTypesInSet";
            if (isSubtype(m, t)) {
                rSet.addAll(m_typeToSetOfOperators.get(m));
            }
        }
        if (m_typeToSetOfOperators.get(t) != null)
            rSet.addAll(m_typeToSetOfOperators.get(t));

        return rSet;
    }

    public Set<String> getParentsByType(MTType t) {
        Set<String> rSet = getSetMatchingType(t);
        Set<String> fSet = new HashSet<String>();
        for (String s : rSet) {
            int id = getIndexForSymbol(s);
            if (m_symbolIndexParentArray.get(id) == id) {
                fSet.add(s);
            }
        }
        return fSet;
    }

    /**
     *
     * @param opIndexA index that becomes parent of B
     * @param opIndexB index to be replaced by opIndexA
     */
    public void substitute(int opIndexA, int opIndexB) {
        MTType aType = getTypeByIndex(opIndexA);
        MTType bType = getTypeByIndex(opIndexB);

        // set usage to most restricted: i.e literal over created over forall
        // this is because the earliest now becomes the parent
        String aS = getSymbolForIndex(opIndexA);
        String bS = getSymbolForIndex(opIndexB);
        Usage a_us = getUsage(aS);
        Usage b_us = getUsage(bS);
        if (!a_us.equals(Usage.FORALL) && isSubtype(bType, aType)) {
            m_indexToType.set(opIndexA, bType);
        }
        if (a_us.equals(Usage.LITERAL) || b_us.equals(Usage.LITERAL)) {
            m_symbolToUsage.put(aS, Usage.LITERAL);
        }
        else if (a_us.equals(Usage.CREATED) || b_us.equals(Usage.CREATED)) {
            m_symbolToUsage.put(aS, Usage.CREATED);
        }
        if (m_partTypes.contains(bS))
            m_partTypes.add(aS);
        m_unusedIndices.push(opIndexB);
        m_symbolIndexParentArray.set(opIndexB, opIndexA);
    }

    protected int findAndCompress(int index) {
        // early return for parent
        if (m_symbolIndexParentArray.get(index) == index)
            return index;
        Stack<Integer> needToUpdate = new Stack<Integer>();
        assert index < m_symbolIndexParentArray.size() : "findAndCompress error";
        int parent = m_symbolIndexParentArray.get(index);
        while (parent != index) {
            needToUpdate.push(index);
            index = parent;
            parent = m_symbolIndexParentArray.get(index);
        }
        while (!needToUpdate.isEmpty()) {
            m_symbolIndexParentArray.set(needToUpdate.pop(), index);
        }

        return index;
    }

    public String getSymbolForIndex(int index) {
        assert index >= 0 : "invalid index: " + index
                + " in Registry.getSymbolForIndex";
        String rS = m_indexToSymbol.get(findAndCompress(index));
        assert rS.length() != 0 : "Blank symbol error";
        return rS;
    }

    public String getRootSymbolForSymbol(String sym) {
        if (m_symbolToIndex.containsKey(sym))
            return getSymbolForIndex(getIndexForSymbol(sym));
        else
            return "";
    }

    public MTType getTypeByIndex(int index) {
        return m_indexToType.get(findAndCompress(index));
    }

    public boolean isSymbolInTable(String symbol) {
        return m_symbolToIndex.containsKey(symbol);
    }

    public int getIndexForSymbol(String symbol) {
        assert m_symbolToIndex.get(symbol) != null : symbol + " not found"
                + m_symbolToIndex.toString();

        if (!m_symbolToIndex.containsKey(symbol)) {
            return -1;
        }
        int r = m_symbolToIndex.get(symbol);
        return findAndCompress(r);
    }

    public Set<String> getForAlls() {
        return m_foralls;
    }

    public int makeSymbol(MTType symbolType, boolean isVariable) {
        String symbolName = "";
        if (isVariable)
            symbolName = String.format(m_cvFormat, m_uniqueCounter++);
        else
            symbolName = String.format(m_ccFormat, m_uniqueCounter++);
        return addSymbol(symbolName, symbolType, Usage.CREATED);
    }

    // if symbol is new, it adds it, otherwise, it returns current int rep
    public int addSymbol(String symbolName, MTType symbolType, Usage usage) {
        symbolName = symbolName.replaceAll("\\p{Cc}", "");
        if (symbolName.contains("lambda"))
            m_lambda_names.add(symbolName);
        assert symbolName.length() != 0 : "blank symbol error in addSymbol";
        if (isSymbolInTable(symbolName)) {
            return getIndexForSymbol(symbolName);
        }
        if (symbolName.contains(".")) {
            m_partTypes.add(symbolName);
        }

        if (m_typeToSetOfOperators.containsKey(symbolType)) {
            m_typeToSetOfOperators.get(symbolType).add(symbolName);
        }
        else {
            TreeSet<String> t = new TreeSet<String>();
            t.add(symbolName);
            assert symbolType != null : symbolName + " has null type";
            if (symbolType != null) {
                m_typeToSetOfOperators.put(symbolType, t);
                m_typeDictionary.put(symbolType.toString().replace("'", ""),
                        symbolType);
            }
        }

        m_symbolToUsage.put(symbolName, usage);
        if (usage.equals(Usage.FORALL) || usage.equals(Usage.HASARGS_FORALL)) {
            m_foralls.add(symbolName);
        }
        int incomingsize = m_symbolToIndex.size();
        m_symbolToIndex.put(symbolName, m_symbolToIndex.size());
        m_indexToSymbol.add(symbolName);
        m_indexToType.add(symbolType);
        m_symbolIndexParentArray.add(incomingsize);
        assert m_symbolToIndex.size() == m_indexToSymbol.size();
        assert incomingsize < m_symbolToIndex.size();
        return m_symbolToIndex.size() - 1;
    }

    public void flushUnusedSymbols() {}

    public Set<String> getFunctionNames() {
        HashSet<String> rSet = new HashSet<String>();
        for (Entry<String, Usage> e : m_symbolToUsage.entrySet()) {
            if (e.getValue().equals(Usage.HASARGS_SINGULAR)) {
                rSet.add(e.getKey());
            }
        }
        return rSet;
    }

    public boolean isCommutative(String op) {
        return m_commutative_operators.contains(op);
    }

    public boolean isCommutative(int opNum) {
        String root = getSymbolForIndex(opNum);
        return isCommutative(root);
    }

    // use sparingly, call with a parent symbol.  assumes parent array is compressed
    protected Set<String> getChildren(String parent) {
        int pInt = getIndexForSymbol(parent);
        HashSet<Integer> ch = new HashSet<Integer>();
        for (int i = 0; i < m_symbolIndexParentArray.size(); ++i) {
            if (i == pInt)
                continue;
            if (m_symbolIndexParentArray.get(i) == pInt) {
                ch.add(i);
            }
        }
        HashSet<String> rSet = new HashSet<String>();
        for (Integer i : ch) {
            rSet.add(m_indexToSymbol.get(i));
        }
        return rSet;
    }
}
