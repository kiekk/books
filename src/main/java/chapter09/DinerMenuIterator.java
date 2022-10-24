package chapter09;

public class DinerMenuIterator implements Iterator {
    MenuItem[] items;
    int position = 0;

    public DinerMenuIterator(MenuItem[] items) {
        this.items = items;
    }

    public MenuItem next() {
        return items[position++];
    }

    public boolean hasNext() {
        return items.length > position;
    }
}