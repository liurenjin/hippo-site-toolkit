/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.security.AuthenticationProvider;
import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.TransientRole;
import org.hippoecm.hst.security.User;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * TestHippoAuthenticationProvider
 * @version $Id$
 */
public class TestHippoAuthenticationProvider extends AbstractHstTestCase {
    
    private AuthenticationProvider authenticationProvider;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Repository systemRepo = getRepository();
        Credentials systemCreds = new SimpleCredentials("admin", "admin".toCharArray());
        Repository userRepo = getRepository();
        
        authenticationProvider = new HippoAuthenticationProvider(systemRepo, systemCreds, userRepo);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testAuthentication() {
        User user = null;
        
        try {
            user = authenticationProvider.authenticate("admin", "admin".toCharArray());
            assertEquals("admin", user.getName());
        } catch (SecurityException e) {
            fail("Failed to log on by admin: " + e);
        }
        
        Set<Role> roleSet = authenticationProvider.getRolesByUsername(user.getName());
        assertTrue(roleSet.contains(new TransientRole("admin")));
    }

}
