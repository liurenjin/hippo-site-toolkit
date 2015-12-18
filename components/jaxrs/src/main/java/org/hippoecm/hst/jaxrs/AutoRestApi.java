/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.jaxrs;

import java.util.HashMap;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.search.jcr.service.HippoJcrSearchService;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.QueryPersistService;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;
import org.onehippo.cms7.services.search.service.SearchServiceFactory;

@Produces("application/json")
public class AutoRestApi {

    public interface Context {
        Session getSession() throws RepositoryException;
    }

    private Context context;
    private SearchServiceFactory searchServiceFactory;

    public AutoRestApi() {
        this.context = new Context() {
            public Session getSession() throws RepositoryException {
                return RequestContextProvider.get().getSession();
            }
        };

        registerSearchServiceFactory();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    /* TODO
     * Refactor the code in SearchService some how to load this factory by simply including a dependency. Advanced
     * search contains a module that initialized this module, but that is part of the enterprise stack, see
     * com.onehippo.cms7.search.service.SearchModule
     */
    private void registerSearchServiceFactory() {
        if (HippoServiceRegistry.getService(SearchServiceFactory.class, Session.class.getName()) == null) {
            searchServiceFactory = new SearchServiceFactory() {
                @Override
                public SearchService createSearchService(final Object clientObject) throws SearchServiceException {
                    if (!(clientObject instanceof Session)) {
                        throw new SearchServiceException("Search service argument must be of type javax.jcr.Session");
                    }

                    final HippoJcrSearchService searchService = new HippoJcrSearchService();
                    searchService.setSession((Session) clientObject);
                    return searchService;
                }

                @Override
                public QueryPersistService createQueryPersistService(final Object clientObject) throws SearchServiceException {
                    if (!(clientObject instanceof Session)) {
                        throw new SearchServiceException("Search service argument must be of type javax.jcr.Session");
                    }

                    final HippoJcrSearchService searchService = new HippoJcrSearchService();
                    searchService.setSession((Session) clientObject);
                    return searchService;
                }
            };

            HippoServiceRegistry.registerService(searchServiceFactory, SearchServiceFactory.class, Session.class.getName());
        }
    }

    private SearchService getSearchService() throws RepositoryException {
        SearchServiceFactory searchServiceFactory = HippoServiceRegistry.getService(SearchServiceFactory.class,
                Session.class.getName());
        if (searchServiceFactory == null) {
            throw new RepositoryException("Cannot get SearchServiceFactory from HippoServiceRegistry");
        }
        return searchServiceFactory.createSearchService(context.getSession());
    }

    final class Link {
        public String url;
    }

    final class SearchResultItem {
        public String name;
        public String uuid;
        public Link[] links;
    }

    final class SearchResult {
        public long offset;
        public long max;
        public long count;
        public boolean more;
        public long total;
        public SearchResultItem[] items;
    }

    final class Error {
        public int status;
        public String description;
        Error(int status, String description) {
            this.status = status;
            this.description = description;
        }
    }

    @GET
    @Path("/content/documents")
    public Response getDocuments() {
        try {
            SearchService searchService = getSearchService();
            Query query = searchService.createQuery()
                    .from("/content")
                    .ofType("hippo:document")
                    .returnParentNode()
                    .orderBy(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE)
                    .descending();
            QueryResult queryResult = searchService.search(query);

            SearchResult returnValue = new SearchResult();
            returnValue.offset = 0;
            returnValue.max = 100;
            returnValue.count = queryResult.getTotalHitCount();
            returnValue.more = false;
            returnValue.total = queryResult.getTotalHitCount();

            int count = (int) queryResult.getTotalHitCount();
            int current = 0;
            returnValue.items = new SearchResultItem[count];
            HitIterator iterator = queryResult.getHits();
            while (iterator.hasNext()) {
                Hit hit = iterator.nextHit();
                returnValue.items[current] = new SearchResultItem();
                String uuid = hit.getSearchDocument().getContentId().toIdentifier();
                Node node = context.getSession().getNodeByIdentifier(uuid);
                returnValue.items[current].name = node.getName();
                returnValue.items[current].uuid = uuid;
                returnValue.items[current].links = new Link[1];
                returnValue.items[current].links[0] = new Link();
                returnValue.items[current].links[0].url = uuid;
                current++;
            }

            return Response.status(200).entity(returnValue).build();
        } catch (RepositoryException e) {
            return Response.status(500).entity(e.toString()).build();
        }
    }

    @GET
    @Path("/content/documents/{uuid}")
    public Response getDocumentsByUUID(@PathParam("uuid") String uuid) {
        try {
            Session session = context.getSession();

            // this could throw a IllegalArgumentException
            UUID.fromString(uuid);

            // this could throw a ItemNotFoundException
            Node node = session.getNodeByIdentifier(uuid);

            HashMap<String, Object> returnValue = new HashMap<>();

            returnValue.put("this", "that");
            returnValue.put("something", returnValue.clone());

            return Response.status(200).entity(returnValue).build();
        } catch (IllegalArgumentException iae) {
            return buildErrorResponse(400, iae, "The string '" + uuid + "' is not a valid UUID.");
        } catch (ItemNotFoundException infe) {
            return buildErrorResponse(404, infe);
        } catch (RepositoryException re) {
            return buildErrorResponse(500, re);
        }
    }

    private Response buildErrorResponse(int status, Exception exception) {
        return buildErrorResponse(status, exception, null);
    }

    private Response buildErrorResponse(int status, Exception exception, String description) {
        Error error;
        if (description == null) {
            error = new Error(status, exception.toString());
        } else {
            error = new Error(status, description);
        }
        return Response.status(status).entity(error).build();
    }

    /*
    @GET
    @Path("/content/{type}")
    public Response getNews(@PathParam("type") String type) {
        return Response.status(500).entity("{\"message\":\"non supported yet\"}").build();
    }

    @GET
    @Path("/content/{type}/{uuid}")
    public Response getNewsByUUID(@PathParam("type") String type, @PathParam("uuid") String uuid) {
        return Response.status(500).entity("{\"message\":\"non supported yet\"}").build();
    }

    @GET
    @Path("/types")
    public Response getTypes() {
        return Response.status(500).entity("{\"message\":\"non supported yet\"}").build();
    }

    @GET
    @Path("/types/{name}")
    public Response getTypesByName(@PathParam("name") String name) {
        return Response.status(500).entity("{\"message\":\"non supported yet\"}").build();
    }

    @GET
    @Path("/content/[{path}]")
    public Response getFolderContentByPath(@PathParam("path") String path) {
        return Response.status(500).entity("{\"message\":\"non supported yet\"}").build();
    }
    */

}
