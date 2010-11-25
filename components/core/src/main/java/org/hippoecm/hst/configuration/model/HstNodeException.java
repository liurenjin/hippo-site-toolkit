/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.configuration.model;

/**
 * The <CODE>{@link HstNodeException}</CODE> class defines a general exception for the Hst Config
 * 
 */
public class HstNodeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link HstNodeException} exception.
     */
    public HstNodeException() {
        super();
    }

    /**
     * Constructs a new {@link HstNodeException} exception with the given message.
     *
     * @param   message
     *          the exception message
     */
    public HstNodeException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link HstNodeException} exception with the nested exception.
     *
     * @param   nested
     *          the nested exception
     */
    public HstNodeException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs a new {@link HstNodeException}
     *
     * @param   msg
     *          the exception message
     * @param   nested
     *          the nested exception
     */
    public HstNodeException(String msg, Throwable nested) {
        super(msg, nested);
    }
    
}
