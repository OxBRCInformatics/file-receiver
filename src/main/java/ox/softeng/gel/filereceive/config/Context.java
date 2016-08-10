
package ox.softeng.gel.filereceive.config;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for context complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="context">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="folder" type="{http://www.filereceive.gel.softeng.ox/1.0.1}folder" maxOccurs="unbounded"/>
 *         &lt;element name="headers" type="{http://www.filereceive.gel.softeng.ox/1.0.1}headers" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="exchange" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="refreshFrequency" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *       &lt;attribute name="path" type="{http://www.w3.org/2001/XMLSchema}string" default="/" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "context", namespace = "http://www.filereceive.gel.softeng.ox/1.0.1", propOrder = {
    "folder",
    "headers"
})
public class Context {

    @XmlElement(namespace = "http://www.filereceive.gel.softeng.ox/1.0.1", required = true)
    protected List<Folder> folder;
    @XmlElement(namespace = "http://www.filereceive.gel.softeng.ox/1.0.1")
    protected Headers headers;
    @XmlAttribute(name = "exchange")
    protected String exchange;
    @XmlAttribute(name = "refreshFrequency")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger refreshFrequency;
    @XmlAttribute(name = "path")
    protected String path;

    /**
     * Gets the value of the folder property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the folder property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFolder().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Folder }
     * 
     * 
     */
    public List<Folder> getFolder() {
        if (folder == null) {
            folder = new ArrayList<Folder>();
        }
        return this.folder;
    }

    /**
     * Gets the value of the headers property.
     * 
     * @return
     *     possible object is
     *     {@link Headers }
     *     
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Sets the value of the headers property.
     * 
     * @param value
     *     allowed object is
     *     {@link Headers }
     *     
     */
    public void setHeaders(Headers value) {
        this.headers = value;
    }

    /**
     * Gets the value of the exchange property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * Sets the value of the exchange property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExchange(String value) {
        this.exchange = value;
    }

    /**
     * Gets the value of the refreshFrequency property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRefreshFrequency() {
        return refreshFrequency;
    }

    /**
     * Sets the value of the refreshFrequency property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRefreshFrequency(BigInteger value) {
        this.refreshFrequency = value;
    }

    /**
     * Gets the value of the path property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPath() {
        if (path == null) {
            return "/";
        } else {
            return path;
        }
    }

    /**
     * Sets the value of the path property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPath(String value) {
        this.path = value;
    }

}
