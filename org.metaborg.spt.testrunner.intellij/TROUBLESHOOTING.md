# Troubleshooting


## Kotlin

### My method or function is never called
In this example despite control flow entering the `foo("DEBUG")` call,
the body of the `foo` function is never executed and `DEBUG` is never printed.

```
// Function declaration:
fun foo(bar: String) = {
    System.out.println(bar)
}

// Function call somewhere else:
foo("DEBUG")
```

The reason is that this function is actually returning a _lambda_, which is discarded
 by the function call. This is because of the `= {` after the function signature. Instead,
 remove the `=`:

```
// Function declaration:
fun foo(bar: String) {
    System.out.println(bar)
}

// Function call somewhere else:
foo("DEBUG")    // Prints "DEBUG"
```
