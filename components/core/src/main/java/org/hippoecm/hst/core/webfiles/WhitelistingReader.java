/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.webfiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhitelistingReader {

    private static final Logger log = LoggerFactory.getLogger(WhitelistingReader.class);
    private static final String EOF = "eof";

    public static final WhitelistingReader emptyWhitelistingReader = new WhitelistingReader();
    private final Set<String> whitelist = new HashSet<>();


    private WhitelistingReader() {
    }

    public WhitelistingReader(final InputStream is) {
        try {
            final Lexer lexer = new Lexer(is);
            String nextToken = lexer.getNextToken();
            while (!EOF.equals(nextToken)) {
                whitelist.add(nextToken);
                nextToken = lexer.getNextToken();
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public Set<String> getWhitelist() {
        return whitelist;
    }

    private class Lexer {

        private final StreamTokenizer st;

        private Lexer(final InputStream is) {
            this(new BufferedReader(new InputStreamReader(is)));
        }

        private Lexer(final Reader reader) {
            st = new StreamTokenizer(reader);
            st.eolIsSignificant(false);
            st.lowerCaseMode(false);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.commentChar('#');
            st.commentChar('*');
            st.ordinaryChar('/');
            st.wordChars(' ', ' ');
            st.wordChars('a', 'z');
            st.wordChars('A', 'Z');
            st.wordChars('_', '_');
            st.wordChars('/', '/');
        }

        private String getNextToken() {
            try {
                int tokenType = st.nextToken();
                if (tokenType == StreamTokenizer.TT_EOF) {
                    return EOF;
                } else if (tokenType == StreamTokenizer.TT_WORD) {
                    return st.sval;
                } else if (tokenType == StreamTokenizer.TT_NUMBER) {
                    return String.valueOf(st.nval);
                } else {
                    return new String(new char[]{(char)tokenType});
                }
            } catch (IOException e) {
                log.warn("IOException while attempting to read input stream. ", e);
                return null;
            }
        }

    }

}
