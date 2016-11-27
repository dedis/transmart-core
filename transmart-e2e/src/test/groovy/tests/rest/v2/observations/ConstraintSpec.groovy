package tests.rest.v2.observations

import base.RESTSpec
import spock.lang.Requires

import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.hasKey
import static org.hamcrest.Matchers.is
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.AFTER
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.Operator.GREATER_THAN
import static tests.rest.v2.Operator.LESS_THAN
import static tests.rest.v2.ValueType.DATE
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.*
import static config.Config.*

class ConstraintSpec extends RESTSpec{

    /**
     * TrueConstraint.class,
     BiomarkerConstraint.class,
     ModifierConstraint.class,
     FieldConstraint.class,
     ValueConstraint.class,
     TimeConstraint.class,
     PatientSetConstraint.class,
     Negation.class,
     Combination.class,
     TemporalConstraint.class,
     ConceptConstraint.class,
     StudyConstraint.class,
     NullConstraint.class
     */
    def final INVALIDARGUMENTEXCEPTION = "InvalidArgumentsException"
    def final EMPTYCONTSTRAINT = "Empty constraint parameter."

    /**
     *  when:" I do a Get query/observations with a wrong type."
     *  then: "then I get a 400 with 'Constraint not supported: BadType.'"
     */
    def "Get /query/observations malformed query"(){
        when:" I do a Get query/observations with a wrong type."
        def constraintMap = [type: 'BadType']

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "then I get a 400 with 'Constraint not supported: BadType.'"
        that responseData.httpStatus, is(400)
        that responseData.type, is(INVALIDARGUMENTEXCEPTION)
        that responseData.message, is('Constraint not supported: BadType.')
    }

    def "TrueConstraint.class"(){
        def constraintMap = [type: TrueConstraint]

        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "BiomarkerConstraint.class"(){

    }

    def "ModifierConstraint.class"(){
        when:
        def constraintMap = [
                type: ModifierConstraint, path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
        ]
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData.size(), is(3)
        that responseData, everyItem(hasEntry('conceptCode', 'TNS:LAB:CELLCNT'))

        when:
        constraintMap = [
                type: ModifierConstraint, modifierCode: "TNS:SMPL",
                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
        ]
        responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData.size(), is(3)
        that responseData, everyItem(hasEntry('conceptCode', 'TNS:LAB:CELLCNT'))
    }

    def "FieldConstraint.class"(){
        def constraintMap = [type: FieldConstraint,
                             field: [dimension: 'PatientDimension',
                                     fieldName: 'age',
                                     type: NUMERIC ],
                             operator: LESS_THAN,
                             value:100]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "ValueConstraint.class"(){
        def constraintMap = [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value:176]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "TimeConstraint.class"(){
        def date = toDateString("01-01-2016Z")
        def constraintMap = [type: TimeConstraint,
                             field: [dimension: 'StartTimeDimension', fieldName: 'startDate', type: DATE ],
                             operator: AFTER,
                             values: [date]]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "PatientSetConstraint.class"(){
        def setID = post(PATH_PATIENT_SET, contentTypeForJSON, null, toJSON([type: PatientSetConstraint, patientIds: -62]))
        def constraintMap = [type: PatientSetConstraint, patientSetId: setID.id]

        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))

        when:
        constraintMap = [type: PatientSetConstraint, patientIds: -62]
        responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))
        
        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "Negation.class"(){
        def constraintMap = [
                type: Negation,
                arg: [type: PatientSetConstraint, patientIds: -62]
        ]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "Combination.class"(){
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: PatientSetConstraint, patientIds: -62],
                        [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                ]
        ]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "TemporalConstraint.class"(){
        def constraintMap = [
                type: TemporalConstraint,
                operator: AFTER,
                eventConstraint: [
                         type: ValueConstraint,
                         valueType: NUMERIC,
                         operator: LESS_THAN,
                         value: 60
                ]
        ]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "ConceptConstraint.class"(){
        def constraintMap = [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "StudyConstraint.class"(){
        def constraintMap = [type: StudyConstraint, studyId: EHR_ID]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "NullConstraint.class"(){
        def constraintMap = [
                type: NullConstraint,
                field: [dimension: 'EndTimeDimension', fieldName: 'endDate', type: DATE ]
        ]

        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, everyItem(hasEntry('endDate', null))
    }

}