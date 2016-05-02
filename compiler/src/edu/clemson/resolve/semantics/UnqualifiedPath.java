package edu.clemson.resolve.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy;
import edu.clemson.resolve.semantics.MathSymbolTable.ImportStrategy;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.searchers.SymbolTypeSearcher;
import edu.clemson.resolve.semantics.searchers.TableSearcher;
import edu.clemson.resolve.semantics.searchers.TableSearcher.SearchContext;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.*;

/**
 * Defines the search path used when a symbol is referenced in an unqualified way, along with some parameters for
 * tweaking how the search is accomplished. In general, the path is as follows:
 * <ol>
 * <li>Search the local scope.</li>
 * <li>Search any facilities declared in the local scope.</li>
 * <li>Search any imports in a depth-first manner, skipping any already-searched scopes.</li>
 * <ul>
 * <li>For each searched import, search any facilities declared inside.</li>
 * </ul>
 * </ol>
 * <p>
 * Instances of this class can be parameterized to search only direct imports or to exclude all imports, as well as
 * to exclude searching facilities, or change how generics are handled when searching facilities.</p>
 * <p>
 * Additionally, by setting the {@code localPriority} flag, the search can be made to stop without considering imports
 * (regardless of the import strategy) if at least one local match is found. Note that any local facilities will still
 * be searched if the facility strategy requires it.</p>
 */
public class UnqualifiedPath implements ScopeSearchPath {

    @NotNull
    private final ImportStrategy importStrategy;
    @NotNull
    private final FacilityStrategy facilityStrategy;
    private final boolean localPriority;

    public UnqualifiedPath(@NotNull ImportStrategy imports,
                           @NotNull FacilityStrategy facilities,
                           boolean localPriority) {
        this.importStrategy = imports;
        this.facilityStrategy = facilities;
        this.localPriority = localPriority;
    }

    @NotNull
    @Override
    public <E extends Symbol> List<E> searchFromContext(@NotNull TableSearcher<E> searcher,
                                                        @NotNull Scope source,
                                                        @NotNull MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException {
        List<E> result = new ArrayList<>();
        Set<Scope> searchedScopes = new HashSet<>();
        Map<String, ProgType> genericInstantiations = new HashMap<>();
        searchModule(searcher, source, repo, result, searchedScopes, genericInstantiations, null, importStrategy, 0);
        return result;
    }

    private <E extends Symbol> boolean searchModule(@NotNull TableSearcher<E> searcher,
                                                    @NotNull Scope source,
                                                    @NotNull MathSymbolTable repo,
                                                    @NotNull List<E> results,
                                                    @NotNull Set<Scope> searchedScopes,
                                                    @NotNull Map<String, ProgType> genericInstantiations,
                                                    @Nullable FacilitySymbol instantiatingFacility,
                                                    @NotNull ImportStrategy importStrategy, int depth)
            throws DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException {

        //First we search locally
        boolean finished =
                source.addMatches(searcher, results, searchedScopes, genericInstantiations, instantiatingFacility,
                        SearchContext.SOURCE_MODULE);

        if (searcher instanceof SymbolTypeSearcher &&
                !finished && facilityStrategy != FacilityStrategy.FACILITY_IGNORE) {
            finished = searchFacilities(searcher, results, source, genericInstantiations, searchedScopes, repo);
        }

        //Finally, if requested, we search imports
        if ((results.isEmpty() || !localPriority) && source instanceof SyntacticScope &&
                importStrategy != ImportStrategy.IMPORT_NONE) {

            SyntacticScope sourceAsSyntacticScope = (SyntacticScope) source;
            ModuleScopeBuilder module = repo.getModuleScope(sourceAsSyntacticScope.getModuleIdentifier());
            List<ModuleIdentifier> imps = module.getImports();
            for (ModuleIdentifier i : imps) {
                finished =
                        searchModule(searcher, repo.getModuleScope(i), repo, results, searchedScopes,
                                genericInstantiations, instantiatingFacility, importStrategy.cascadingStrategy(),
                                depth + 1);
                if (finished) break;
            }
        }
        return finished;
    }

    public <E extends Symbol> boolean searchFacilities(@NotNull TableSearcher<E> searcher,
                                                       @NotNull List<E> result,
                                                       @NotNull Scope source,
                                                       @NotNull Map<String, ProgType> genericInstantiations,
                                                       @NotNull Set<Scope> searchedScopes, MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException {

        List<FacilitySymbol> facilities =
                source.getMatches(SymbolTypeSearcher.FACILITY_SEARCHER, SearchContext.SOURCE_MODULE);

        FacilitySymbol facility;

        boolean finished = false;
        Iterator<FacilitySymbol> facilitiesIter = facilities.iterator();
        ModuleParameterization facilityConcept;
        Scope facilityScope;
        while (!finished && facilitiesIter.hasNext()) {
            facility = facilitiesIter.next();
            facilityConcept = facility.getFacility().getSpecification();

            facilityScope = facilityConcept.getScope(facilityStrategy.equals(FacilityStrategy.FACILITY_INSTANTIATE));

            finished = facilityScope.addMatches(
                    searcher, result, searchedScopes, new HashMap<String, ProgType>(), null, SearchContext.FACILITY);

            // YS Edits
            // Search any enhancements in this facility declaration
            /*  if (!finished) {
                  List<ModuleParameterization> enhancementList =
                          facility.getEnhancements();
                  for (ModuleParameterization facEnh : enhancementList) {
                      // Obtain the scope for the enhancement
                      facilityScope =
                              facEnh
                                      .getScope(myFacilityStrategy
                                              .equals(FacilityStrategy.FACILITY_INSTANTIATE));
                      // Search and add matches.
                      finished =
                              facilityScope.addMatches(searcher, result,
                                      searchedScopes,
                                      new HashMap<String, ProgType>(), null,
                                      SearchContext.FACILITY);
                  }
              }*/
        }
        return finished;
    }
}
