package com.github.anno4j.querying.tests;

import com.github.anno4j.model.Annotation;
import com.github.anno4j.model.Body;
import com.github.anno4j.querying.Comparison;
import com.github.anno4j.querying.QuerySetup;
import org.apache.marmotta.ldpath.parser.ParseException;
import org.junit.Test;
import org.openrdf.annotations.Iri;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Containing all tests, that do query for specific data types.
 */
public class DataTypeTest extends QuerySetup {

    @Test
    /**
     * Querying for all annotations, that has bodies with xsd:double values set, using eq comparison.
     */
    public void doubleEqualTest() throws RepositoryException, QueryEvaluationException, MalformedQueryException, ParseException {
        List<Annotation> list = queryService
                .addPrefix("ex", "http://www.example.com/schema#")
                .addCriteria("oa:hasBody/ex:doubleValue[^^xsd:double]")
                .execute();

        assertEquals(1, list.size());
        assertEquals(new Double(2.0), ((DataTypeBody) list.get(0).getBodies().iterator().next()).getDoubleValue());
    }

    @Test
    /**
     *  Querying for all annotations, that has bodies with a xsd:double values < 4.0 set.
     */
    public void doubleLtTest() throws RepositoryException, QueryEvaluationException, MalformedQueryException, ParseException {
        List<Annotation> list = queryService
                .addPrefix("ex", "http://www.example.com/schema#")
                .addCriteria("oa:hasBody/ex:doubleValue[^^xsd:double]", 4.0, Comparison.LT)
                .execute();

        assertEquals(1, list.size());
        assertEquals(new Double(2.0), ((DataTypeBody) list.get(0).getBodies().iterator().next()).getDoubleValue());
    }

    @Test
    /**
     *  Querying for all annotations, that has bodies with xsd:double values > 4.0 set. This should not return any result
     *  because no such object was persisted before.
     */
    public void falseGtTest() throws RepositoryException, QueryEvaluationException, MalformedQueryException, ParseException {
        List<Annotation> list = queryService
                .addPrefix("ex", "http://www.example.com/schema#")
                .addCriteria("oa:hasBody/ex:doubleValue[^^xsd:double]", 4.0, Comparison.GT)
                .execute();

        assertEquals(0, list.size());
    }

    @Test
    /**
     *  Querying for all annotations, that has bodies with xsd:string values set.
     */
    public void stringTest() throws RepositoryException, QueryEvaluationException, MalformedQueryException, ParseException {

        List<Annotation> list = queryService
                .addPrefix("ex", "http://www.example.com/schema#")
                .addCriteria("oa:hasBody/ex:stringValue[^^xsd:string]")
                .execute();

        assertEquals(1, list.size());
        assertEquals("3.0", ((DataTypeBody) list.get(0).getBodies().iterator().next()).getStringValue());
        assertEquals(null, ((DataTypeBody) list.get(0).getBodies().iterator().next()).getDoubleValue());
    }

    @Override
    public void persistTestData() throws RepositoryException, InstantiationException, IllegalAccessException {
        // Persisting some data
        Annotation annotation = anno4j.createObject(Annotation.class);
        DataTypeBody dataTypeBody = anno4j.createObject(DataTypeBody.class);
        dataTypeBody.setDoubleValue(2.0);
        annotation.addBody(dataTypeBody);

        Annotation annotation1 = anno4j.createObject(Annotation.class);
        DataTypeBody dataTypeBody2 = anno4j.createObject(DataTypeBody.class);
        dataTypeBody2.setStringValue("3.0");
        annotation1.addBody(dataTypeBody2);
    }

    @Iri("http://www.example.com/schema#datatTypeBody")
    public static interface DataTypeBody extends Body {
        @Iri("http://www.example.com/schema#doubleValue")
        Double getDoubleValue();

        @Iri("http://www.example.com/schema#doubleValue")
        void setDoubleValue(Double doubleValue);

        @Iri("http://www.example.com/schema#stringValue")
        String getStringValue();

        @Iri("http://www.example.com/schema#stringValue")
        void setStringValue(String stringValue);
    }

}
