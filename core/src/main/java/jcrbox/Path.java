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
package jcrbox;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import jcrbox.util.Lazy;

/**
 * Represents a JCR path.
 */
public class Path implements Iterable<Path.Element> {

    /**
     * Represents a single path element.
     */
    public static class Element {

        /**
         * Namespace of this {@link Element}.
         */
        public final String ns;

        /**
         * Simple name of this {@link Element}.
         */
        public final String name;

        private Element(String ns, String name) {
            super();
            this.ns = StringUtils.trimToEmpty(ns);
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Element) {
                final Element other = (Element) obj;
                return Objects.equals(ns, other.ns) && Objects.equals(name, other.name);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(ns, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return ns.isEmpty() ? name : String.format("{%s}%s", ns, name);
        }

        /**
         * Render this {@link Element} using the specified {@link NamespaceRegistry}.
         *
         * @param namespaceRegistry
         * @return {@link StringIndexOutOfBoundsException}
         * @throws RepositoryException
         */
        public String toString(NamespaceRegistry namespaceRegistry) throws RepositoryException {
            if (ns.isEmpty()) {
                return name;
            }
            try {
                return String.format("%s:%s", namespaceRegistry.getPrefix(ns), name);
            } catch (NamespaceException e) {
            }
            return toString();
        }
    }

    /**
     * Fluent {@link Path} builder API.
     */
    public static class Builder implements Supplier<Path> {

        private boolean absolute;
        private final List<Element> elements = new ArrayList<>();

        private Builder() {
        }

        private Builder(List<Element> elements) {
            this.elements.addAll(elements);
        }

        /**
         * Specify that an absolute {@link Path} should be built.
         *
         * @return {@code this}, fluently
         */
        public Builder absolute() {
            return absolute(true);
        }

        /**
         * Specify whether the {@link Path} built should be absolute.
         *
         * @param absolute
         * @return {@code this}, fluently
         */
        public Builder absolute(boolean absolute) {
            this.absolute = absolute;
            return this;
        }

        /**
         * Specify the next path element in qualified form.
         *
         * @param uri
         * @param name
         * @return {@code this}, fluently
         */
        public Builder next(String uri, String name) {
            elements.add(new Element(uri, name));
            return this;
        }

        /**
         * Specify the next path element as an unqualified name.
         *
         * @param name
         * @return {@code this}, fluently
         */
        public Builder next(String name) {
            return next("", name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Path get() {
            return new Path(absolute, elements);
        }
    }

    private static class Parser {

        final NamespaceRegistry namespaceRegistry;
        final String path;
        final ParsePosition pos = new ParsePosition(0);

        private Parser(NamespaceRegistry namespaceRegistry, String path) {
            super();
            this.namespaceRegistry = namespaceRegistry;
            this.path = path;
        }

        Builder parse(Builder b) throws RepositoryException {
            if (path.charAt(0) == '/') {
                b.absolute();
                next();
            }
            while (validPosition()) {
                b.next(namespace(), seek('/'));
                if (validPosition()) {
                    next();
                }
            }
            return b;
        }

        private String namespace() throws RepositoryException {
            final int start = pos.getIndex();

            char c = path.charAt(start);
            if (c == '{') {
                next();
                final String ns = seek('}');
                Validate.validState(pos.getIndex() < path.length(), "Malformed uri at pos %d of %s", start, path);
                next();
                return ns;
            }
            while (validPosition()) {
                c = path.charAt(pos.getIndex());
                if (c == '/') {
                    break;
                }
                try {
                    if (c == ':') {
                        final String prefix = path.substring(start, pos.getIndex());
                        Validate.validState(namespaceRegistry != null, "No %s specified; cannot resolve ns prefix '%s'",
                            NamespaceRegistry.class.getSimpleName(), prefix);
                        return namespaceRegistry.getURI(prefix);
                    }
                } finally {
                    next();
                }
            }
            pos.setIndex(start);
            return null;
        }

        private String seek(char c) {
            final int start = pos.getIndex();
            int i = path.indexOf(c, start);
            if (i < 0) {
                pos.setIndex(path.length());
                return path.substring(start);
            }
            pos.setIndex(i);
            return path.substring(start, i);
        }

        private void next() {
            pos.setIndex(pos.getIndex() + 1);
        }

        private boolean validPosition() {
            return pos.getIndex() < path.length();
        }
    }

    /**
     * Obtain a path builder.
     *
     * @return {@link Path.Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parse a path.
     *
     * @param namespaceRegistry
     *            required if path contains colon-delimited namespace prefixes.
     * @param path
     * @return {@link Path}
     * @throws RepositoryException
     */
    public static Path parse(NamespaceRegistry namespaceRegistry, String path) throws RepositoryException {
        return new Parser(namespaceRegistry, path).parse(builder()).get();
    }

    private final boolean absolute;
    private final List<Path.Element> elements;
    private final Lazy<String> toString;

    private Path(boolean absolute, List<Path.Element> elements) {
        this.absolute = absolute;
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements));
        toString = new Lazy<>(
            () -> stream().map(Object::toString).collect(Collectors.joining("/", Path.this.absolute ? "/" : "", "")));
    }

    /**
     * Learn whether this {@link Path} is absolute.
     *
     * @return {@code boolean}
     */
    public boolean isAbsolute() {
        return absolute;
    }

    /**
     * Learn whether this {@link Path} is relative.
     *
     * @return {@code boolean}
     */
    public boolean isRelative() {
        return !absolute;
    }

    /**
     * Learn whether this {@link Path} is empty.
     *
     * @return {@code boolean}
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<Element> iterator() {
        return elements.listIterator();
    }

    /**
     * Return a {@link Stream} of the {@link Element}s of this {@link Path}.
     *
     * @return {@link Stream}
     */
    public Stream<Element> stream() {
        return elements.stream();
    }

    /**
     * Get a {@link Path} representing the parent of this {@link Path}. An empty {@link Path} is its own parent.
     *
     * @return {@link Path}
     */
    public Path parent() {
        if (elements.isEmpty()) {
            return this;
        }
        return new Path(absolute, elements.subList(0, elements.size() - 1));
    }

    /**
     * Return this path in absolute form.
     *
     * @return {@link Path}
     */
    public Path absolute() {
        return absolute ? this : new Path(true, elements);
    }

    /**
     * Return this path in relative form.
     *
     * @return {@link Path}
     */
    public Path relative() {
        return absolute ? new Path(false, elements) : this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Path) {
            final Path other = (Path) obj;
            return absolute == other.absolute && elements.equals(other.elements);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(absolute, elements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString.get();
    }

    /**
     * Render to {@link String} using the specified {@link NamespaceRegistry}.
     *
     * @param namespaceRegistry
     * @return {@link String}
     * @throws RepositoryException
     */
    public String toString(NamespaceRegistry namespaceRegistry) throws RepositoryException {
        final StringBuilder buf = new StringBuilder();
        if (isAbsolute()) {
            buf.append('/');
        }
        boolean first = true;
        for (Element e : elements) {
            if (first) {
                first = false;
            } else {
                buf.append('/');
            }
            buf.append(e.toString(namespaceRegistry));
        }
        return buf.toString();
    }

    /**
     * Get a path builder whose result will be a child of this {@link Path}.
     *
     * @return {@link Path.Builder}
     */
    public Builder child() {
        return new Builder(elements).absolute(absolute);
    }
}
