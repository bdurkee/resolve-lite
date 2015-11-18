package org.rsrg.semantics;

import org.rsrg.semantics.MathSymbolTable.FacilityStrategy;
import org.rsrg.semantics.MathSymbolTable.ImportStrategy;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.searchers.SymbolTypeSearcher;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.searchers.TableSearcher.SearchContext;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.*;

/**
 * Defines the search path used when a symbol is referenced in an
 * unqualified way, along with some parameters for tweaking how the search is
 * accomplished. In general, the path is as follows:
 * <ol>
 * <li>Search the local scope.</li>
 * <li>Search any facilities declared in the local scope.</li>
 * <li>Search any imports in a depth-first manner, skipping any already-searched
 * scopes.</li>
 * <ul>
 * <li>For each searched import, search any facilities declared inside.</li>
 * </ul>
 * </ol>
 * <p>
 * Instances of this class can be parameterized to search only direct imports or
 * to exclude all imports, as well as to exclude searching facilities, or change
 * how generics are handled when searching facilities.</p>
 * <p>
 * Additionally, by setting the {@code localPriority} flag, the search can be
 * made to stop without considering imports (regardless of the import strategy)
 * if at least one local match is found. Note that any local facilities will
 * still be searched if the facility strategy requires it.</p>
 */
public class UnqualifiedPath implements ScopeSearchPath {

    private final ImportStrategy importStrategy;
    private final FacilityStrategy facilityStrategy;
    private final boolean localPriority;

    public UnqualifiedPath(ImportStrategy imports, FacilityStrategy facilities,
            boolean localPriority) {
        this.importStrategy = imports;
        this.facilityStrategy = facilities;
        this.localPriority = localPriority;
    }

    @Override public <E extends Symbol> List<E> searchFromContext(
            TableSearcher<E> searcher, Scope source, MathSymbolTable repo)
            throws DuplicateSymbolException {

        List<E> result = new ArrayList<>();
        Set<Scope> searchedScopes = new HashSet<>();
        Map<String, PTType> genericInstantiations = new HashMap<>();

        searchModule(searcher, source, repo, result, searchedScopes,
                genericInstantiations, null, importStrategy, 0);

        return result;
    }

    private <E extends Symbol> boolean searchModule(TableSearcher<E> searcher,
            Scope source, MathSymbolTable repo, List<E> results,
            Set<Scope> searchedScopes,
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility,
            ImportStrategy importStrategy, int depth)
            throws DuplicateSymbolException {

        //First we search locally
        boolean finished =
                source.addMatches(searcher, results, searchedScopes,
                        genericInstantiations, instantiatingFacility,
                        SearchContext.SOURCE_MODULE);

        //Hws: Next, if requested, we search any local facilities.
        //Dtw edit: added temporary first condition.. I want import recursive to search facilities even though normally we don't want to (unless the thing is qualified)
        //
        // TODO: ^^ Ideally we wouldn't change anything in here, but instead just fix the queries themselves with a facility ignore
        //so we wouldn't even have to touch this class. This way we could just keep symbolTypeQuery w/ Facility_Instantiate, or Facility_Generic.
        if ( searcher instanceof SymbolTypeSearcher && !finished &&
                facilityStrategy != FacilityStrategy.FACILITY_IGNORE ) {
            finished =
                    searchFacilities(searcher, results, source,
                            genericInstantiations, searchedScopes, repo);
        }

        //Finally, if requested, we search imports
        if ( (results.isEmpty() || !localPriority)
                && source instanceof SyntacticScope
                && importStrategy != ImportStrategy.IMPORT_NONE ) {

            SyntacticScope sourceAsSyntacticScope = (SyntacticScope) source;
            ModuleScopeBuilder module =
                    repo.getModuleScope(sourceAsSyntacticScope
                            .getModuleID());
            List<String> imports = module.getImports();

            for (String s : module.getImports()) {
                finished =
                        searchModule(searcher, repo.getModuleScope(s),
                                repo, results, searchedScopes,
                                genericInstantiations,
                                instantiatingFacility,
                                importStrategy.cascadingStrategy(),
                                depth + 1);
                if ( finished ) break;
            }
        }
        return finished;
    }

    public <E extends Symbol> boolean searchFacilities(
            TableSearcher<E> searcher, List<E> result, Scope source,
            Map<String, PTType> genericInstantiations,
            Set<Scope> searchedScopes, MathSymbolTable repo)
            throws DuplicateSymbolException {

        List<FacilitySymbol> facilities =
                source.getMatches(SymbolTypeSearcher.FACILITY_SEARCHER,
                        SearchContext.SOURCE_MODULE);

        FacilitySymbol facility;

        boolean finished = false;
        Iterator<FacilitySymbol> facilitiesIter = facilities.iterator();
        ModuleParameterization facilityConcept;
        Scope facilityScope;
        while (!finished && facilitiesIter.hasNext()) {
            facility = facilitiesIter.next();
            facilityConcept = facility.getFacility().getSpecification();

            facilityScope =
                    facilityConcept.getScope(facilityStrategy
                            .equals(FacilityStrategy.FACILITY_INSTANTIATE));

            finished =
                    facilityScope.addMatches(searcher, result, searchedScopes,
                            new HashMap<String, PTType>(), null,
                            SearchContext.FACILITY);

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
                                      new HashMap<String, PTType>(), null,
                                      SearchContext.FACILITY);
                  }
              }*/
        }
        return finished;
    }
}
