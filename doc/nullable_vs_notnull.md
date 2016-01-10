## The story on `@NotNull` and `@Nullable`

These annotations are contracts for whether or not variables, fields, params,
etc are allowed to be null (or not). Note that these annotations are external,
meaning they come from the following Jetbrains dependency:

```
<dependency>
    <groupId>com.intellij</groupId>
    <artifactId>annotations</artifactId>
    <version>12.0</version>
</dependency>
```

After some fluctuation, I think I've settled on a somewhat consistent convention
for what to place these annotations on, and where.

###