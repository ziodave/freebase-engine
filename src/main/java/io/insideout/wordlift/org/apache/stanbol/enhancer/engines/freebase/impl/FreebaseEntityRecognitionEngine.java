package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import io.insideout.wordlift.org.apache.stanbol.domain.TextAnnotation;
import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freebase.impl.domain.FreebaseResult;
import io.insideout.wordlift.org.apache.stanbol.services.TextAnnotationService;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
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

    private final String freebaseBaseURI = "http://rdf.freebase.com/ns";

    // holds the site bound to this engine. it gets initialized in the Activate method, using the siteName
    // variable defined above.
    private final String siteName = "dbpedia";
    private Site site;

    @Reference
    private Entityhub entityHub;

    @Reference
    private SiteManager siteManager;

    @Reference
    private TextAnnotationService textAnnotationService;

    @Reference
    private FreebaseEntityRecognition freebaseEntityRecognition;

    public FreebaseEntityRecognitionEngine() {}

    public FreebaseEntityRecognitionEngine(Site site,
                                           TextAnnotationService textAnnotationService,
                                           FreebaseEntityRecognitionImpl freebaseEntityRecognition) {
        this.site = site;
        this.textAnnotationService = textAnnotationService;
        this.freebaseEntityRecognition = freebaseEntityRecognition;
    }

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);

        logger.trace("Freebase Entity Recognition engine is being activated.");

        logger.trace("The bound EntityHub is [{}].", entityHub.getClass());
        logger.trace("The bound SiteManager is [{}].", siteManager.getClass());

        site = siteManager.getSite(siteName);
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

        logger.trace("The bound Site [{}] is [{}].", new Object[] {siteName, site.getClass()});

        String defaultLanguage = EnhancementEngineHelper.getLanguage(ci);
        Collection<TextAnnotation> textAnnotations = textAnnotationService.getTextAnnotationsFromContentItem(
            ci, true);

        // process each annotation.
        for (TextAnnotation textAnnotation : textAnnotations) {

            // get the text and language to use for the search.
            String text = textAnnotation.getText();
            String language = (textAnnotation.getLanguageTwoLetterCode().isEmpty() ? defaultLanguage
                    : textAnnotation.getLanguageTwoLetterCode());

            logger.trace("Searching Freebase for [{}].", text);

            // perform the actual search.
            Collection<FreebaseResult> results = freebaseEntityRecognition.extractEntities(text, language);

            // continue to the next search if there are no results.
            if (null == results) continue;

            Set<UriRef> textAnnotationURIs = textAnnotation.getUriReference();

            for (FreebaseResult result : results) {
                String sameAsReference = freebaseBaseURI + result.getMid().replace("/m/", "/m.");

                FieldQueryFactory fieldQueryFactory = site.getQueryFactory();

                // // ##### E N T I T Y H U B Q U E R Y I N G #####
                // FieldQuery fieldQuery = entityHub.getQueryFactory().createFieldQuery();
                //
                // setFieldQueryParameters(fieldQuery, sameAsReference, language);
                //
                // QueryResultList<Entity> entities = null;
                // try {
                // entities = entityHub.findEntities(fieldQuery);
                // logger.trace("Found [{}] entities via the EntityHub for [{}].",
                // new Object[] {entities.size(), sameAsReference});
                // } catch (EntityhubException e) {
                // logger.error(
                // "An Entity Hub exception occured [{}] while looking for entities for [{}]:\n{}",
                // new Object[] {e.getClass(), sameAsReference, e.getMessage()}, e);
                // } catch (NullPointerException e) {
                // logger.error(
                // "An Entity Hub exception occured [{}] while looking for entities for [{}]:\n{}",
                // new Object[] {e.getClass(), sameAsReference, e.getMessage()}, e);
                // }

                FieldQuery fieldQuery = fieldQueryFactory.createFieldQuery();

                setFieldQueryParameters(fieldQuery, sameAsReference, language);

                QueryResultList<Entity> entities = null;
                try {
                    entities = site.findEntities(fieldQuery);
                } catch (SiteException e) {
                    logger.error("An Site exception occured [{}] while looking for entities for [{}]:\n{}",
                        new Object[] {e.getClass(), sameAsReference, e.getMessage()}, e);

                    //
                    continue;
                }

                logger.trace("Found [{}] entities via the Site for [{}].", new Object[] {entities.size(),
                                                                                         sameAsReference});

                // now write the results (requires write lock)
                MGraph graph = ci.getMetadata();
                ci.getLock().writeLock().lock();
                try {

                    for (Entity entity : entities) {
                        // Now create the entityAnnotation
                        UriRef contentItemID = ci.getUri();
                        Representation representation = entity.getRepresentation();
                        UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(graph,
                            this, contentItemID);

                        Iterator<String> fieldNamesIterator = entity.getRepresentation().getFieldNames();
                        while (fieldNamesIterator.hasNext())
                            logger.trace("field name [{}].", fieldNamesIterator.next());
                        Text labelText = representation.getFirst(
                            "http://www.w3.org/2000/01/rdf-schema#label", language);
                        if (null == labelText) labelText = representation.getText(
                            "http://www.w3.org/2000/01/rdf-schema#label").next();
                        PlainLiteralImpl label = new PlainLiteralImpl(labelText.getText(), new Language(
                                labelText.getLanguage()));
                        UriRef entityURI = new UriRef(representation.getId());

                        // add the URI references to the Text Annotations.
                        for (UriRef textAnnotationURI : textAnnotationURIs)
                            graph.add(new TripleImpl(entityAnnotation, DC_RELATION, textAnnotationURI));

                        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_REFERENCE, entityURI));
                        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_LABEL, label));
                        graph.add(new TripleImpl(entityAnnotation, ENHANCER_CONFIDENCE, LiteralFactory
                                .getInstance().createTypedLiteral(result.getScore())));

                        Iterator<org.apache.stanbol.entityhub.servicesapi.model.Reference> types = representation
                                .getReferences(RDF_TYPE.getUnicodeString());
                        while (types.hasNext()) {
                            graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_TYPE, new UriRef(types
                                    .next().getReference())));
                        }

                        graph.add(new TripleImpl(entityAnnotation, new UriRef(RdfResourceEnum.site.getUri()),
                                new PlainLiteralImpl(entity.getSite())));

                        graph.addAll(RdfValueFactory.getInstance().toRdfRepresentation(representation)
                                .getRdfGraph());
                    }

                } finally {
                    ci.getLock().writeLock().unlock();
                }

            }
        }

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

    private void setFieldQueryParameters(FieldQuery fieldQuery, String sameAsReference, String language) {
        fieldQuery.addSelectedField("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        fieldQuery.addSelectedField("http://www.w3.org/2000/01/rdf-schema#comment");
        fieldQuery.addSelectedField("http://www.w3.org/2000/01/rdf-schema#label");
        fieldQuery.addSelectedField("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
        fieldQuery.addSelectedField("http://www.w3.org/2003/01/geo/wgs84_pos#long");
        fieldQuery.addSelectedField("http://xmlns.com/foaf/0.1/depiction");
        fieldQuery.addSelectedField("http://dbpedia.org/ontology/thumbnail");

        fieldQuery.setConstraint("http://www.w3.org/2002/07/owl#sameAs", new ReferenceConstraint(
                sameAsReference));

        fieldQuery.setConstraint("http://www.w3.org/2000/01/rdf-schema#comment", new TextConstraint("",
                language));
        fieldQuery.setConstraint("http://www.w3.org/2000/01/rdf-schema#label", new TextConstraint("",
                language));
    }

}
