package com.fagner.test.apollo;

/**
 *
 * @author Fagner
 */
public class Test {

    public String attributeOne;

    public String attributeTwo;

    public String getAttributeOne() {
        return attributeOne;
    }

    public void setAttributeOne(String attributeOne) {
        this.attributeOne = attributeOne;
    }

    public String getAttributeTwo() {
        return attributeTwo;
    }

    public void setAttributeTwo(String attributeTwo) {
        this.attributeTwo = attributeTwo;
    }

    @Override
    public String toString() {
        return String
                .format("Attribute one: %s. Attribute two: %s",
                        this.attributeOne,
                        this.attributeTwo);
    }
}
