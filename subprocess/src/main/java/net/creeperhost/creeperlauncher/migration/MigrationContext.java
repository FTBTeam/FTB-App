package net.creeperhost.creeperlauncher.migration;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import net.covers1624.quack.collection.TypedMap;
import net.covers1624.quack.sort.CyclePresentException;
import net.covers1624.quack.sort.TopologicalSort;
import net.covers1624.quack.util.SneakyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static net.covers1624.quack.collection.ColUtils.contains;

/**
 * Created by covers1624 on 13/1/21.
 */
@SuppressWarnings ("UnstableApiUsage")
public class MigrationContext {

    private final ImmutableList<MigratorState> migrators;
    private Map<Class<? extends Migrator>, MigratorState> migratorMap;
    private TypedMap blackboard = new TypedMap();

    public MigrationContext(ImmutableList<MigratorState> migrators) {
        this.migrators = migrators;
        migratorMap = migrators.stream().collect(Collectors.toMap(e -> e.migratorClass, identity()));
    }

    public static MigrationContext buildContext(Set<Class<? extends Migrator>> migratorClasses, int minStep) {
        Map<Class<? extends Migrator>, MigratorState> states = migratorClasses.stream()
                .map(MigratorState::new)
                .filter(e -> e.props.from() >= minStep)
                .collect(Collectors.toMap(e -> e.migratorClass, identity()));

        //Start building our graph, add each node to be sorrted.
        MutableGraph<MigratorState> graph = GraphBuilder.directed().build();
        states.values().forEach(graph::addNode);

        //Figure out what nodes depend on other nodes.
        for (MigratorState s1 : states.values()) {
            //Make sure all dependencies exist.
            for (Class<? extends Migrator> require : s1.props.requires()) {
                if (!states.containsKey(require)) {
                    String cs1 = s1.migratorClass.getName();
                    throw new IllegalStateException("Migrator '" + cs1 + "' declares unregistered dependency: " + require.getName());
                }
            }
            for (MigratorState s2 : states.values()) {
                if (s1 == s2) continue;//Pointless
                if (s1.props.from() < s2.props.to() && s1.props.to() != s2.props.to()) {
                    graph.putEdge(s1, s2);
                } else if (contains(s1.props.requires(), s2.migratorClass)) {
                    if (s1.props.from() != s2.props.from() && s1.props.to() != s2.props.to()) {
                        String cs1 = s1.migratorClass.getName();
                        String cs2 = s2.migratorClass.getName();
                        throw new IllegalStateException("'" + cs1 + "' Depends on '" + cs2 + "' but they don't share a from and to.");
                    }
                    graph.putEdge(s2, s1);
                }
            }
        }

        //Perform a topological sort on our graph. Nodes are guaranteed to be before their dependants,
        // but other than that, the order is undefined.
        try {
            List<MigratorState> sortedStates = TopologicalSort.topologicalSort(graph, null);
            return new MigrationContext(ImmutableList.copyOf(sortedStates));
        } catch (CyclePresentException e) {
            //Throw a meaningful exception
            StringBuilder builder = new StringBuilder("Cyclic Migrator dependencies detected.");
            for (Set<MigratorState> cycle : SneakyUtils.<Set<Set<MigratorState>>>unsafeCast(e.getCycles())) {
                builder.append("\n");
                boolean first = true;
                for (MigratorState s : cycle) {
                    if (first) {
                        builder.append("\t");
                    }
                    builder.append(s.migratorClass.getName());
                    if (first) {
                        builder.append(" <-> ");
                    } else {
                        builder.append("\n");
                    }
                    first = false;
                }
            }
            throw new RuntimeException(builder.toString(), e);
        }
    }

    @SuppressWarnings ("unchecked")
    public <T extends Migrator> T getMigrator(Class<T> clazz) {
        MigratorState state = migratorMap.get(clazz);
        if (!state.executed) throw new IllegalStateException("Migrator has not been executed yet.");
        return (T) state.migrator;
    }

    public TypedMap getBlackboard() {
        return blackboard;
    }

    ImmutableList<MigratorState> getMigrators() {
        return migrators;
    }

    static class MigratorState {

        public final Class<? extends Migrator> migratorClass;
        public final Migrator.Properties props;
        public final Migrator migrator;
        public boolean executed;

        public MigratorState(Class<? extends Migrator> migratorClass) {
            this.migratorClass = migratorClass;
            props = migratorClass.getAnnotation(Migrator.Properties.class);
            if (props == null) throw new RuntimeException("Migrator must have @MigrationHandler annotation.");
            try {
                this.migrator = migratorClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException("Failed to instantiate Migrator.", e);
            }
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", MigratorState.class.getSimpleName() + "[", "]")
                    .add("migratorClass=" + migratorClass)
                    .add("props=" + props)
                    .add("migrator=" + migrator)
                    .add("executed=" + executed)
                    .toString();
        }
    }
}
