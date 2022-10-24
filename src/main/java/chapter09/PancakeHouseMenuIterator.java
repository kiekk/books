package chapter09;

import java.util.List;

public class PancakeHouseMenuIterator implements Iterator {
    List<MenuItem> items;
    int position = 0;

    public PancakeHouseMenuIterator(List<MenuItem> items) {
        this.items = items;
    }

    public MenuItem next() {
        return items.get(position++);
    }

    public boolean hasNext() {
        return items.size() > position;
    }
}