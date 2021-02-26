# lambdastepper

*Disclaimer: View this as an alpha-quality release. No comments, no test suite, etc.*

This is a tool for tracing the steps of a lambda calculus expression.
For example, given the expression ([example1.txt](example1.txt))
```
> (\x.\f.f x) ((\a.a a) b) (\c.c c)
```
it prints ([output1.txt](output1.txt))
```
> (\x.\f.f x) ((\a.a a) b) (\c.c c)
: ||||||||||||||||||||||||
: |||||||||||||||||||
: (\f.f ((\a.a a) b)) (\c.c c)
: ||||||||||||||||||||||||||||
: |||||||||||||||||||||
: (\c.c c) ((\a.a a) b)
: |||||||||||||||||||||
: |||||||||||||||||||||||
: (\a.a a) b ((\a.a a) b)
: ||||||||||
: |||
: b b ((\a.a a) b)
:     ||||||||||||
:     |||||
: b b (b b)
: Done (5 reductions).
----------------------------------------
```
The vertical bars between each pair of expressions show which part of the
expression was reduced at each step.  For example, in
```
: b b ((\a.a a) b)
:     ||||||||||||
:     |||||
: b b (b b)
```
The `((\a.a a) b)` was reduced to `(b b)`.

For each step, the lambdastepper chooses the *leftmost-outermost* redex.

## Running

To run the lambdastepper, place the lambda expressions to be reduced
in a file, such as `mycode.txt` and run
```
scala lambdastepper.scala mycode.txt
```
This was most recently tested with Scala 2.13.4 but should run with any version of 2.13. (It was originally written for 2.12 and will likely still work for that.)

## Format

The file should contain one or more *commands*.  This tool is intended for
small examples so each command must be on a single line.

A command can be either a lambda expression to be reduced in the form
```
> expr
```
or a definition
```
! Name = expr
```
Names are capitalized to distinguish them from ordinary variables.

Expressions to be reduced can contain free variables, but definitions cannot.
The body of a definition can refer to previously defined names, but
cannot refer to its own name or to names that have not yet been defined.

Any line that begins with a `#` is a comment.
```
# This is a comment
```

## Syntax

A *variable* can be any mix of upper and lower case letters, but must begin with a lowercase letter, as in `x` or `theFirst`.

A *name* can be any mix of upper and lower case letters beginning with an uppercase letter as in `Y` or `Add`.  In addition, a name can be one or more
digits, as `0` or `123`. (There are no built-in numbers, so digits would
typically only be used as names when simulating numbers with lambda
expressions.)

An expression can be
* A variable
* A name
* An *application* written as two expressions in a row, as in `add 3`
* A *lambda abstraction* written as `\var.body`, where `var` is a variable
 and `body` is an expression
* Any expression inside parentheses, as in `(e)`

As usual, application is left-associative and has higher priority than lambda
abstraction.

## Renaming

When doing reductions, it is sometimes necessary to rename bound variables by
adding a unique numeric suffix to the existing base name.
For example, a renamed `x` might be displayed as `x23`.

You can see this in the output of the second trace in [example2.txt](example2.txt) and [output2.txt](output2.txt),
where an `x` is renamed `x1` in line 252 of the output. Other renamings in
that trace go as high as `x8`.

## Warning

When viewing the output, if lines get long enough to wrap, they quickly
become unreadable, so turn line-wrap off if you can.
