package chapter08.list;

import java.util.AbstractList;

public class MyStringList extends AbstractList<String> {
    private final String[] myList;

    MyStringList(String[] strings) {
        myList = strings;
    }

    @Override
    public String get(int index) {
        return myList[index];
    }

    @Override
    public String set(int index, String item) {
        String oldString = myList[index];
        myList[index] = item;
        return oldString;
    }

    @Override
    public int size() {
        return myList.length;
    }
}