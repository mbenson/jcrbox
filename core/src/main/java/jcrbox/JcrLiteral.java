package jcrbox;

/**
 * Broad interface to expose JCR entities as literal using Java {@code enum}s.
 * 
 * @param <E>
 *            self type
 */
public interface JcrLiteral<E extends Enum<E> & JcrLiteral<E>> {

    /**
     * Get this {@link JcrLiteral} as an {@link Enum} constant.
     *
     * @return {@code E}
     */
    @SuppressWarnings("unchecked")
    default E asEnum() {
        return (E) this;
    }

    /**
     * Get the JCR namespace of this {@link JcrLiteral}.
     * 
     * @return {@link String}
     */
    default String namespace() {
        return JcrNamespace.Helper.getNamespace(asEnum().getDeclaringClass());
    }

    /**
     * Get the basename of this {@link JcrLiteral}.
     * 
     * @return {@link String}
     * @see EnumHelper#basename(Enum)
     */
    default String basename() {
        return EnumHelper.basename(asEnum());
    }

    /**
     * Get the fully-qualified name of this {@link JcrLiteral}.
     *
     * @return {@link String}
     * @see #basename()
     * @see JcrNamespace.Helper#format(Class, String)
     */
    default String fullname() {
        return JcrNamespace.Helper.format(getClass(), basename());
    }
}
