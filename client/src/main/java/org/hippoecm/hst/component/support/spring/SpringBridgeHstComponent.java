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
package org.hippoecm.hst.component.support.spring;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A bridge component which delegates all invocation to a bean managed by the spring IoC.
 * <p>
 * By default, the delegated bean's name should be configured in the component configuration
 * parameter value with the parameter name, 'spring-delegated-bean-param-name'.
 * This bridge component will retrieve the bean from the spring web application context by the
 * bean name.
 * If you want to change the default parameter name, then you can achieve that 
 * by configuring the parameter name in the web.xml.
 * For example, if you want to change the default parameter name to 'my-bean-param', then
 * you can configure this like the following:
 * 
 * <xmp>
 *  <servlet>
 *    <servlet-name>HstContainerServlet</servlet-name>
 *    <servlet-class>org.hippoecm.hst.container.HstContainerServlet</servlet-class>
 *    <!--
 *    ...
 *    -->
 *    <init-param>
 *      <param-name>spring-delegated-bean-param-name</param-name>
 *      <param-value>my-bean-param</param-value>
 *    </init-param>
 *    <!--
 *    ...
 *    -->
 *    <load-on-startup>2</load-on-startup>
 *  </servlet>
 * </xmp>
 * 
 * With the above setting, you need to set the parameters with name, 'my-bean-param' in the
 * component configurations in the repository.
 * </p>
 * <p>
 * If the root web application context has hierarchical child bean factories and one of the
 * child bean factories has defined a bean you need, then you can set the bean name component
 * configuration parameter with context path prefix like the following example:
 * <br/>
 * <CODE>com.mycompany.myapp::contactBean</CODE>
 * <br/>
 * In the above example, 'com.mycompany.myapp' is the name of the child bean factory name,
 * and 'contactBean' is the bean name of the child bean factory.
 * The bean factory paths can be multiple to represent the hierarchy
 * like 'com.mycompany.myapp::crm::contactBean'.
 * <br/>
 * The separator for hierarchical bean factory path can be changed by setting the servlet
 * init parameter, 'spring-context-name-separator-param-name'.
 * </p>
 * 
 * @version $Id$
 */
public class SpringBridgeHstComponent extends GenericHstComponent implements ApplicationListener {
    
    protected String delegatedBeanNameParamName = "spring-delegated-bean";
    protected String contextNameSeparator = "::";
    
    protected AbstractApplicationContext delegatedBeanApplicationContext;
    protected HstComponent delegatedBean;
    
    @Override
    public void init(ServletConfig servletConfig, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletConfig, componentConfig);
        
        String param = servletConfig.getInitParameter("spring-delegated-bean-param-name");
        
        if (param != null) {
            delegatedBeanNameParamName = param;
        }
        
        param = servletConfig.getInitParameter("spring-context-name-separator-param-name");
        
        if (param != null) {
            contextNameSeparator = param;
        }
    }

    @Override
    public void destroy() throws HstComponentException {
        this.delegatedBeanApplicationContext = null;
        
        if (delegatedBean != null) {
            delegatedBean.destroy();
            delegatedBean = null;
        }

        super.destroy();
    }
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doAction(request, response);
    }

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doBeforeRender(request, response);
    }

    @Override
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doBeforeServeResource(request, response);
    }
    
    protected String getParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
    protected HstComponent getDelegatedBean(HstRequest request) throws HstComponentException {
        if (delegatedBean == null) {
            String beanName = getParameter(delegatedBeanNameParamName, request);
            
            if (beanName == null) {
                throw new HstComponentException("The name of delegated spring bean is null.");
            }
            
            String [] contextNames = null;
            
            if (beanName.contains(this.contextNameSeparator)) {
                String [] tempArray = beanName.split(this.contextNameSeparator);
                
                if (tempArray.length > 1) {
                    contextNames = new String[tempArray.length - 1];
                    
                    for (int i = 0; i < tempArray.length - 1; i++) {
                        contextNames[i] = tempArray[i];
                    }
                    
                    beanName = tempArray[tempArray.length - 1];
                }
            }
            
            WebApplicationContext rootWebAppContext = WebApplicationContextUtils.getWebApplicationContext(getServletConfig().getServletContext());

            if (rootWebAppContext == null) {
                throw new HstComponentException("Cannot find the root web application context.");
            }
            
            BeanFactory beanFactory = rootWebAppContext;
            
            String contextName = null;
            
            try {
                if (contextNames != null) {
                    for (int i = 0; i < contextNames.length; i++) {
                        contextName = contextNames[i];
                        beanFactory = (BeanFactory) beanFactory.getBean(contextName);
                    }
                }
            } catch (NoSuchBeanDefinitionException e) {
                throw new HstComponentException("There's no beanFactory definition with the specified name: " + contextName, e);
            } catch (BeansException e) {
                throw new HstComponentException("The beanFactory cannot be obtained: " + contextName, e);
            } catch (ClassCastException e) {
                throw new HstComponentException("The bean is not an instance of beanFactory: " + contextName, e);
            }
            
            try {
                delegatedBean = (HstComponent) beanFactory.getBean(beanName);
            } catch (NoSuchBeanDefinitionException e) {
                throw new HstComponentException("There's no bean definition with the specified name: " + beanName, e);
            } catch (BeansException e) {
                throw new HstComponentException("The bean cannot be obtained: " + beanName, e);
            }
            
            if (delegatedBean == null) {
                throw new HstComponentException("Cannot find delegated spring HstComponent bean: " + beanName);
            }

            delegatedBean.init(getServletConfig(), getComponentConfiguration());
            
            if (beanFactory instanceof AbstractApplicationContext) {
                delegatedBeanApplicationContext = (AbstractApplicationContext) beanFactory;
                
                if (!delegatedBeanApplicationContext.getApplicationListeners().contains(this)) {
                    delegatedBeanApplicationContext.addApplicationListener(this);
                }
            }
        }
        
        return delegatedBean;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextClosedEvent) {
            this.delegatedBeanApplicationContext = null;
            
            if (delegatedBean != null) {
                delegatedBean.destroy();
                delegatedBean = null;
            }
        }
    }
    
}
