package edu.clemson.resolve.semantics.query;

import edu.clemson.resolve.semantics.*;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An implementation of {@link SymbolQuery SymbolQuery} that decorates an existing {@link SymbolQuery}, post
 * processing its results and returning the processed set of results.
 *
 * @param <T> The return type of the base {@link SymbolQuery}.
 * @param <R> The return type of the resultant, processed entries.
 */
public class ResultProcessingQuery<T extends Symbol, R extends Symbol> implements SymbolQuery<R> {

    @NotNull
    private final SymbolQuery<T> baseQuery;
    @NotNull
    private final SymbolTransformerFunction<T, R> mapping;

    public ResultProcessingQuery(@NotNull SymbolQuery<T> baseQuery,
                                 @NotNull SymbolTransformerFunction<T, R> mapping) {
        this.baseQuery = baseQuery;
        this.mapping = mapping;
    }

    @Override
    public List<R> searchFromContext(@NotNull Scope source, @NotNull MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException {
        List<R> result = new ArrayList<>();
        try {
            result.addAll(baseQuery.searchFromContext(source, repo).stream()
                    .map(mapping::apply)
                    .collect(Collectors.toList()));
        } catch (RuntimeException re) {
            if (re.getCause() instanceof UnexpectedSymbolException) {
                String symDesc = ((UnexpectedSymbolException) re.getCause())
                        .getTheUnexpectedSymbolDescription();
                throw new UnexpectedSymbolException(symDesc);
            }
        }
        return result;
    }

    //TODO: Unfuckingbelievable. "Google checked exception java 8 method ref" and have fun.
    @FunctionalInterface
    public interface SymbolTransformerFunction<V extends Symbol, U extends Symbol> extends Function<V, U> {

        @Override
        default U apply(V t) {
            try {
                return applyThrows(t);
            } catch (UnexpectedSymbolException e) {
                throw new RuntimeException(e);
            }
        }

        U applyThrows(V t) throws UnexpectedSymbolException;
    }
}
