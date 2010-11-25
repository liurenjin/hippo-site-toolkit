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
package org.hippoecm.hst.component.support.forms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;

/**
 * A simple Form object just holding a fieldname <--> fields HashMap
 */
public class FormMap {


    private Map<String, FormField> formMap = new HashMap<String, FormField>();
    // all messages
    private Map<String, List<String>> messages = null;
    // first messages
    private Map<String, String> message = null;
    private String predecessorUUID = null;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    // flag indicating form is sealed: sealed form data will not be displayed after subsequent reloads
    private boolean sealed;

    public FormMap() {
        // empty form
    }

    public FormMap(HstRequest request, List<String> fieldNames) {
        // lets populate from
        this(request, fieldNames.toArray(new String[fieldNames.size()]));
    }

    public FormMap(HstRequest request, String[] fieldNames) {
        for (String name : fieldNames) {
            String[] values = request.getParameterValues(name) == null ? EMPTY_STRING_ARRAY : request.getParameterValues(name);
            FormField field = new FormField(name);
            for (String value : values) {
                field.addValue(value);
            }
            formMap.put(name, field);
        }
    }

    public FormMap(Map<String, FormField> paramMap) {
        formMap.putAll(paramMap);
    }

    public void addFormField(String fieldName, FormField field) {
        formMap.put(fieldName, field);
    }

    public void addMessage(String name, String value) {
        FormField field = formMap.get(name);
        if (field == null) {
            field = new FormField();
            formMap.put(name, field);
        }
        field.addMessage(value);

    }

    public void setPrevious(String uuid) {
        this.predecessorUUID = uuid;
    }

    public FormField getField(String name) {
        return formMap.get(name);
    }

    public Map<String, FormField> getValue() {
        return formMap;
    }

    /**
     * For each of th fields we only fetch it's first error message
     * (if field contains error message(s)).
     *
     * @return map containing (single) field  (error) messages
     * @see FormField#getMessages()
     */
    public Map<String, String> getMessage() {
        if (message != null) {
            return message;
        } else {
            message = new HashMap<String, String>();
            for (FormField field : formMap.values()) {
                List<String> fieldMessages = field.getMessages();
                if (fieldMessages.size() > 0) {
                    message.put(field.getName(), fieldMessages.get(0));
                }
            }
        }
        return message;
    }

    /**
     * Returns all field (error) messages.
     * If you only need to display one message, per form field please see {@link #getMessage()}
     *
     * @return empty map or map of error messages
     */
    public Map<String, List<String>> getMessages() {
        if (messages != null) {
            return messages;
        } else {
            messages = new HashMap<String, List<String>>();
            for (FormField field : formMap.values()) {
                messages.put(field.getName(), field.getMessages());
            }
        }
        return messages;
    }

    public String getPrevious() {
        return this.predecessorUUID;
    }

    public Map<String, FormField> getFormMap() {
        return formMap;
    }

    public boolean isSealed() {
        return sealed;
    }

    /**
     * Seal this form data. Once sealed, data will not be read anymore when user reloads the page
     *
     * @param sealed boolean, if true, we'll finalize our form
     */
    public void setSealed(final boolean sealed) {
        this.sealed = sealed;
    }

    /**
     * Override this method if you need a different prefix
     *
     * @return the message prefix.
     */
    @Deprecated
    public String getMessageKeyPrefix() {
        return "msg-key::";
    }


}

