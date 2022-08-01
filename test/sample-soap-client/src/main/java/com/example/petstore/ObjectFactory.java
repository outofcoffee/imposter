
package com.example.petstore;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.example.petstore package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetPetByIdResponse_QNAME = new QName("urn:com:example:petstore", "getPetByIdResponse");
    private final static QName _GetPetByNameRequest_QNAME = new QName("urn:com:example:petstore", "getPetByNameRequest");
    private final static QName _GetPetByIdRequest_QNAME = new QName("urn:com:example:petstore", "getPetByIdRequest");
    private final static QName _Fault_QNAME = new QName("urn:com:example:petstore", "fault");
    private final static QName _GetPetByNameResponse_QNAME = new QName("urn:com:example:petstore", "getPetByNameResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.example.petstore
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PetType }
     * 
     */
    public PetType createPetType() {
        return new PetType();
    }

    /**
     * Create an instance of {@link GetPetByNameRequest }
     * 
     */
    public GetPetByNameRequest createGetPetByNameRequest() {
        return new GetPetByNameRequest();
    }

    /**
     * Create an instance of {@link GetPetByIdRequest }
     * 
     */
    public GetPetByIdRequest createGetPetByIdRequest() {
        return new GetPetByIdRequest();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PetType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:com:example:petstore", name = "getPetByIdResponse")
    public JAXBElement<PetType> createGetPetByIdResponse(PetType value) {
        return new JAXBElement<PetType>(_GetPetByIdResponse_QNAME, PetType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPetByNameRequest }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:com:example:petstore", name = "getPetByNameRequest")
    public JAXBElement<GetPetByNameRequest> createGetPetByNameRequest(GetPetByNameRequest value) {
        return new JAXBElement<GetPetByNameRequest>(_GetPetByNameRequest_QNAME, GetPetByNameRequest.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPetByIdRequest }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:com:example:petstore", name = "getPetByIdRequest")
    public JAXBElement<GetPetByIdRequest> createGetPetByIdRequest(GetPetByIdRequest value) {
        return new JAXBElement<GetPetByIdRequest>(_GetPetByIdRequest_QNAME, GetPetByIdRequest.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:com:example:petstore", name = "fault")
    public JAXBElement<String> createFault(String value) {
        return new JAXBElement<String>(_Fault_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PetType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:com:example:petstore", name = "getPetByNameResponse")
    public JAXBElement<PetType> createGetPetByNameResponse(PetType value) {
        return new JAXBElement<PetType>(_GetPetByNameResponse_QNAME, PetType.class, null, value);
    }

}
