/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.cache.internal;

import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.List;

public final class CacheVersion implements Comparable<CacheVersion> {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    public static final String COMPONENT_SEPARATOR = ".";

    public static CacheVersion parse(String version) {
        List<String> parts = Splitter.on(COMPONENT_SEPARATOR).splitToList(version);
        int[] components = new int[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            components[i] = Integer.parseInt(parts.get(i));
        }
        return new CacheVersion(components);
    }

    public static CacheVersion empty() {
        return new CacheVersion(EMPTY_INT_ARRAY);
    }

    public static CacheVersion of(int component) {
        return new CacheVersion(new int[] {component});
    }

    public static CacheVersion of(int... components) {
        return new CacheVersion(components.clone());
    }

    private final int[] components;

    private CacheVersion(int[] components) {
        this.components = components;
    }

    public CacheVersion append(int additionalComponent) {
        int[] appendedComponents = new int[components.length + 1];
        System.arraycopy(components, 0, appendedComponents, 0, components.length);
        appendedComponents[components.length] = additionalComponent;
        return new CacheVersion(appendedComponents);
    }

    @Override
    public String toString() {
        return Ints.join(COMPONENT_SEPARATOR, components);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CacheVersion that = (CacheVersion) o;
        return Arrays.equals(this.components, that.components);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(components);
    }

    @Override
    public int compareTo(CacheVersion that) {
        int minLength = Math.min(this.components.length, that.components.length);
        for (int i = 0; i < minLength; i++) {
            int result = Ints.compare(this.components[i], that.components[i]);
            if (result != 0) {
                return result;
            }
        }
        return Ints.compare(this.components.length, that.components.length);
    }
}
