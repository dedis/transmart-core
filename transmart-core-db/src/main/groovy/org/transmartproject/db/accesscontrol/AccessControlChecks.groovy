/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.accesscontrol

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Subqueries
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.*
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.ontology.AbstractI2b2Metadata
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.util.StringUtils

import java.util.stream.Collectors

import static org.transmartproject.db.ontology.AbstractAcrossTrialsOntologyTerm.ACROSS_TRIALS_TABLE_CODE

/**
 * Access control checks.
 *
 * It implements checks for 17.1 data model and legacy one.
 */
@Slf4j
@Component
@CompileStatic
class AccessControlChecks implements AuthorisationChecks, LegacyAuthorisationChecks {

    public static final String PUBLIC_SOT = 'EXP:PUBLIC'
    public static final Set<String> PUBLIC_TOKENS = [org.transmartproject.db.i2b2data.Study.PUBLIC, PUBLIC_SOT] as Set

    @Autowired
    OntologyTermsResource conceptsResource

    @Autowired
    StudiesResource studiesResource

    @Autowired
    SessionFactory sessionFactory


    /**
     * Checks if a {@link org.transmartproject.db.i2b2data.Study} (in the i2b2demodata schema)
     * exists to which the user has access.
     * Access is checked based on the {@link org.transmartproject.db.i2b2data.Study#secureObjectToken}
     * field.
     * This is the <code>/v2</code> way of checking study based access.
     *
     * @param user the user to check access for.
     * @param minAccessLevel minimal access level user has to have on the study
     * @param study the study object that is referred to from the trial visit dimension.
     * @return true iff a study exists that the user has access to.
     */
    @Override
    boolean canReadPatientData(User user, PatientDataAccessLevel patientDataAccessLevel, MDStudy study) {
        assert study instanceof org.transmartproject.db.i2b2data.Study
        hasAtLeastAccessLevel(user, patientDataAccessLevel, study.secureObjectToken)
    }

    @Override
    boolean hasAnyAccess(User user, MDStudy study) {
        canReadPatientData(user, PatientDataAccessLevel.minimalAccessLevel, study)
    }

    @Override
    boolean hasAccess(User user, QueryResult result) {

        def res = result.username == user.username || user.admin

        if (!res) {
            log.warn "Denying $user access to query result $result because " +
                    "its creator (${result.username}) doesn't match the user " +
                    "(${user.username}) and requesting user is not an admin"
        } else {
            log.debug "Granting ${user.username} access to $result"
        }

        res
    }

    @Override
    boolean hasAccess(User user, OntologyTerm term) {
        hasAtLeastAccessLevelForTheSecureNode(user, PatientDataAccessLevel.minimalAccessLevel, term)
    }

    @Override
    boolean canRun(User user, QueryDefinition definition) {
        if (user.admin) {
            log.debug "Bypassing check on query definition for user ${user.username}  because she is an administrator"
            return true
        }

        // check there is at least one non-inverted panel for which the user
        // has permission in all the terms
        def res = definition.panels.findAll { !it.invert }.any { Panel panel ->
            Set<Study> foundStudies = panel.items.findAll { Item item ->
                /* always permit across trial nodes.
                 * Across trial terms have no study, so they have to be
                 * handled especially */
                new ConceptKey(item.conceptKey).tableCode !=
                        ACROSS_TRIALS_TABLE_CODE
            }.collect { Item item ->
                /* this could be optimized by adding a new method in
                 * StudiesResource */
                OntologyTerm concept = conceptsResource.getByKey(item.conceptKey)
                if (concept instanceof AbstractI2b2Metadata) {
                    def dimensionCode = ((AbstractI2b2Metadata) concept).dimensionCode
                    if (dimensionCode != concept.fullName) {
                        log.warn "Shared concepts not supported. Term: ${concept.fullName}, concept: ${dimensionCode}"
                        throw new AccessDeniedException("Shared concepts cannot be used for cohort selection.")
                    }
                } else {
                    throw new AccessDeniedException("Node type not supported: ${concept?.class?.simpleName}.")
                }

                def study = concept.study

                if (study == null) {
                    log.info "User included concept with no study: ${concept.fullName}"
                }

                study
            } as Set

            foundStudies.every { Study study1 ->
                if (study1 == null) {
                    return false
                }
                hasAccess(user, study1)
            }
        }

        if (!res) {
            log.warn "User ${user.username} defined access for definition ${definition.name} " +
                    "because it doesn't include one non-inverted panel for" +
                    "which the user has permission in all the terms' studies"
        } else {
            log.debug "Granting access to user ${user.username} to use " +
                    "query definition ${definition.name}"
        }

        res
    }

