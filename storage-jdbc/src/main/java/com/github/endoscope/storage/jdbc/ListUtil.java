package com.github.endoscope.storage.jdbc;

import java.util.ArrayList;
import java.util.List;

public class ListUtil {
    public static <T> List<List<T>> partition(List<T> list, int partitionSize) {
        List<List<T>> result = new ArrayList<List<T>>();
        int offset = 0;
        int size = list.size();
        for(int i = 0, j = partitionSize; i < size; i += j) {
            offset = i + j;
            if(offset >= size) {
                offset = size;
            }
            result.add(list.subList(i, offset));
        }
        return result;
    }
}
