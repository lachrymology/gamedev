package patagonia.edn.parser;

import java.util.*;

final class DefaultListFactory implements CollectionBuilder.Factory {
    public CollectionBuilder builder() {
        return new CollectionBuilder() {
            ArrayList<Object> list = new ArrayList<Object>();
            public void add(Object o) {
                list.add(o);
            }
            public Object build() {
                return new DelegatingList(list);
            }
        };
    }
}

final class DelegatingList<E> extends AbstractList<E> {
    final List<E> delegate;

    DelegatingList(List<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public E get(int index) {
        return delegate.get(index);
    }

}