    /**
     * Checks if a {@link I2b2Secure} node, representing the study, exists to which the user has access.
     * Access is granted if such a node does not exist.
     * If the node exists, access is checked based on the {@link I2b2Secure#secureObjectToken}
     * field.
     * Warning: this is the <code>/v1</code> way of checking study based access, do not use for
     * <code>/v2</code> code!
     *
     * @param user the user to check access for.
     * @param minAccessLevel minimal access level user has to have on the study
     * @param study the core API study object representing the study.
     * @return true iff
     * - a study node does not exist in I2b2Secure
     * - or a study node exists in I2b2Secure that the user has access to.
     */
    @Override
    boolean canReadPatientData(User user, PatientDataAccessLevel patientDataAccessLevel, Study study) {
        hasAtLeastAccessLevelForTheSecureNode(user, patientDataAccessLevel, study.ontologyTerm)
    }

    @Override
    boolean hasAccess(User user, Study study) {
        canReadPatientData(user, PatientDataAccessLevel.minimalAccessLevel, study)
    }

    Session getSession() {
        sessionFactory.currentSession
    }

    /* Study is included if the user has ANY kind of access */

    Set<Study> getLegacyStudiesForUser(User user) {
        /* this method could benefit from caching */
        def studySet = studiesResource.studySet
        studySet.findAll {
            OntologyTerm ontologyTerm = it.ontologyTerm
            assert ontologyTerm: "No ontology node found for study ${it.id}."
            I2b2Secure i2b2Secure = getSecureNodeIfExists(ontologyTerm)
            if (!i2b2Secure) {
                log.debug("No secure record for ${ontologyTerm.fullName} path found. Study treated as public.")
                return true
            }
            hasAccess(user, i2b2Secure)
        }
    }

    @Transactional(readOnly = true)
    Collection<MDStudy> getStudiesForUser(User user, PatientDataAccessLevel level) {
        if (user.admin) {
            return org.transmartproject.db.i2b2data.Study.findAll() as List<MDStudy>
        }

        def accessibleStudyTokens = getAccessibleStudyTokensForUser(user, level)
        def criteria = DetachedCriteria.forClass(org.transmartproject.db.i2b2data.Study)
        criteria.add(Restrictions.in('secureObjectToken', accessibleStudyTokens))
        criteria.getExecutableCriteria(sessionFactory.currentSession).list() as List<MDStudy>
    }

    /**
     * Retrieves study ids for the studies to which the user has at least the required access level.
     * @param user
     * @param requiredAccessLevel
     * @return the set of study ids.
     */
    private static Set<String> getAccessibleStudyTokensForUser(User user, PatientDataAccessLevel requiredAccessLevel) {
        user.studyToPatientDataAccessLevel.entrySet().stream()
                .filter({ Map.Entry<String, PatientDataAccessLevel> entry ->
                    entry.value >= requiredAccessLevel
                })
                .map({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.key })
                .collect(Collectors.toSet()) + PUBLIC_TOKENS
    }

    private boolean exists(DetachedCriteria criteria) {
        (criteria.getExecutableCriteria(sessionFactory.currentSession).setMaxResults(1).uniqueResult() != null)
    }

