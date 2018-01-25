package org.jboss.windup.graph.service;

import com.syncleus.ferma.Traversable;
import com.syncleus.ferma.VertexFrame;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.janusgraph.core.attribute.Text;
import org.janusgraph.util.datastructures.IterablesUtil;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.service.exception.NonUniqueResultException;
import org.jboss.windup.util.ExecutionStatistics;
import org.jboss.windup.util.FilteredIterator;
import org.jboss.windup.util.exception.WindupException;

import static org.jboss.windup.util.Util.NL;

public class GraphService<T extends WindupVertexFrame> implements Service<T>
{
    private final Class<T> type;
    private final GraphContext context;

    public GraphService(GraphContext context, Class<T> type)
    {
        this.context = context;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public static <T extends WindupVertexFrame> T refresh(GraphContext context, T frame)
    {
        Vertex v = context.getGraph().traversal().V((Long)frame.getId()).next();
        return (T) context.getFramed().frameElement(v, WindupVertexFrame.class);
    }

    @Override
    public void commit()
    {
        ExecutionStatistics.performBenchmarked("GraphService.commit", () ->
        {
            getGraphContext().getGraph().tx().commit();
            return null;
        });
    }

    /**
     * Create a new instance of the given {@link WindupVertexFrame} type. The ID is generated by the underlying graph database.
     */
    @Override
    public T create()
    {
        return ExecutionStatistics.performBenchmarked("GraphService.create", () -> context.getFramed().addFramedVertex(type));
    }

    @Override
    public T addTypeToModel(final WindupVertexFrame model)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.addTypeToModel", () -> GraphService.addTypeToModel(getGraphContext(), model, type));
    }

    protected Traversable<?, ?> findAllQuery()
    {
        return getGraphContext().getQuery(type);
    }

    @Override
    public List<T> findAll()
    {
        return (List<T>)findAllQuery().toList(type);
    }

