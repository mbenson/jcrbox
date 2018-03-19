/*
 *  Copyright the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jcrbox.query;

import javax.jcr.Value;

/**
 * Modeled query parameter.
 */
public class QueryParameter {

    /**
     * {@link QueryParameter} factory method.
     * 
     * @param name
     * @param value
     * @return {@link QueryParameter}
     */
    public static QueryParameter of(String name, Value value) {
        return new QueryParameter(name, value);
    }

    /**
     * Parameter name.
     */
    public final String name;

    /**
     * Parameter value.
     */
    public final Value value;

    private QueryParameter(String name, Value value) {
        super();
        this.name = name;
        this.value = value;
    }
}
