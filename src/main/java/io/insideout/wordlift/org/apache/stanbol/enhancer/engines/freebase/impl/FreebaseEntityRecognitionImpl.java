package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl;

import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl.domain.FreebaseResult;
import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl.services.FreebaseSearch;

import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

@Component(immediate=true)
@Service
public class FreebaseEntityRecognitionImpl implements FreebaseEntityRecognition {

    private final String key = "AIzaSyDjiS-2oTprBk2twsCzFdXnEB2hrM3fwGw";
    private final int limit = 3;
    private final boolean indent = false;

    public Collection<FreebaseResult> extractEntities(String query, String language) {

        FreebaseSearchOptions freebaseSearchOptions = new FreebaseSearchOptions();
        freebaseSearchOptions.setLang(language);
        freebaseSearchOptions.setIndent(indent);
        freebaseSearchOptions.setLimit(limit);
        freebaseSearchOptions.setKey(key);
        freebaseSearchOptions
                .setMqlOutput("{\"id\":null, \"mid\":null, \"name\":null, \"type\":[{\"id\":null,\"name\":null}]}}");

        FreebaseSearch freebaseSearch = new FreebaseSearch();
        return freebaseSearch.search(query, freebaseSearchOptions);

    }

}