    @Override
    public Iterable<T> findAllByProperties(final String[] keys, final String[] vals)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.findAllByProperties(" + Arrays.asList(keys) + ")", () ->
        {
            Traversable<?, ?> traversable = findAllQuery();
            for (int i = 0, j = keys.length; i < j; i++)
            {
                final String key = keys[i];
                final String val = vals[i];

                traversable = traversable.traverse(g ->
                    g.has(key, val)
                );
            }
            return (List<T>)traversable.toList(type);
        });
    }

    @Override
    public Iterable<T> findAllByProperty(final String key, final Object value)
    {
        return findAllByProperty(key, value, false);
    }

    /**
     * Allows optional filtering by model type, because getVertices(prop, value, type) 
     * does not filter by model type (it only frames the vertex as that type).
     * That can be used to prevent collisions if multiple models use the same property name (e.g. "name").
     * 
     * @param filterByType  If true, the items returned from the graph are further filtered by this service's model type.
     */
    public Iterable<T> findAllByProperty(final String key, final Object value, final boolean filterByType)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.findAllByProperty(" + key + ")", () -> {
            Class<?> typeSearch = filterByType ? WindupVertexFrame.class : type;
            final Iterator verticesIterator = context.getFramed().getFramedVertices(key, value, typeSearch);
            final List<T> vertices = new ArrayList<>();

            verticesIterator.forEachRemaining(v -> vertices.add((T)v));
            if (!filterByType)
                return vertices;

            return (Iterable<T>) () -> {
                final FilteredIterator.Filter<T> filter = new ModelTypeFilter<>(GraphService.this.type);
                return new FilteredIterator<T>(vertices.iterator(), filter);
            };
        });
    }

    @Override
    public Iterable<T> findAllWithoutProperty(final String key, final Object value)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.findAllWithoutProperty(" + key + ")", () -> {
            //return (List<T>)findAllQuery().traverse(g -> g.hasNot(key).or(g.has(key, P.neq(value)))).toList(type);
            return findAllQuery().getRawTraversal().not(__.has(key, P.eq(value))).toList()
                    .stream()
                    .map(v -> frame((Vertex)v))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public Iterable<T> findAllWithoutProperty(final String key)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.findAllWithoutProperty(" + key + ")", () -> {
            return (List<T>)findAllQuery().traverse(g -> g.hasNot(key)).toList(type);
        });
    }

    @Override
    public Iterable<T> findAllByPropertyMatchingRegex(final String key, final String... regex)
    {
        return ExecutionStatistics.performBenchmarked("GraphService.findAllByPropertyMatchingRegex(" + key + ")", () ->
        {
            if (regex.length == 0)
                return IterablesUtil.emptyIterable();

            final String regexFinal;
            if (regex.length == 1)
            {
                regexFinal = regex[0];
            }
            else
            {
                StringBuilder builder = new StringBuilder();
                builder.append("\\b(");
                int i = 0;
                for (String value : regex)
                {
                    if (i > 0)
                        builder.append("|");
                    builder.append(value);
                    i++;
                }
                builder.append(")\\b");
                regexFinal = builder.toString();
            }
            return (List<T>)findAllQuery().traverse(g -> g.has(key, Text.textRegex(regexFinal))).toList(type);
        });
    }

    /**
     * Returns the vertex with given ID framed into given interface.
     */
    @Override
    public T getById(Object id)
    {
        return context.getFramed().getFramedVertex(this.type, id);
    }

    @Override
    public T frame(Vertex vertex)
    {
        return getGraphContext().getFramed().frameElement(vertex, this.getType());
    }

    @Override
    public Class<T> getType()
    {
        return this.type;
    }

    protected Traversable<?, ?> getQuery()
    {
        return getGraphContext().getQuery(this.getType());
    }

    @Override
    public T getUnique() throws NonUniqueResultException
    {
        Iterable<T> results = findAll();

        if (!results.iterator().hasNext())
        {
            return null;
        }

        Iterator<T> iterator = results.iterator();
        T result = iterator.next();

        if (iterator.hasNext())
        {
            throw new NonUniqueResultException("Expected unique value, but returned non-unique.");
        }

        return result;
    }

    @Override
    public T getUniqueByProperty(String property, Object value) throws NonUniqueResultException
    {
        return getUniqueByProperty(property, value, false);
    }

    public T getUniqueByProperty(String property, Object value, boolean enforceType) throws NonUniqueResultException
    {
        Iterable<T> results = findAllByProperty(property, value, enforceType);

        T result = null;
        for (WindupVertexFrame item : results)
        {
            // There can be other types using the same property name.
            if (!type.isInstance(item))
                continue;

            if (result != null)
            {
                throw new NonUniqueResultException("Expected unique value, but returned non-unique: " + property + " Conflicting models:"
                        + NL + "\t" + StringUtils.join(item.getClass().getInterfaces(), ", ") + NL + "\t\t" + item.toPrettyString()
                        + NL + "\t" + StringUtils.join(result.getClass().getInterfaces(), ", ") + NL + "\t\t" + result.toPrettyString());
            }
            result = (T) item;
        }

        return result;
    }

    protected T getUnique(Traversal<?, ?> query)
    {
        List<?> results = query.toList();

        if (!results.iterator().hasNext())
        {
            return null;
        }

        Iterator<?> iter = results.iterator();
        Object resultObj = iter.next();

        if (iter.hasNext())
        {
            throw new NonUniqueResultException("Expected unique value, but returned non-unique.");
        }

        if (!(resultObj instanceof Vertex))
            throw new WindupException("Unrecognized type returned by framed query: " + resultObj);

        return frame((Vertex)resultObj);
    }

    protected GraphContext getGraphContext()
    {
        return context;
    }

    @Override
    public Transaction newTransaction()
    {
        return context.getGraph().tx();
    }

    /**
     * Adds the specified type to this frame, and returns a new object that implements this type.
     */
    public static <T extends WindupVertexFrame> T addTypeToModel(GraphContext graphContext, WindupVertexFrame frame, Class<T> type)
    {
        Vertex vertex = frame.getElement();
        graphContext.getGraphTypeManager().addTypeToElement(type, vertex);
        return graphContext.getFramed().frameElement(vertex, type);
    }

    /**
     * Removes the specified type from the frame.
     */
    public static <T extends WindupVertexFrame> WindupVertexFrame removeTypeFromModel(GraphContext graphContext, WindupVertexFrame frame, Class<T> type)
    {
        Vertex vertex = frame.getElement();
        graphContext.getGraphTypeManager().removeTypeFromElement(type, vertex);
        return graphContext.getFramed().frameElement(vertex, WindupVertexFrame.class);
    }

    @Override
    public void remove(final T model)
    {
        ExecutionStatistics.performBenchmarked("GraphService.commit", () ->
        {
            model.getElement().remove();
            return null;
        });
    }


    /**
     * Only accepts vertices of given type and it's subtypes.
     */
    static class ModelTypeFilter<E extends VertexFrame> implements FilteredIterator.Filter<E>
    {
        Class<E> type;

        public ModelTypeFilter(Class<E> type)
        {
            this.type = type;
        }

        public boolean accept(E item)
        {
            return this.type.isInstance(item);
        }
    }

}
