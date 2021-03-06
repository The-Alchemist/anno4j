package com.github.anno4j.schema.model.rdfs;

import com.github.anno4j.model.namespaces.RDFS;
import org.openrdf.annotations.Iri;

import java.util.Set;

/**
 * Refers to https://www.w3.org/TR/rdf-schema/#ch_class
 * This is the class of resources that are RDF classes. rdfs:Class is an instance of rdfs:Class.
 */
@Iri(RDFS.CLAZZ)
public interface RDFSClazz extends RDFSSchemaResource {

    /**
     * Refers to https://www.w3.org/TR/rdf-schema/#ch_subclassof
     * The property rdfs:subClassOf is an instance of rdf:Property that is used to state that all the instances of one class are instances of another.
     */
    @Iri(RDFS.SUB_CLASS_OF)
    void setSuperclazzes(Set<RDFSClazz> subClazzes);

    /**
     * Refers to https://www.w3.org/TR/rdf-schema/#ch_subclassof
     * The property rdfs:subClassOf is an instance of rdf:Property that is used to state that all the instances of one class are instances of another.
     */
    @Iri(RDFS.SUB_CLASS_OF)
    Set<RDFSClazz> getSuperclazzes();

    /**
     * Refers to https://www.w3.org/TR/rdf-schema/#ch_subclassof
     * The property rdfs:subClassOf is an instance of rdf:Property that is used to state that all the instances of one class are instances of another.
     */
    void addSuperclazz(RDFSClazz subClazz);
}
