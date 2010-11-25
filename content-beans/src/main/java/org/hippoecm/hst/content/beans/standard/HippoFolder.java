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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippostd:folder")
public class HippoFolder extends HippoItem implements HippoFolderBean {
    private static Logger log = LoggerFactory.getLogger(HippoFolder.class);
    protected List<HippoFolderBean> hippoFolders;
    protected List<HippoDocumentBean> hippoDocuments;
    
    public List<HippoFolderBean> getFolders(){
         return this.getFolders(false);
    }
    
    public List<HippoFolderBean> getFolders(boolean sorted){
        if(this.hippoFolders != null) {
            if(sorted) {
                List<HippoFolderBean> dest = new ArrayList<HippoFolderBean>(this.hippoFolders.size());
                Collections.copy(dest, this.hippoFolders);
                Collections.sort(dest);
                return dest;
            }
            return this.hippoFolders;
        }
        if(this.node == null) {
            log.warn("Cannot get documents because node is null");
            return new ArrayList<HippoFolderBean>();
        }
        try {
            this.hippoFolders = new ArrayList<HippoFolderBean>();
            NodeIterator nodes = this.node.getNodes();
            while(nodes.hasNext()) {
                javax.jcr.Node child = nodes.nextNode();
                if(child == null) {continue;}
                HippoFolderBean hippoFolder = getHippoFolder(child);
                if(hippoFolder != null) {
                    this.hippoFolders.add(hippoFolder);
                }
            }
            if(sorted) {
                if(sorted) {
                    List<HippoFolderBean> dest = new ArrayList<HippoFolderBean>(this.hippoFolders.size());
                    Collections.copy(dest, this.hippoFolders);
                    Collections.sort(dest);
                    return dest;
                }
            }
            return this.hippoFolders;
        } catch (RepositoryException e) {
            log.warn("Repository Exception : {}", e);
            return new ArrayList<HippoFolderBean>();
        }
    }
    
    public int getDocumentSize(){
        return getDocuments().size();
    }
    
    public List<HippoDocumentBean> getDocuments() {
        return getDocuments(false);
    }
    
    public List<HippoDocumentBean> getDocuments(int from, int to) {
        return getDocuments(from,to,false);
    }
    
    public List<HippoDocumentBean> getDocuments(int from, int to, boolean sorted) {
        List<HippoDocumentBean> documents = getDocuments(sorted);
        try {
            return documents.subList(from, to); 
        } catch (IndexOutOfBoundsException e) {
            log.warn("Invalid sublist for getDocuments '{}'. Return empty list.", e.getMessage());
            return new ArrayList<HippoDocumentBean>();
        }
    }
    
    public <T> List<T> getDocuments(Class<T> beanMappingClass) {
        List<HippoDocumentBean> documents = getDocuments();
        List<T> documentOfClass = new ArrayList<T>();
        for(HippoDocumentBean bean : documents) {
            if(beanMappingClass.isAssignableFrom(bean.getClass())) {
                documentOfClass.add((T)bean);
            }
        }
        return documentOfClass;
    }
    
    public List<HippoDocumentBean> getDocuments(boolean sorted) {
        if(this.hippoDocuments != null) {
            if(sorted) {
                List<HippoDocumentBean> dest = new ArrayList<HippoDocumentBean>(this.hippoDocuments.size());
                Collections.copy(dest, this.hippoDocuments);
                Collections.sort(dest);
                return dest;
            }
            return this.hippoDocuments;
        }
        if(this.node == null) {
            log.warn("Cannot get documents because node is null");
            return new ArrayList<HippoDocumentBean>();
        }
        try {
            this.hippoDocuments = new ArrayList<HippoDocumentBean>();
            NodeIterator nodes = this.node.getNodes();
            while(nodes.hasNext()) {
                javax.jcr.Node child = nodes.nextNode();
                if(child == null) {continue;}
                HippoDocumentBean hippoDocument = getHippoDocument(child);
                if(hippoDocument != null) {
                    this.hippoDocuments.add(hippoDocument);
                }
            }
            if(sorted) {
                // do not sort the actual list, but first copy the list:
                List<HippoDocumentBean> dest = new ArrayList<HippoDocumentBean>(this.hippoDocuments.size());
                Collections.copy(dest, this.hippoDocuments);
                Collections.sort(dest);
                return dest;
            }
            return this.hippoDocuments;
        } catch (RepositoryException e) {
            log.warn("Repository Exception : {}", e);
            return new ArrayList<HippoDocumentBean>();
        }
    }
    
    
    
