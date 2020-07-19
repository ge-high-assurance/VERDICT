package com.ge.verdict.gsn;

public class Context {
    /** An unique Id */
    protected String id;

    /** Some text to display */
    protected String displayText;

    /** Additional information as string */
    protected String moreInfo;

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link String }
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is {@link String }
     */
    public void setid(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the displayText property.
     *
     * @return possible object is {@link String }
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * Sets the value of the displayText property.
     *
     * @param value allowed object is {@link String }
     */
    public void setDisplayText(String value) {
        this.displayText = value;
    }
    
    
    /**
     * Gets the value of the moreInfo property.
     *
     * @return possible object is {@link String }
     */
    public String getExtraInfo() {
        return moreInfo;
    }

    /**
     * Sets the value of the extraInfo property.
     *
     * @param value allowed object is {@link String }
     */
    public void setExtraInfo(String value) {
        this.moreInfo = value;
    }
}
