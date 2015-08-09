What is RESOLVE?
==============

<img align="right" src="compiler/resources/resolve_logo.png"/>

RESOLVE (REusable SOftware Language with VErification) is a programming and
specification language designed for verifying correctness of object oriented
programs.

The RESOLVE language is designed from the ground up to facilitate *mathematical
reasoning*. As such, the language provides syntactic slots for assertions such
as pre-post conditions that are capable of abstractly describing a program's
intended behavior. In writing these assertions, users are free to draw from from
a variety of pre-existing and user-defined mathematical theories containing
fundamental axioms, definitions, and results necessary/useful in establishing
program correctness.

All phases of the verification process spanning verification condition (VC)
generation to proving are performed in-house, while RESOLVE programs are
translated to Java and run on the JVM.

##Authors and major contributors
The creation and continual evolution of the RESOLVE language is owed to an
ongoing joint effort between Clemson University, The Ohio State University, and
countless educators and researchers from a variety of [other]
(http://www.cs.clemson.edu/resolve/about.html) institutions.

Developers of this particular test/working-iteration of the RESOLVE compiler
include:

* [The Resuable Software Research Group (RSRG)]
(http://www.cs.clemson.edu/resolve/) - School of Computing, Clemson University

##First steps with RESOLVE

###Requirements
To get started, you really only need to have Java JDK 1.8 installed on either
Windows, Mac OSX, or Linux (any distribution should do).

###Installing
1. You can download the latest stable build of the tool from the [releases page]
(). Note that the tool comes packaged in a `.zip` containing both standard
RESOLVE libraries and other core `.java` classes required by generated code.

2. Unpack the `.zip` to the directory of your choosing, for example, on OSX:
`/usr/local/<resolve_root_zip_directory>` (that is: `/usr/local/resolve/`).
*The rest of these instructions will assume this directory -- so make changes
accordingly.*

3. To help the compiler find the location of standard RESOLVE libraries, it is
necessary to set a `RESOLVEROOT` environment variable:

  1. **OSX & Linux**: Simply open a new terminal and type the following:
    `export RESOLVEROOT="/usr/local/resolve"`
    which should add the variable to your `.bash_profile`

  2. **Windows**: Todo

###Setting classpath

With the above steps complete, we need to make sure that Java will be able to
find the RESOLVE tool -- which means we need to set the classpath variable
accordingly. So once again, open a terminal and type the following:

```
export CLASSPATH=".:$RESOLVEROOT/tool/resolve-<VERSION>-complete.jar:$CLASSPATH"
```

where `<VERSION>` corresponds to the three digit semantic version contained
within the name of `.zip` (and in the release notes) for the desired build of
the tool. For this tutorial, we assume `0.0.1`.

To ensure all is well, restart the terminal and run the following command to
launch the tool without arguments:

```
java -jar /usr/local/resolve/tool/resolve-0.0.1-complete.jar
```

and you should receive the following help prompt:

```
RESOLVE Compiler Version 0.0.1
 -longMessages        show exception details on errors
 -o ___               specify an output directory where all output is generated
 -lib ___             specify working directory containing *.resolve source files
 ...
```

###Optional* (but recommended): creating an alias

Typing the above command to run the tool over an over again would be admittedly
painful. So to facilitate easier usage of the compiler from the terminal, we
can set an alias as follows:

```
alias resolve='java -jar $RESOLVEROOT/tool/resolve-0.0.1-complete.jar'
```
now simply typing `resolve` gives the same effect as the (long) command given
in the previous section.

### The obligatory "hello world" example
Now that we have the compiler setup, go ahead create a new file called
`Hello.resolve` and punch in the following bit of RESOLVE:

```
Facility Hello;
        uses Standard_Char_Strings;

    Operation Main();
        Procedure
            Std_Char_Str_Fac :: Write_Line("hello world!");
        end Main;
end Hello;
```
To run, open the terminal, cd to the directory where you've
saved the file and type:

```
resolve Hello.resolve -genCode Java -jar
```

The compiler will do some thinking, and eventually produce `Hello.jar`, which
is run as follows:
```
$java -jar Hello.jar
>hello world!
```

##Useful information

* [Release notes]()
* [Official site](http://www.cs.clemson.edu/resolve/)
* [RESOLVE wiki] (https://github.com/Welchd1/resolve-lite/wiki)

##Copyright and license

Copyright (c) Clemson University, 2015. All rights reserved. The use and
distribution terms for this software are covered by the BSD 3-clause license
which can be found in the file `LICENSE.txt` at the root of this repository.
By using this software in any fashion, you are agreeing to be bound by the terms
of this license. You must not remove this notice, or any other, from this
software.