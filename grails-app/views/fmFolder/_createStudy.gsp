
<%! import annotation.* %> 
<%! import bio.* %>  
<%! import com.recomdata.util.* %> 
  
<g:form name="createStudyForm">
<g:hiddenField name="id" value="${folder?.id}" />
<g:set var="objectUid" value="${folder?.uniqueId}"/>
<g:render template="metaData" model="[templateType: templateType, title:title, bioDataObject:bioDataObject, folder:folder, amTagTemplate: amTagTemplate, metaDataTagItems: metaDataTagItems]"/>
<br/>
<div align="center">
    <span id="savestudybutton" class="greybutton">Save</span>
    <span id="cancelstudybutton" class="greybutton buttonicon close">Cancel</span>
</div>
</g:form>
