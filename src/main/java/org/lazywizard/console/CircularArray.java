package org.lazywizard.console;

// https://github.com/halls7588/Data_Structures_in_15_Languages/tree/master
// is MIT licensed

/*******************************************************
 *  CircularArray.java
 *  Created by Stephen Hall on 9/22/17.
 *  Copyright (c) 2017 Stephen Hall. All rights reserved.
 *  A Circular Array implementation in Java
 ********************************************************/

import org.jetbrains.annotations.Nullable;

/**
 * Circular Array Class
 * @param <T> Generic type
 */
public class CircularArray<T> {

    /**
     * Private Members
     */
    private T[] array;
    private int count;
    private int size;
    private int zeroIndex;

    /**
     * CircularArray default constructor
     */
    public CircularArray(){
        this(10);
    }

    /**
     * CircularArray constructor initialized to a specific size
     * @param size Size to initialize the array to
     */
    @SuppressWarnings("unchecked")
    public CircularArray(int size){
        count = zeroIndex = 0;
        this.size = size;
        array = (T[]) new Object[this.size];
    }

    /**
     * Adds new item into the array, removes first element if at capacity
     * @param data Data to add into the array
     * @return Data added into the array
     */
    public T add(T data) {
        int tmp = (zeroIndex + count) % size;
        if(count == size) {
            zeroIndex = (zeroIndex + 1) % size;
            array[tmp] = data;
        } else {
            array[tmp] = data;
            count++;
        }
        return array[tmp];
    }

    /**
     * Gets the data at the arrays given index
     * @param index Index to get data at
     * @return Data at the given index or default value of T if index does not exist
     */
    @Nullable
    public T dataAt(int index) {
        if ((index + zeroIndex) % size < count && array[(index + zeroIndex) % size] != null){
            return (array[(index + zeroIndex) % size]);
        }
        return null;
    }

    /**
     * Removes the data at arrays given index
     * @param index Index to remove
     * @return Data removed from the array or default T value if index does not exist
     */
    @Nullable
    public T remove(int index){
        if (index > size)
            return null;

        T tmp = array[((index + zeroIndex) % size)];
        array[((index + zeroIndex) % size)] = array[zeroIndex];
        array[zeroIndex] = null;
        count--;
        zeroIndex = (zeroIndex + 1) % size;
        return tmp;
    }

    /**
     * Gets the current count of the array
     * @return Number of items in the array
     */
    public int count(){
        return count;
    }

    public int getCapacity() {
        return size;
    }

    /**
     * Private method to resize the array if capacity has been reached
     */
//    @SuppressWarnings("unchecked")
//    private void resize() {
//        T[] arr = (T[])new Object[size * 2];
//        for(int i = 0; i < count; i++) {
//            arr[i] = array[(zeroIndex + i) % size];
//        }
//        size *= 2;
//        zeroIndex = 0;
//        array = arr;
//    }

    public void print() {
        for(int i = 0; i < count; i++) {
            System.out.println("i: " + ((zeroIndex + i) % size));
            System.out.println(array[((zeroIndex + i) % size)]);
        }
        System.out.println("done");
    }

}