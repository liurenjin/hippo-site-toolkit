/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.order.ObjectOrdererRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestHstSitePipeline
 */
public class TestHstSitePipeline {

    private static Logger log = LoggerFactory.getLogger(TestHstSitePipeline.class);

    private InitializationValve initializationValve;
    private CmsSecurityValve cmsSecurityValve;

    private LocalizationValve localizationValve;
    private SecurityValve securityValve;
    private ContextResolvingValve contextResolvingValve;
    private ActionValve actionValve;
    private ResourceServingValve resourceServingValve;
    private PageCachingValve pageCachingValve;
    private AggregationValve aggregationValve;

    private CleanupValve cleanupValve = new CleanupValve();
    private DiagnosticReportingValve diagnosticReportingValve = new DiagnosticReportingValve();

    @Before
    public void setUp() throws Exception {
        initializationValve = new InitializationValve();
        initializationValve.setValveName(toCamelCaseString(InitializationValve.class.getSimpleName()));

        cmsSecurityValve = new CmsSecurityValve();
        cmsSecurityValve.setValveName(toCamelCaseString(CmsSecurityValve.class.getSimpleName()));

        localizationValve = new LocalizationValve();
        localizationValve.setValveName(toCamelCaseString(LocalizationValve.class.getSimpleName()));

        securityValve = new SecurityValve();
        securityValve.setValveName(toCamelCaseString(SecurityValve.class.getSimpleName()));

        contextResolvingValve = new ContextResolvingValve();
        contextResolvingValve.setValveName(toCamelCaseString(ContextResolvingValve.class.getSimpleName()));

        actionValve = new ActionValve();
        actionValve.setValveName(toCamelCaseString(ActionValve.class.getSimpleName()));

        resourceServingValve = new ResourceServingValve();
        resourceServingValve.setValveName(toCamelCaseString(ResourceServingValve.class.getSimpleName()));

        pageCachingValve = new PageCachingValve();
        pageCachingValve.setValveName(toCamelCaseString(PageCachingValve.class.getSimpleName()));

        aggregationValve = new AggregationValve();
        aggregationValve.setValveName(toCamelCaseString(AggregationValve.class.getSimpleName()));

        cleanupValve = new CleanupValve();
        cleanupValve.setValveName(toCamelCaseString(CleanupValve.class.getSimpleName()));

        diagnosticReportingValve = new DiagnosticReportingValve();
        diagnosticReportingValve.setValveName(toCamelCaseString(DiagnosticReportingValve.class.getSimpleName()));
    }

    @Test
    public void testBasicValveOrdering() throws Exception {
        HstSitePipeline pipeline = new HstSitePipeline();

        pipeline.setInitializationValves(new Valve[] { initializationValve });
        pipeline.setProcessingValves(new Valve [] { localizationValve, securityValve, contextResolvingValve, actionValve, resourceServingValve, aggregationValve });
        pipeline.setCleanupValves(new Valve[] { cleanupValve });

        cmsSecurityValve.setAfterValves(toCamelCaseString(InitializationValve.class.getSimpleName()));
        pipeline.addInitializationValve(cmsSecurityValve);

        pageCachingValve.setAfterValves(toCamelCaseString(ActionValve.class.getSimpleName()));
        pageCachingValve.setBeforeValves(toCamelCaseString(AggregationValve.class.getSimpleName()));
        pipeline.addProcessingValve(pageCachingValve);

        diagnosticReportingValve.setAfterValves(toCamelCaseString(CleanupValve.class.getSimpleName()));
        pipeline.addCleanupValve(diagnosticReportingValve);

        Valve [] mergedProcessingValves = pipeline.mergeProcessingValves();
        log.info("merged processing valves: \n\t{}", StringUtils.join(mergedProcessingValves, "\n\t"));
        assertArrayEquals(new Valve [] {
                initializationValve,
                cmsSecurityValve,
                localizationValve,
                securityValve,
                contextResolvingValve,
                actionValve,
                resourceServingValve,
                pageCachingValve,
                aggregationValve
        }, mergedProcessingValves);

        Valve [] mergedCleanupValves = pipeline.mergeCleanupValves();
        log.info("merged cleanup valves: \n\t{}", StringUtils.join(mergedCleanupValves, "\n\t"));
        assertArrayEquals(new Valve [] {
                cleanupValve,
                diagnosticReportingValve
        }, mergedCleanupValves);
    }

    @Test
    public void testMultipleAfterValveOrdering() throws Exception {
        HstSitePipeline pipeline = new HstSitePipeline();

        pipeline.setInitializationValves(new Valve[] { initializationValve });
        pipeline.setProcessingValves(new Valve [] { localizationValve, securityValve, contextResolvingValve, actionValve, resourceServingValve, aggregationValve });
        pipeline.setCleanupValves(new Valve[] { cleanupValve });

        cmsSecurityValve.setAfterValves(toCamelCaseString(InitializationValve.class.getSimpleName()));
        pipeline.addInitializationValve(cmsSecurityValve);

        pageCachingValve.setAfterValves(toCamelCaseString(ActionValve.class.getSimpleName() +"," +toCamelCaseString(ResourceServingValve.class.getSimpleName())));
        pageCachingValve.setBeforeValves(toCamelCaseString(AggregationValve.class.getSimpleName()));
        pipeline.addProcessingValve(pageCachingValve);

        diagnosticReportingValve.setAfterValves(toCamelCaseString(CleanupValve.class.getSimpleName()));
        pipeline.addCleanupValve(diagnosticReportingValve);

        Valve [] mergedProcessingValves = pipeline.mergeProcessingValves();
        log.info("merged processing valves: \n\t{}", StringUtils.join(mergedProcessingValves, "\n\t"));
        assertArrayEquals(new Valve [] {
                initializationValve,
                cmsSecurityValve,
                localizationValve,
                securityValve,
                contextResolvingValve,
                actionValve,
                resourceServingValve,
                pageCachingValve,
                aggregationValve
        }, mergedProcessingValves);

    }

    @Test
    public void testIncorrectCircularValveOrdering() throws Exception {
        HstSitePipeline pipeline = new HstSitePipeline();

        pipeline.setInitializationValves(new Valve[] { initializationValve });
        pipeline.setProcessingValves(new Valve [] { localizationValve, securityValve, contextResolvingValve, actionValve, resourceServingValve, aggregationValve });
        pipeline.setCleanupValves(new Valve[] { cleanupValve });

        // order below should result in exception
        securityValve.setAfterValves("contextResolvingValve");
        contextResolvingValve.setAfterValves("actionValve");
        actionValve.setAfterValves("securityValve");

        try {
            Valve [] mergedProcessingValves = pipeline.mergeProcessingValves();
            fail("Expected to throw ObjectOrdererRuntimeException, but nothing thrown!!");
        } catch (ObjectOrdererRuntimeException e) {
            log.info("Expected ObjectOrdererRuntimeException on the intended circular ordering dependencies.");
        }
    }

    private static String toCamelCaseString(String s) {
        if (StringUtils.isEmpty(s)) {
            return s;
        }

        return new StringBuilder(s.length()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
    }
}
