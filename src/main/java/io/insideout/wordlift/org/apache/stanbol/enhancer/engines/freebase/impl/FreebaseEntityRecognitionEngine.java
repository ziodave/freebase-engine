package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl;

import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl.domain.FreebaseResult;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, specVersion = "1.1", metatype = true, immediate = true, inherit = true)
@Service
@org.apache.felix.scr.annotations.Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME)})
public class FreebaseEntityRecognitionEngine extends
        AbstractEnhancementEngine<RuntimeException,RuntimeException> implements EnhancementEngine,
        ServiceProperties {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);

        logger.trace("Freebase Entity Recognition engine is being activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);

        logger.trace("Freebase Entity Recognition engine is being deactivated.");
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        return ENHANCE_ASYNC;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {

        FreebaseEntityRecognition entityRecognition = new FreebaseEntityRecognition();
        Collection<FreebaseResult> results = entityRecognition.extractEntities(query, language, indent);

        SparqlFieldQueryFactory factory = SparqlFieldQueryFactory.getInstance();
        assertNotNull(factory);

        SparqlFieldQuery query = factory.createFieldQuery();
        assertNotNull(query);

        query.addSelectedField("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        query.addSelectedField("http://www.w3.org/2000/01/rdf-schema#comment");
        query.addSelectedField("http://www.w3.org/2000/01/rdf-schema#label");
        query.addSelectedField("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
        query.addSelectedField("http://www.w3.org/2003/01/geo/wgs84_pos#long");
        query.addSelectedField("http://xmlns.com/foaf/0.1/depiction");
        query.addSelectedField("http://dbpedia.org/ontology/thumbnail");

        query.setConstraint("http://www.w3.org/2002/07/owl#sameAs", new ReferenceConstraint(sameAsReference));

        query.setConstraint("http://www.w3.org/2000/01/rdf-schema#comment", new TextConstraint("", language));
        query.setConstraint("http://www.w3.org/2000/01/rdf-schema#label", new TextConstraint("", language));

        String sparqlSelect = query.toSparqlSelect(true);

        // EntitySearcher entitySearcher;
        // // if (Entityhub.ENTITYHUB_IDS.contains(referencedSiteName.toLowerCase())) {
        // entitySearcher = new EntityhubSearcher(context.getBundleContext(), 10);
        // // } else {
        // entitySearcher = new ReferencedSiteSearcher(context.getBundleContext(), referencedSiteName, 10);
        // // }
        //
        // entitySearcher.lookup(config.getNameField(), config.getSelectedFields(), searchStrings, state
        // .getSentence().getLanguage(), config.getDefaultLanguage());
        //
        // entitySearcher.lookup(config.getNameField(), config.getSelectedFields(), searchStrings, state
        // .getSentence().getLanguage(), config.getDefaultLanguage());
    }

}
