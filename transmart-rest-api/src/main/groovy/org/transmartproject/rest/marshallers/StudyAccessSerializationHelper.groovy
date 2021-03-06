/*
 * Copyright 2016 The Hyve B.V.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.ontology.StudyAccess
import org.transmartproject.db.ontology.StudyAccessImpl

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class StudyAccessSerializationHelper extends AbstractHalOrJsonSerializationHelper<StudyAccessImpl> {

    final Class targetType = StudyAccess
    final String collectionName = 'studiesAccess'
    private static final String VERSION = "v1"


    @Override
    Collection<Link> getLinks(StudyAccessImpl studyAccessImpl) {
        [new Link(RELATIONSHIP_SELF, "/$VERSION/studies/" +
                studyAccessImpl.study.id.toLowerCase(Locale.ENGLISH).encodeAsURL())]
    }

    @Override
    Map<String, Object> convertToMap(StudyAccessImpl studyAccessImpl) {
        def term = new OntologyTermWrapper(studyAccessImpl.study.ontologyTerm, true)
        def mapResponse = [id: studyAccessImpl.study.id, ontologyTerm: term, accessibleByUser: studyAccessImpl.accessibleByUser]
        mapResponse
    }

    @Override
    Set<String> getEmbeddedEntities(StudyAccessImpl object) {
        ['ontologyTerm'] as Set
    }
}
