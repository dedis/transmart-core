/*************************************************************************
 * tranSMART - translational medicine data mart
 * 
 * Copyright 2008-2012 Janssen Research & Development, LLC.
 * 
 * This product includes software developed at Janssen Research & Development, LLC.
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software  * Foundation, either version 3 of the License, or (at your option) any later version, along with the following terms:
 * 1.	You may convey a work based on this program in accordance with section 5, provided that you retain the above notices.
 * 2.	You may convey verbatim copies of this program code as you receive it, in any medium, provided that you retain the above notices.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS    * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *
 ******************************************************************/
  

package fm


import groovy.xml.StreamingMarkupBuilder
import java.util.ArrayList;
import java.util.List;
import annotation.AmTagTemplate;

class FmFolder implements Buildable{
	
	Long id
	String folderName
	String folderFullName
	Long folderLevel
	String folderType
	String folderTag
	String description
	Boolean activeInd = Boolean.TRUE
	
	
	static mapping = {
		table 'fm_folder'
		version false
		cache true
		sort "folderName"
		id generator: 'sequence', params:[sequence:'seq_fm_id']
		fmFiles joinTable: [name: 'fm_folder_file_association',  key:'folder_id', column: 'file_id'], lazy: false
		columns { id column:'folder_id' }
		uniqueIds joinTable:[name:'FM_DATA_UID', key:'FM_DATA_ID']
		
	
	}
	
	static belongsTo = [parent: FmFolder]	
	static hasMany = [fmFiles: FmFile, children: FmFolder, uniqueIds: FmData ] //, amTagTemplates: AmTagTemplate]
	
	def getFmData()
	{
		if(uniqueIds!=null && !uniqueIds.isEmpty())
			return (uniqueIds.iterator().next());
		return null;

	}

		def getUniqueId(){
		if(uniqueIds!=null && !uniqueIds.isEmpty())
			return (uniqueIds.iterator().next()).uniqueId;
		return null;
	}

	
	static constraints = {
		folderName(maxSize:1000)
		folderFullName(maxSize:1000)
		folderType(maxSize:100)
		folderTag(nullable: true, maxSize:50)
		description(nullable: true, maxSize: 2000)
		parent(nullable: true)
	}
	
	def void build(GroovyObject builder)
	{
        def fmFolder = {
             folderDefinition(id:this.id){
				 folderName(this.folderName)
				 folderFullName(this.folderFullName)
				 folderLevel(this.folderLevel)
				 folderType(this.folderType)
				 
				 List<FmFolder> subFolderList = FmFolder.findAll("from FmFolder as fd where fd.folderFullName like :fn and fd.folderLevel = :fl",
				 [fn:this.folderFullName+"%", fl: (this.folderLevel + 1)])

				 unescaped << '<fmFolders>'
				 subFolderList.each {
					 
					 	println it
						 out << it
				 	}
				 unescaped << '</fmFolders>'
/*		                 addresses {
		                     this.addresses.each{address ->
		                         out << address
		                     }
		                     *
		                 }
		                 */
             }
         }
		
         fmFolder.delegate = builder
         fmFolder()

	}
	
	/**
	* override display
	*/
   public String toString() {
	   StringBuffer sb = new StringBuffer();
	   sb.append("ID: ").append(this.id).append(", Folder Name: ").append(this.folderName);
	   sb.append(", Folder Full Name: ").append(this.folderFullName).append(", Folder Level: ").append(this.folderLevel);
	   sb.append(", Folder Type: ").append(this.folderType).append(", Object UID ").append(", Description ").append(this.description);
	   return sb.toString();
   }

}
