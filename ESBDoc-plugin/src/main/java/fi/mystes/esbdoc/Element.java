/**
 * Copyright 2018 Mystes Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import static fi.mystes.esbdoc.Element.Attribute.BEAN;
import static fi.mystes.esbdoc.Element.Attribute.NAME;
import static fi.mystes.esbdoc.Element.Type.PROPERTY;
import static fi.mystes.esbdoc.Element.Type.SPRING;

/**
 * Created by mystes-am on 27.5.2015.
 */
public class Element {

    public enum Type {
        PROXY("proxy"),
        ENDPOINT("endpoint"),
        ADDRESS("address"),
        SEQUENCE_TYPE("sequence"),
        CUSTOM_CALLOUT("customcallout"),
        CALLOUT("callout"),
        TARGET("target"),
        FILTER("filter"),
        ELSE("else"),
        SWITCH("switch"),
        CASE("case"),
        DEFAULT("default"),
        FAULT_SEQUENCE("faultsequence"),
        ITERATE("iterate"),
        PROPERTY("property"),
        SPRING("spring"),
        CLASS("class");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public enum Attribute {
        NAME("name"),
        KEY("key"),
        URI("uri"),
        SERVICE_URL("serviceURL"),
        ENDPOINT_KEY("endpointKey"),
        IN_SEQUENCE("inSequence"),
        OUT_SEQUENCE("outSequence"),
        SEQUENCE_VALUE("sequence"),
        XPATH("xpath"),
        SOURCE("source"),
        REGEX("regex"),
        EXPRESSION("expression"),
        VALUE("value"),
        BEAN("bean"),
        MESSAGE_STORE("messageStore");

        private final String name;

        Attribute(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public enum SpringBean {
        SIMPLE_ITERATOR("simpleIterator");

        private final String name;
        private final Springproperty[] properties;

        SpringBean(String name, Springproperty... properties) {
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return this.name;
        }

        public Springproperty[] getProperties() {
            return this.properties;
        }
    }

    public enum Springproperty {
        SIMPLE_ITERATOR_SPLIT_EXPRESSION("simpleiterator.splitexpression"),
        SIMPLE_ITERATOR_TARGET("simpleiterator.target");

        private final String name;

        Springproperty(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    private final String name;
    private final Attributes attributes;

    protected Element(String name) {
        this.name = name;
        this.attributes = new AttributesImpl();
    }

    protected Element(String name, Attributes attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    protected boolean is(Type type) {
        return is(type.getName().toLowerCase());
    }

    protected boolean is(String type) {
        return StringUtils.equals(this.name.toLowerCase(), type.toLowerCase());
    }

    protected boolean nameContains(String partOfname) {
        return StringUtils.contains(this.name.toLowerCase(), partOfname.toLowerCase());
    }

    protected boolean isNot(Type type) {
        return !is(type);
    }

    protected boolean isAnyOf(Type... types) {
        for (Type type : types) {
            if (this.is(type)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isAnyOf(String... types) {
        for (String type : types) {
            if (this.is(type)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isNoneOf(String... types) {
        return !isAnyOf(types);
    }

    protected boolean has(Attribute attribute) {
        return has(attribute.getName());
    }

    protected boolean has(String attributeName) {
        return StringUtils.isNotEmpty(this.get(attributeName));
    }

    protected boolean doesNotHave(Attribute attribute) {
        return !has(attribute);
    }

    protected boolean doesNotHave(String attributeName) {
        return !has(attributeName);
    }

    protected String get(Attribute attribute) {
        return get(attribute.getName());
    }

    protected String get(String attributeName) {
        return this.getAttributes().getValue(attributeName);
    }

    protected boolean attributeValueEquals(Attribute target, String expectedValue) {
        if (doesNotHave(target)) {
            return false;
        }
        String actualValue = get(target);
        return StringUtils.equalsIgnoreCase(expectedValue, actualValue);
    }

    protected String getName() {
        return this.name;
    }

    protected Attributes getAttributes() {
        return this.attributes;
    }

    protected boolean isSpringBean(SpringBean bean) {
        return isSpringBean(bean.getName());
    }

    protected boolean isSpringBean(String beanName) {
        return this.is(SPRING) && this.attributeValueEquals(BEAN, beanName);
    }

    protected boolean hasSpringProperty(Springproperty property) {
        return hasSpringProperty(property.getName());
    }

    protected boolean hasSpringProperty(String propertyName) {
        return this.is(PROPERTY) && this.attributeValueEquals(NAME, propertyName);
    }

}
