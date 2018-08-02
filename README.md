# jcrbox
Utility library for Java Content Repository API

`jcrbox` is intended to make working with JCR friendlier in the Java 8+ era. Legacy JCR APIs which predate even such deeply entrenched features as generic types have been wrapped with what are intended as more modern APIs. Additionally, jcrbox provides support for strongly-typed modeling of JCR:

* nodes
* properties
* queries

## core
Core functionality is provided by the `jcrbox` module.

_Note: jcrbox support for Java lambda functions is all specified in terms of extended `@FunctionalInterface` types, all of which rethrow JCR `RepositoryException`s wrapped as `java.lang.RuntimeException`s._

### base API

#### Jcr
This class is to be considered the primary point of entry. An instance wraps a JCR `Session`. Operations that are available on the `Session` continue to be, as the `Session` itself is only as far away as the `public final session` instance field. The `Jcr` class provides support for a number of other operations:

* `Node` operations (see `WithNode`)
* `Node` type registration/configuration
* `Query` definition/execution

As well as providing support for more basic operations in terms of jcrbox extended API types.

#### WithNode
Defines a set of convenience methods around a `Node`, including:

* Management of child/descendant `Node`s
* Customizable relative `Node` resolution defined in terms of Java lambda functions
* Relative `Node` search expressed as `Optional<WithNode>`
* Convenience mechanism to set the binary content of a given `Node` (`JCR_DATA` property)

As well as providing alternate forms of basic operations in terms of `WithNode` and other jcrbox APi types.

### type safety
jcrbox provides type safety features that are intended to provide alternatives for any point of the base JCR API that typically relies on `String` identifiers. These are built around Java `enum` types:

#### JcrLiteral
Interface describing behaviors common to jcrbox literal types:

* `#asEnum()` - provides access to the `enum` instance
* `#namespace()` - accesses JCR namespace as defined by a `@JcrNamespace` annotation attached at any point in the `enum`'s nested class structure
* `#basename()` - accesses the camel-case translation of the `enum` constant name
* `#fullname()` - accesses the fully-qualified JCR name, as _{namespace}basename_

#### JcrSource
`JcrLiteral` sub-interface describing behaviors of a jcrbox literal that can be used as a query source:

* `#selectorName()` - accesses the JCR query selector name to associate with this source

A number of `JcrSource` types can be arranged into a schema, using the `@JcrSchema` annotation. This annotation can be applied at any point at or above a given source in a nested class structure; it is recommended that sources of a schema be organized beneath a single containing class, to which the schema annotation is applied. This schema annotation indicates an implementation of the `SelectorNameStrategy` interface, which should provide a no-arg constructor. In this way a set of unique selector names can be calculated among sources of a schema.

#### JcrNode
`JcrSource` that supports the modeling of JCR `Node`s. By implementing `JcrNode` your custom `enum` type can easily model a JCR `Node` and be used in place of a `String` `Node` name at various points of the API. This promotes your custom `Node` types to "first class citizens" in your JCR codebase and supports search and refactoring. Any `enum` constant's `Node` definition can be fully customized, if needed, using the `@NodeDefinition` annotation.

#### JcrChild
`JcrSource` that supports the definition of child nodes, customizable via the `@ChildDefinition` annotation.

#### JcrProperty
`JcrLiteral` that supports the modeling of JCR `Node` properties using Java `enum`s in a similar manner; your `enum` need only implement the `JcrProperty` interface. Individual `enum` members will typically customize the property definition using `@PropertyDefinition` and nested annotations. In addition to the various data types defined by the JCR specification, it is also possible to associate a given property to a Java `enum` for its values using `@PropertyDefinition#constrainAsEnum()`. This will define the property as a `String` type with its possible values constrained to the `#name()` values of the specified `Enum` type.

#### QualifiedProperty
Represents a `JcrProperty P` of `JcrSource S` and can be easily obtained using the instance method `#of(JcrSource)` available on a given `JcrProperty` instance, e.g. `MyProperty.NAME.of(MyNode.PERSON)`.

#### JcrQuery
This is a type of marker class that can be used to declare that a given class literal will be used to represent a specific JCR stored query. The position of your `JcrQuery` subclass in a nested class structure is used to dictate the corresponding repository path at which a stored query definition will be stored/from which it will be retrieved. From an organizational perspective, the class definition becomes a natural place to store query-specific bind variable names.

### JCR query APIs
Beyond permitting the representation of a JCR query by a Java class instance, other enhanced APIs exist around queries.

#### QueryBuilder
This is an abstract class that uses a so-called "snap-in" implementation pattern to allow a subclass to make direct calls to the methods of JCR's `QueryObjectModelFactory` as though the builder were itself the `QueryObjectModelFactory` instance, the goal of this being a general reduction in code verbosity. `QOMF` methods are overloaded to accept jcrbox types and other conveniences are provided, notably the nested `CreateQuery` fluent builder.

##### QueryBuilder.Strong
This `QueryBuilder` subclass allows a further optimization: extend this class binding a `JcrQuery` class to its type parameter and the `QueryBuilder` now carries with it the associated `JcrQuery`.

#### QueryParameter
Models a query parameter so that this bi-valued tuple can be used with Java varargs-enabled query execution methods. `Jcr` hosts a factory method that uses a lambda function to derive a `Value` from the `ValueFactory` associated with the `Session`. Alternatively, a `QueryParameter` instance can be constructed directly, often using `Jcr#createValue(Enum)`.

#### JcrResult
This is the type returned by jcrbox query execution APIs and simply adapts the classic JCR `QueryResult` type to a more modern API:

* `java.lang.Iterable` is implemented
* fine-grained result data is exposed in terms of parameterized rather than raw and array types.

#### JcrRow
This is the element type of a `JcrResult` and enhances the classic JCR query `Row` type with awareness of jcrbox types:

* `JcrNode`
* `WithNode`

## spring-boot support
The `jcrbox-spring-boot` module adds Spring Boot autoconfiguration of `Repository`, `Session` and `Jcr` instances. Any beans defined that implement `JcrConsumer<Jcr>` will be taken as configurers of the `Session` via the `Jcr` API and invoked. Likewise, any `QueryBuilder.Strong` beans will be invoked to store their queries.
