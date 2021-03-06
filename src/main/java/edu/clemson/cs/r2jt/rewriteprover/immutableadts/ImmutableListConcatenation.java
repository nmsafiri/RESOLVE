/*
 * ImmutableListConcatenation.java
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
package edu.clemson.cs.r2jt.rewriteprover.immutableadts;

import java.util.Iterator;

import edu.clemson.cs.r2jt.rewriteprover.iterators.ChainingIterator;

public class ImmutableListConcatenation<E> extends AbstractImmutableList<E> {

    private final ImmutableList<E> myFirstList;
    private final int myFirstListSize;

    private final ImmutableList<E> mySecondList;
    private final int mySecondListSize;

    private final int myTotalSize;

    public ImmutableListConcatenation(ImmutableList<E> firstList,
            ImmutableList<E> secondList) {

        myFirstList = firstList;
        myFirstListSize = myFirstList.size();

        mySecondList = secondList;
        mySecondListSize = mySecondList.size();

        myTotalSize = myFirstListSize + mySecondListSize;
    }

    @Override
    public E get(int index) {
        E retval;

        if (index < myFirstListSize) {
            retval = myFirstList.get(index);
        }
        else {
            retval = mySecondList.get(index - myFirstListSize);
        }

        return retval;
    }

    @Override
    public ImmutableList<E> head(int length) {
        ImmutableList<E> retval;

        if (length <= myFirstListSize) {
            retval = myFirstList.head(length);
        }
        else {
            retval = new ImmutableListConcatenation<E>(myFirstList,
                    mySecondList.head(length - myFirstListSize));
        }

        return retval;
    }

    @Override
    public Iterator<E> iterator() {
        return new ChainingIterator<E>(myFirstList.iterator(),
                mySecondList.iterator());
    }

    @Override
    public int size() {
        return myTotalSize;
    }

    @Override
    public ImmutableList<E> subList(int startIndex, int length) {
        return tail(startIndex).head(length);
    }

    @Override
    public ImmutableList<E> tail(int startIndex) {
        ImmutableList<E> retval;

        if (startIndex < myFirstListSize) {
            retval = new ImmutableListConcatenation<E>(
                    myFirstList.tail(startIndex), mySecondList);
        }
        else {
            retval = mySecondList.tail(startIndex - myFirstListSize);
        }

        return retval;
    }
}
