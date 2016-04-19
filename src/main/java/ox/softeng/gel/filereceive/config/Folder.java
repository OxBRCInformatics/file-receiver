
package ox.softeng.gel.filereceive.config;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for folder complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="folder">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="monitorDirectory" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="moveDirectory" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="bindingKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="headers" type="{http://www.filereceive.gel.softeng.ox/1.0.1}headers" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="action" type="{http://www.filereceive.gel.softeng.ox/1.0.1}action" default="MOVE" />
 *       &lt;attribute name="refreshFrequency" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *       &lt;attribute name="exchange" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "folder", namespace = "http://www.filereceive.gel.softeng.ox/1.0.1", propOrder = {
    "monitorDirectory",
    "moveDirectory",
    "bindingKey",
    "headers"
})
public class Folder {

    @XmlElement(namespace = "http://www.filereceive.gel.softeng.ox/1.0.1", required = true)
    protected String monitorDirectory;
    @XmlElement(namespace = "http://www.filereceive.gel.softeng.ox/1.0.1", required = true)
    protected String moveDirectory;
    @XmlElement(namespace = "http://www.filereceive.gel.softeng.ox/1.0.1", required = true)
    protected String bindingKey;
    @XmlElement(namespace = "http://www.filereceive.gel.softeng.ox/1.0.1")
    protected Headers headers;
    @XmlAttribute(name = "action")
    protected Action action;
    @XmlAttribute(name = "refreshFrequency")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger refreshFrequency;
    @XmlAttribute(name = "exchange")
    protected String exchange;

    /**
     * Gets the value of the monitorDirectory property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMonitorDirectory() {
        return monitorDirectory;
    }

    /**
     * Sets the value of the monitorDirectory property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMonitorDirectory(String value) {
        this.monitorDirectory = value;
    }

    /**
     * Gets the value of the moveDirectory property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMoveDirectory() {
        return moveDirectory;
    }

    /**
     * Sets the value of the moveDirectory property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMoveDirectory(String value) {
        this.moveDirectory = value;
    }

    /**
     * Gets the value of the bindingKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBindingKey() {
        return bindingKey;
    }

    /**
     * Sets the value of the bindingKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBindingKey(String value) {
        this.bindingKey = value;
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
     * Gets the value of the action property.
     * 
     * @return
     *     possible object is
     *     {@link Action }
     *     
     */
    public Action getAction() {
        if (action == null) {
            return Action.MOVE;
        } else {
            return action;
        }
    }

    /**
     * Sets the value of the action property.
     * 
     * @param value
     *     allowed object is
     *     {@link Action }
     *     
     */
    public void setAction(Action value) {
        this.action = value;
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

}
