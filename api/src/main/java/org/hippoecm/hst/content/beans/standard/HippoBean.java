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
package org.hippoecm.hst.content.beans.standard;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.manager.ObjectConverterAware;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;

public interface HippoBean extends NodeAware, ObjectConverterAware, Comparable<HippoBean> {

    Node getNode();
    
    JCRValueProvider getValueProvider();

    String getName();

    String getPath();

    Map<String, Object> getProperties();

    <T> T getProperty(String name);

    /**
     * Return a (pseudo)-map to use in expression language like jsp
     * @return Map of all properties
     */
    Map<String, Object> getProperty();
    
    /**
     * @param relPath
     * @return return the HippoBean with relative path wrt to this bean
     */
    <T> T getBean(String relPath);
    
    /**
     * Returns the parent bean wrt this bean. Note that this does not automatically imply
     * a bean with the parent jcr node of this bean. When the parent node is of type "hippo:handle",
     * the parent of the handle must be taken
     * @return the parent bean wrt this bean. 
     */
    HippoBean getParentBean();
    
    /**
     * A convenience method capable of comparing two HippoBean instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) this method returns true.
     * @param compare the object to compare to
     * @return <code>true</code> if the object compared has the same canonical node
     */
    
    /**
     * @return List<HippoBean> where the backing jcr nodes are of type jcrPrimaryNodeType
     */
    <T> List<T> getChildBeans(String jcrPrimaryNodeType);
    
    boolean equalCompare(Object compare);
    
    /**
     * A convenience method capable of comparing two HippoBean instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) the get(Object o) returns true.
     * In expression language, for example jsp, you can use to compare as follows:
     * 
     * <code>${mydocument.equalComparator[otherdocument]}</code>
     * 
     * this only returns true when mydocument and otherdocument have the same canonical node
     * 
     * @return a ComparatorMap in which you can compare HippoBean's via the get(Object o)
     */
    Map<Object,Object> getEqualComparator();

}
