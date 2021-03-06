package org.aksw.jena_sparql_api.collection;

import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.util.Set;
import java.util.function.Predicate;

import org.aksw.facete3.app.vaadin.components.rdf.editor.TripleConstraint;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;

import com.google.common.collect.Sets;



/**
 * This is a mutable graph view based on filtering a delegate {@link ObservableGraph}'s triples.
 * Listeners registered on this class are wrapped with a filtering listener that gets registered
 * on the delegate.
 *
 * All access and modification methods (add/delete/remove/find/clear) only affect the set of triples
 * that match the given {@link TripleConstraint}.
 * Addition of triples for which the predicate tests to false are silently discarded.
 *
 *
 * @author raven
 *
 */
public class ObservableSubGraph
    extends GraphWithFilter
    implements ObservableGraph
{
//    public ObservableSubGraph(ObservableGraph graph, Predicate<? super Triple> predicate) {
//        super(graph, predicate);
//    }
//
//    public static ObservableSubGraph decorate(ObservableGraph graph, Predicate<? super Triple> predicate) {
//        return new ObservableSubGraph(graph, predicate);
//    }

    public ObservableSubGraph(ObservableGraph graph, TripleConstraint predicate) {
        super(graph, predicate);
    }

    public static ObservableSubGraph decorate(ObservableGraph graph, TripleConstraint predicate) {
        return new ObservableSubGraph(graph, predicate);
    }

    @Override
    public ObservableGraph get() {
        return (ObservableGraph)super.get();
    }


    public static <T> Set<T> filterSet(Set<T> set, Predicate<? super T> predicate) {
        return set == null ? null : Sets.filter(set, predicate::test);
    }

    public static CollectionChangedEvent<Triple> filter(Object self,
            CollectionChangedEvent<Triple> ev, TripleConstraint tripleConstraint) {
        return new CollectionChangedEventImpl<>(
            self,
            new GraphWithFilter((Graph)ev.getOldValue(), tripleConstraint),
            new GraphWithFilter((Graph)ev.getNewValue(), tripleConstraint),
            filterSet((Set<Triple>)ev.getAdditions(), tripleConstraint),
            filterSet((Set<Triple>)ev.getDeletions(), tripleConstraint),
            filterSet((Set<Triple>)ev.getRefreshes(), tripleConstraint));
    }

    @Override
    public Runnable addVetoableChangeListener(VetoableChangeListener listener) {
        return get().addVetoableChangeListener(ev -> {
            CollectionChangedEvent<Triple> newEv = filter(this, (CollectionChangedEvent<Triple>)ev, predicate);

            if (newEv.hasChanges()) {
                listener.vetoableChange(newEv);
            }
        });

    }


    @Override
    public Runnable addPropertyChangeListener(PropertyChangeListener listener) {
        return get().addPropertyChangeListener(ev -> {
            CollectionChangedEvent<Triple> newEv = filter(this, (CollectionChangedEvent<Triple>)ev, predicate);

            if (newEv.hasChanges()) {
                listener.propertyChange(newEv);
            }
        });
    }
}
