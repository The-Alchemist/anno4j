package com.github.anno4j.schema_parsing.building.support;

import com.github.anno4j.annotations.Partial;
import com.github.anno4j.schema.model.rdfs.RDFSClazz;
import com.github.anno4j.schema_parsing.building.OntGenerationConfig;
import com.github.anno4j.schema_parsing.model.BuildableRDFSClazz;
import com.github.anno4j.schema_parsing.model.BuildableRDFSProperty;
import com.github.anno4j.schema_parsing.naming.MethodNameBuilder;
import com.github.anno4j.schema_parsing.validation.Validator;
import com.squareup.javapoet.*;
import org.openrdf.repository.RepositoryException;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This partial class belonging to {@link com.github.anno4j.schema_parsing.model.BuildableRDFSProperty}
 * provides functionality to generate setter method stubs to its inheriting classes.
 */
@Partial
public abstract class SetterBuildingSupport extends PropertyBuildingSupport implements BuildableRDFSProperty {

    /**
     * Returns the JavaPoet representation of the parameters type.
     * @param config The configuration to use for building type names.
     * @param allowCapture Whether the parameter type should be transformed into a capture, if applicable (e.g. {@link CharSequence}).
     * @return Returns the JavaPoet representation of the parameters target type.
     * @throws RepositoryException Thrown if an error occurs while querying the repository.
     */
    TypeName getParameterType(OntGenerationConfig config, boolean allowCapture) throws RepositoryException {
        TypeName paramType = findSingleRangeClazz().getJavaPoetClassName(config);

        // For convenience the parameter type for strings should be a wildcard, i.e. Set<? extends CharSequence>:
        if (allowCapture && paramType.equals(ClassName.get(CharSequence.class))) {
            paramType = WildcardTypeName.subtypeOf(paramType);
        }

        return paramType;
    }

    /**
     * Generates a setters signature stub without any parameter for this property.
     * This stub can be used as a basis for inheriting support classes in order to generate overloading
     * setters.
     * @param domainClazz The class for which to generate a setter method.
     * @param config The configuration on basis of which to build the method stub.
     * @return Returns the method stub without any parameter or {@code null} if there is no range specified
     * for this property.
     * @throws RepositoryException Thrown if an error occurs while querying the repository.
     */
    MethodSpec.Builder buildParameterlessSetterSignature(RDFSClazz domainClazz, OntGenerationConfig config) throws RepositoryException {
        if (getRanges() != null) {
            // Find most specific common superclass:
            BuildableRDFSClazz rangeClazz = findSingleRangeClazz();

            // JavaDoc of the method:
            CodeBlock.Builder javaDoc = CodeBlock.builder();
            CharSequence preferredComment = getPreferredRDFSComment(config);
            if (preferredComment != null) {
                javaDoc.add(preferredComment.toString());
            }
            javaDoc.add("\n@param values The elements to set.");

            // Add a throws declaration if the value space is constrained:
            addJavaDocExceptionInfo(javaDoc, rangeClazz, config);

            return MethodNameBuilder.forObjectRepository(getObjectConnection())
                    .getJavaPoetMethodSpec("set", this, config, !isSingleValueProperty(domainClazz))
                    .toBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addJavadoc(javaDoc.build());
        } else {
            return null;
        }
    }

    /**
     * Adds code to the method stub which performs validation of the first parameter of the method according to
     * the {@link Validator}s in {@code config} and afterwards sets the values.
     * Changes are propagated to all super- and subproperties of the property mapped.
     * @param stub The JavaPoet method specification to which the generated code will be added.
     * @param domainClazz The class for which the method is defined.
     * @param config The configuration for building code.
     * @param allowSingleValueParam Whether the parameter of the method can be single valued in case
     *                              the cardinality of the property is one.
     * @return Returns a method builder representing the method after the code was added.
     * @throws RepositoryException Thrown if an error occurs while querying the repository.
     */
    MethodSpec.Builder addSetterImplementationCode(MethodSpec.Builder stub, RDFSClazz domainClazz, OntGenerationConfig config, boolean allowSingleValueParam) throws RepositoryException {
        BuildableRDFSClazz range = findSingleRangeClazz();
        ClassName rangeClassName = range.getJavaPoetClassName(config);
        ParameterSpec param = stub.build().parameters.get(0);
        String paramName = param.name;
        ParameterSpec current = ParameterSpec.builder(rangeClassName, "current").build();
        boolean isVarArg = stub.build().varargs;
        boolean isParameterSingleValue = allowSingleValueParam && isSingleValueProperty(domainClazz);

        // Add validation code:
        for (Validator validator : config.getValidators()) {
            if(validator.isValueSpaceConstrained(range)) {
                if(isParameterSingleValue) {
                    validator.addValueSpaceCheck(stub, param, range);
                } else {
                    stub.beginControlFlow("for($T $N : $N)", rangeClassName, current, paramName);
                    validator.addValueSpaceCheck(stub, current, range);
                    stub.endControlFlow();
                }
            }
        }
        // If the setter is single valued and this is a vararg setter then allow max. one value:
        if(isVarArg && isSingleValueProperty(domainClazz)) {
            stub.beginControlFlow("if($N.length > 1)", param)
                    .addStatement("throw new $T($S)", ClassName.get(IllegalArgumentException.class), "Too many values.")
                    .endControlFlow();
        }

        TypeName newValuesType = ParameterizedTypeName.get(ClassName.get(Set.class), rangeClassName);
        TypeName hashSet = ParameterizedTypeName.get(ClassName.get(HashSet.class), rangeClassName);
        stub.addStatement("$T _newValues = new $T()", newValuesType, hashSet);

        if(isParameterSingleValue) {
            stub.addStatement("_newValues.add($N)", paramName);
        } else if(isVarArg) {
            stub.addStatement("_newValues.addAll($T.asList($N))", ClassName.get(Arrays.class), paramName);
        }

        // Get the properties setter-method:
        MethodSpec setter = buildSetter(domainClazz, config);

        // Set the values. Set single value if single valued setter:
        if(isSingleValueProperty(domainClazz)) {
            stub.beginControlFlow("if(!_newValues.isEmpty())")
                    .addStatement("$N(_newValues.iterator().next())", setter)
                    .endControlFlow();
        } else {
            stub.addStatement("$N(_newValues)", setter);
        }

        // Sanitize the schema using SchemaSanitizingObjectSupport:
        stub.addStatement("sanitizeSchema($S)", getResourceAsString());

        // Override annotation of the method:
        AnnotationSpec overrideAnnotation = AnnotationSpec.builder(Override.class).build();

        return stub.addAnnotation(overrideAnnotation);
    }
}
