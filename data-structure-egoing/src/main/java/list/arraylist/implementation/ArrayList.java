package list.arraylist.implementation;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArrayList {
    private int size = 0;
    private Object[] elementData = new Object[100];

    public boolean addFirst(Object element) {
        return add(0, element);
    }

    public boolean addLast(Object element) {
        elementData[size] = element;
        size++;
        return true;
    }

    public boolean add(int index, Object element) {
        for (int i = size - 1; i >= index; i--) {
            elementData[i + 1] = elementData[i];
        }
        elementData[index] = element;
        size++;
        return true;
    }

    public Object remove(int index) {
        Object removeElement = elementData[index];
        for (int i = index + 1; i <= size - 1; i++) {
            elementData[i - 1] = elementData[i];
        }
        size--;
        elementData[size] = null;
        return removeElement;
    }

    public Object removeFirst() {
        return remove(0);
    }

    public Object removeLast() {
        return remove(size - 1);
    }

    public Object get(int index) {
        return elementData[index];
    }

    public int size() {
        return size;
    }

    public int indexOf(Object element) {
        for (int i = 0; i <= size - 1; i++) {
            if (Objects.equals(element, elementData[i])) {
                return i;
            }
        }
        return -1;
    }

    public ListIterator listIterator() {
        return new ListIterator();
    }

    @Override
    public String toString() {
        return "[" + Arrays.stream(elementData).filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(", ")) + "]";
    }

    class ListIterator {
        private int nextIndex = 0;

        public Object next() {
            return elementData[nextIndex++];
        }

        public boolean hasNext() {
            return nextIndex < size();
        }
    }
}
