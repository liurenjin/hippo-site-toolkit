<%--
  Copyright 2008 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="nextpage" value="${searchResults.next}"/>
<c:set var="offset" value="${(searchResults.currentPage -1) * searchResults.pageSize}"/>
<c:set var="prevpage" value="${searchResults.previous}"/>
<c:set var="firstpage" value="${searchResults.startPage}"/>
<c:set var="lastpage" value="${searchResults.endPage}"/>

<hst:head-contribution keyHint="title"><title>Faceted Navigation Result</title></hst:head-contribution>

<div id="yui-u">
        <h1>Results</h1>
        
        <c:if test="${subnavigation}">
            <ul>
             <c:forEach var="ancestor" items="${facetNavigation.ancestors}">
               <li>
                 <c:out value="${ancestor.facetValueCombi.key} = ${ancestor.facetValueCombi.value}" escapeXml="true"/> 
                 <hst:facetnavigationlink var="withoutAncestor" current="${facetNavigation}" remove="${ancestor}"/>
                   [<a href="${withoutAncestor}" class="deleteFacet">X</a>]
                 </li>
             </c:forEach>
               <li>
                   <c:out value="${facetNavigation.facetValueCombi.key} = ${facetNavigation.facetValueCombi.value}" escapeXml="true"/> 
                   <hst:facetnavigationlink var="withoutAncestor" current="${facetNavigation}" remove="${facetNavigation}"/>
                   [<a href="${withoutAncestor}" class="deleteFacet">X</a>]
               </li>
            </ul>
        </c:if>
        
        <c:choose>
          <c:when test="${empty facetNavigation}">
            <p>No hits found</p>
          </c:when>
          <c:otherwise>
            <br/>
            
            
            <table class="facetedTable">
            
             <tbody>
	            <c:forEach var="result" items="${resultset}">
	              <hst:link var="link" hippobean="${result}" navigationStateful="true" />
	                
	                 <tr>
	                    <td>
		                    <c:choose>
		                      <c:when test="${empty result.title}">
		                          <a href="${link}">${result.name}</a>
		                      </c:when>
		                      <c:otherwise>
		                          <a href="${link}">${result.title}</a>
		                      </c:otherwise>
		                   </c:choose>
	                    </td>
	                    <td>price : ${result.price}</td>
	                 </tr>
	            </c:forEach>
	          </tbody>
            </table>
          </c:otherwise>
        </c:choose>
</div>