    boolean canAccessConceptByCode(User user, PatientDataAccessLevel requiredAccessLevel, String conceptCode) {
        if (conceptCode == null || conceptCode.empty) {
            throw new AccessDeniedException('No concept code provided.')
        }

        if (user.admin) {
            return true
        }

        Set<String> tokens = getAccessibleStudyTokensForUser(user, requiredAccessLevel)

        def conceptCriteria = DetachedCriteria.forClass(ConceptDimension)
        conceptCriteria = conceptCriteria.add(StringUtils.like('conceptCode', conceptCode, MatchMode.EXACT))
        conceptCriteria = conceptCriteria.setProjection(Projections.property('conceptPath'))

        def criteria = DetachedCriteria.forClass(I2b2Secure)
                .add(Restrictions.in('secureObjectToken', tokens))
                .add(Restrictions.ilike('dimensionTableName', 'concept_dimension'))
                .add(Restrictions.ilike('columnName', 'concept_path'))
                .add(Restrictions.ilike('operator', 'like'))
                .add(Subqueries.propertyIn('dimensionCode', conceptCriteria))
        exists(criteria)
    }

    boolean canAccessConceptByPath(User user, PatientDataAccessLevel requiredAccessLevel, String conceptPath) {
        if (conceptPath == null || conceptPath.empty) {
            throw new AccessDeniedException('No concept path provided.')
        }

        if (user.admin) {
            return true
        }

        Set<String> tokens = getAccessibleStudyTokensForUser(user, requiredAccessLevel)

        def conceptCriteria = DetachedCriteria.forClass(ConceptDimension)
        conceptCriteria = conceptCriteria.add(StringUtils.like('conceptPath', conceptPath, MatchMode.EXACT))
        conceptCriteria = conceptCriteria.setProjection(Projections.property('conceptPath'))

        def criteria = DetachedCriteria.forClass(I2b2Secure)
                .add(Restrictions.in('secureObjectToken', tokens))
                .add(Restrictions.ilike('dimensionTableName', 'concept_dimension'))
                .add(Restrictions.ilike('columnName', 'concept_path'))
                .add(Restrictions.ilike('operator', 'like'))
                .add(Subqueries.propertyIn('dimensionCode', conceptCriteria))
        exists(criteria)
    }


    private
    static boolean hasAtLeastAccessLevelForTheSecureNode(User user, PatientDataAccessLevel minAccessLevel, OntologyTerm term) {
        I2b2Secure secureNode = getSecureNodeIfExists(term)
        if (!secureNode) {
            log.warn "Could not find object '${term.fullName}' in i2b2_secure; allowing access"
            return true
        }
        return hasAtLeastAccessLevel(user, minAccessLevel, secureNode.secureObjectToken)
    }

    /**
     * Checks whether user has right to perform given opertion on the resource with the given token
     * @param user
     * @param minAccessLevel minimal access level
     * @param token
     * @return
     */
    private static boolean hasAtLeastAccessLevel(User user, PatientDataAccessLevel minAccessLevel, String token) {
        if (!token) {
            throw new UnexpectedResultException('Token is null.')
        }

        if (user.admin) {
            log.debug "Bypassing check for $minAccessLevel on ${token} for user ${user.username}" +
                    ' because she is an administrator'
            return true
        }

        if (token in PUBLIC_TOKENS) {
            return true
        }

        minAccessLevel <= user.studyToPatientDataAccessLevel[token]
    }

    private static I2b2Secure getSecureNodeIfExists(OntologyTerm term) {
        if (term instanceof I2b2Secure) {
            return (I2b2Secure) term
        }
        Collection<I2b2Secure> secureNodes = (Collection<I2b2Secure>) I2b2Secure.createCriteria()
                .list { eq('fullName', term.fullName) }
        if (secureNodes) {
            return secureNodes.iterator().next()
        }
    }

}