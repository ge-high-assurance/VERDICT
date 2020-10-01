package com.ge.verdict.gsn;

import javax.xml.bind.annotation.XmlElement;

/** @author Saswata Paul */
public class Solution {

    @XmlElement
    /** An unique Id */
    protected String id;

    @XmlElement
    /** Some text to display */
    protected String displayText;

    @XmlElement
    /** Solution status true if supports goal false if does not support goal */
    protected boolean status;

    @XmlElement
    /** Additional information as string */
    protected String moreInfo;

    @XmlElement
    /** A clickable URL */
    protected String url;

    @XmlElement
    /** The address of the soteria output xml file for the solution */
    protected String soteriaOutputXml;

    /**
     * Gets the value of the soteriaOutputXml property.
     *
     * @return possible object is {@link String }
     */
    protected String getSoteriaOutputXml() {
        return soteriaOutputXml;
    }

    /**
     * Sets the value of the soteriaOutputXml property.
     *
     * @param value allowed object is {@link String }
     */
    protected void setSoteriaOutputXml(String value) {
        this.soteriaOutputXml = value;
    }

    /**
     * Gets the value of the URL property.
     *
     * @return possible object is {@link String }
     */
    protected String getUrl() {
        return url;
    }

    /**
     * Sets the value of the Url property.
     *
     * @param value allowed object is {@link String }
     */
    protected void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link String }
     */
    protected String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is {@link String }
     */
    protected void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the displayText property.
     *
     * @return possible object is {@link String }
     */
    protected String getDisplayText() {
        return displayText;
    }

    /**
     * Sets the value of the displayText property.
     *
     * @param value allowed object is {@link String }
     */
    protected void setDisplayText(String value) {
        this.displayText = value;
    }

    /**
     * Gets the value of the status property.
     *
     * @return possible object is {@link boolean }
     */
    protected boolean getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     *
     * @param value allowed object is {@link boolean }
     */
    protected void setStatus(boolean value) {
        this.status = value;
    }

    /**
     * Gets the value of the moreInfo property.
     *
     * @return possible object is {@link String }
     */
    protected String getExtraInfo() {
        return moreInfo;
    }

    /**
     * Sets the value of the extraInfo property.
     *
     * @param value allowed object is {@link String }
     */
    protected void setExtraInfo(String value) {
        this.moreInfo = value;
    }
}
