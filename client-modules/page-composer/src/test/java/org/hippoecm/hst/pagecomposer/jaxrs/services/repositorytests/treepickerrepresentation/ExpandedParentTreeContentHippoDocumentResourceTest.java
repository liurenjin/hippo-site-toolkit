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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.treepickerrepresentation;

import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExpandedParentTreeContentHippoDocumentResourceTest extends AbstractHippoDocumentResourceTest {

    @Test
    public void representation_for_siteMapPathInfo_returns_tree_containing_ancestors_with_siblings_and_direct_children() throws Exception {

        // about-us sitemap item as relative contentpath 'common/aboutfolder/about-us' and that path should be expanded

        TreePickerRepresentation representation =
                createPartialTreeRepresentation("", getRootContentRequestConfigIdentifier(), "about-us");

        aboutUsRepresentationAssertions(representation);

    }

    private void aboutUsRepresentationAssertions(final TreePickerRepresentation representation) {
        assertEquals("unittestproject",representation.getNodeName());
        assertEquals("expected 'common' and 'News' folder", 2, representation.getItems().size());

        // news folder should not be expanded!
        assertEquals("Folder News' should not be loaded/expanded ", 0, representation.getItems().get(1).getItems().size());


        // 'common' should be expanded
        final TreePickerRepresentation commonFolderRepresentation = representation.getItems().get(0);

        assertEquals("Folder 'common' should loaded/expanded ", 2, commonFolderRepresentation.getItems().size());
        assertFalse("Folder 'common' should not be able to be matched in sitemap ",commonFolderRepresentation.isSelectable());

        // ordered folder, home page first
        assertEquals("Home Page", commonFolderRepresentation.getItems().get(0).getDisplayName());
        assertFalse("Document 'Home Page' is not the selected one according 'about-us' sitemapPathInfo",
                commonFolderRepresentation.getItems().get(0).isSelected());

        final TreePickerRepresentation aboutFolderRepresentation = commonFolderRepresentation.getItems().get(1);
        assertEquals("aboutfolder", aboutFolderRepresentation.getDisplayName());
        assertFalse("Folder 'aboutfolder' should not be able to be matched in sitemap ", aboutFolderRepresentation.isSelectable());

        final TreePickerRepresentation aboutDocumentRepresentation = aboutFolderRepresentation.getItems().get(0);

        assertEquals("About Us", aboutDocumentRepresentation.getDisplayName());
        assertTrue(aboutDocumentRepresentation.isSelectable());
        assertTrue("Document 'About Us' is *selected* one according 'about-us' sitemapPathInfo",
                aboutDocumentRepresentation.isSelected());
    }

    @Test
    public void representation_for_siteMapPathInfo_that_can_be_matched_to_siteMapItem_default_but_not_to_valid_content_results_in_root_content_presentation() throws Exception {

        // 'matching_to_default_siteMapItem' will match _default_ which does not have a relative content path and as a result,
        // should return 'root content representation' since a wildcard site map item cannot be picked in the sitemap representation either
        TreePickerRepresentation representation =
                createPartialTreeRepresentation("", getRootContentRequestConfigIdentifier(), "matching_to_default_siteMapItem");

        rootContentRepresentationAssertions(representation);
    }

    @Test
    public void representation_for_siteMapPathInfo_that_can_be_matched_in_sitemap_to_explicit_item_without_relative_contentpath_results_in_sitemap_representation() throws Exception {

        TreePickerRepresentation representation =
                createPartialTreeRepresentation("", getRootContentRequestConfigIdentifier(), "contact");

        assertNotNull(representation);
        // TODO HSTTWO-3225
    }

    @Test
    public void representation_for_siteMapPathInfo_that_cannot_be_matched_in_sitemap_results_in_root_content_presentation() throws Exception {

        TreePickerRepresentation representation =
                createPartialTreeRepresentation("", getRootContentRequestConfigIdentifier(), "path/that/cannot/be/matched");

        rootContentRepresentationAssertions(representation);

    }

    @Test
    public void representation_for_siteMapPathInfo_for_non_root_content_request_config_identifier_uses_root_content_config_identifier() throws Exception {

        TreePickerRepresentation representation =
                createPartialTreeRepresentation("", getCommonFolderRequestConfigIdentifier(), "about-us");

        aboutUsRepresentationAssertions(representation);
    }
}