    private HippoFolderBean getHippoFolder(javax.jcr.Node child) {
        try {
            Object o  = objectConverter.getObject(child);
            if(o instanceof HippoFolderBean) {
                return (HippoFolderBean)o;
            } 
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot return HippoFolder. Return null : {} " , e);
        }
        return null;
    }
    
   
   private <T> T getHippoDocument(javax.jcr.Node node, Class<T> beanMappingClass) {
       HippoDocumentBean bean = getHippoDocument(node);
       if(bean == null) {
           return null;
       }
       if(beanMappingClass.isAssignableFrom(bean.getClass())) {
           return ((T)bean);
       }
       return null;
    }
    
    private HippoDocumentBean getHippoDocument(javax.jcr.Node node) {
        try {
            if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
                if(node.hasNode(node.getName())) {
                    Object o  = objectConverter.getObject(node.getNode(node.getName()));
                    if(o instanceof HippoDocumentBean) {
                        return (HippoDocumentBean)o;
                    } else {
                        log.warn("Cannot return HippoDocument for. Return null '{}'", node.getPath());
                    }
                } 
                return null;
            } else if(node.getParent().isNodeType(HippoNodeType.NT_HANDLE) || node.getParent().isNodeType(HippoNodeType.NT_FACETRESULT)) {
                Object o  = (HippoDocument)objectConverter.getObject(node);
                if(o instanceof HippoDocument) {
                    return (HippoDocumentBean)o;
                } else {
                    log.warn("Cannot return HippoDocument for. Return null '{}'", node.getPath());
                }
            }
        } catch (RepositoryException e) {
            log.error("Cannot return HippoDocument. Return null : {} " , e);
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot return HippoDocument. Return null : {} " , e);
        }
        return null;
    }

    public <T> HippoDocumentIterator<T> getDocumentIterator(Class<T> beanMappingClass){
        return new HippoDocumentIteratorImpl<T>(beanMappingClass);
    }
    
    private class HippoDocumentIteratorImpl<T> implements HippoDocumentIterator<T> {

        NodeIterator nodeIterator;
        Class<T> beanMappingClass;
        T nextHippoDocument;
        int position = -1;
        
        public HippoDocumentIteratorImpl() {
            this(null);
         }
        
        public HippoDocumentIteratorImpl(Class<T> beanMappingClass) {
            if(beanMappingClass == null) {
                throw new IllegalArgumentException("beanMappingClass not allowed to be null");
            }
            this.beanMappingClass = beanMappingClass;
            if(HippoFolder.this.node == null) {
                log.warn("Cannot get documents because node is null");
            }
            try {
                nodeIterator = node.getNodes();
            } catch (RepositoryException e) {
                log.warn("Repository exception happened. Return empty iterator");
            }
        }

        public void skip(int skipNum) {
            if(nodeIterator == null) {
                return;
            }
            if(skipNum < 0) {
                throw new IllegalArgumentException("SkipNum is not allowed to be negative");
            }
            while(skipNum > 0  && nodeIterator.hasNext()) {
                javax.jcr.Node child = nodeIterator.nextNode();
                if(child == null) {
                    continue;
                }
                T hippoDocument = getHippoDocument(child, beanMappingClass);
                if(hippoDocument != null) {
                    position++;
                    skipNum--;
                }
            }
            if(skipNum > 0) {
                log.debug("Skipped beyond last hippo document. Iterator won't return any hippo documents anymore");
            }
        }

        public boolean hasNext() {
            if(nodeIterator == null) {
                return false;
            }
            if(this.nextHippoDocument != null) {
                return true;
            }
            while(nodeIterator.hasNext()) {
                javax.jcr.Node child = nodeIterator.nextNode();
                if(child == null) {
                    continue;
                }
                T hippoDocument = getHippoDocument(child, beanMappingClass);
                if(hippoDocument != null) {
                    this.nextHippoDocument = hippoDocument; 
                    return true;
                }
            } 
            return false;
        }

        public T next() {
            if(nodeIterator == null) {
                throw new NoSuchElementException("NodeIterator is null. No next element");
            }
            if(nextHippoDocument != null) {
                T next = nextHippoDocument;
                nextHippoDocument = null;
                position++;
                return next;
            } else {
                while(nodeIterator.hasNext()) {
                    javax.jcr.Node child = nodeIterator.nextNode();
                    if(child == null) {
                        continue;
                    }
                    T hippoDocument = getHippoDocument(child, beanMappingClass);
                    if(hippoDocument != null) {
                        position++;
                        return hippoDocument;
                    }
                } 
            }
            throw new NoSuchElementException("No next HippoDocumentBean");
        }

        public void remove() {
            throw new NoSuchElementException("Remove is not supported");
        }

        public int getPosition() {
            return this.position == -1? 0 : this.position;
        }

    }
}
