package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl;

import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl.domain.FreebaseResult;
import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl.services.FreebaseSearch;

import java.util.Collection;

public class FreebaseEntityRecognition {

    public Collection<FreebaseResult> extractEntities(String query, String language, boolean indent) {

        FreebaseSearchOptions freebaseSearchOptions = new FreebaseSearchOptions();
        freebaseSearchOptions.setLang(language);
        freebaseSearchOptions.setIndent(indent);
        freebaseSearchOptions.setLimit(50);
        freebaseSearchOptions
                .setMqlOutput("{\"id\":null, \"mid\":null, \"name\":null, \"type\":[{\"id\":null,\"name\":null}]}}");

        FreebaseSearch freebaseSearch = new FreebaseSearch();
        return freebaseSearch.search(query, freebaseSearchOptions);

    }

}
