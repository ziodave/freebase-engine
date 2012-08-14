package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl;

import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl.domain.FreebaseResult;

import java.util.Collection;

import org.junit.Test;

public class TestFreebaseEntityRecognition {

    @Test
    public void test() {

        FreebaseEntityRecognitionImpl entityRecognition = new FreebaseEntityRecognitionImpl();
        Collection<FreebaseResult> results = entityRecognition.extractEntities("Veronica Lario", "it");

        for (FreebaseResult result : results) {
            System.out.println(result.toString());
        }
    }
}
