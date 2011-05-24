/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.cxf.provider;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

// TODO copied from Apache CXF 2.4.0, remove with upgrade
public abstract class AbstractJsonpOutInterceptor extends AbstractPhaseInterceptor<Message> {
    
    protected AbstractJsonpOutInterceptor(String phase) {
        super(phase);
    }

    protected String getCallbackValue(Message message) {
        Exchange exchange = message.getExchange();
        return (String) exchange.get(JsonpInInterceptor.CALLBACK_KEY);
    }
    
    protected void writeValue(Message message, String value) throws Fault {
        HttpServletResponse response = (HttpServletResponse) message.get("HTTP.RESPONSE");
        try {
            response.getOutputStream().write(value.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new Fault(e);
        }
    }

}
