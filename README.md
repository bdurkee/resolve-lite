What is RESOLVE?
==============

<img align="right" src="compiler/resources/resolve_logo.png"/>

RESOLVE (REusable SOftware Language with VErification) is a programming and
specification language designed for verifying correctness of object oriented
programs.

The RESOLVE language provides syntactic slots for mathematical assertions such
as pre-post conditions that are capable of abstractly describing a program's
intended behavior. Users write these assertions using definitions from a variety
of pre-existing and user-defined mathematical theories.

All phases of the verification process spanning verification condition (VC)
generation to proving are performed in-house, while RESOLVE programs themselves
are translated to Java and run on the JVM.

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
Windows, Mac OSX, or linux (any distribution should do).

###Installing
1. You can download the latest stable build of the tool from the [releases page]
(). Note that the tool comes packaged in a `.zip` containing both standard
RESOLVE libraries and other core `.java` classes required by generated code.

2. Unpack the `.zip` to the directory of your choosing, for example, on OSX:
`/usr/local/<resolve_root_zip_directory>`.

3. To help the compiler find the location of standard RESOLVE libraries, it is
necessary to set a `RESOLVEROOT` environment variable:

  1. **OSX & Linux**: Simply open a new terminal and type the following:
    `export RESOLVEROOT="/usr/local/resolve"`
    which should add the variable to your `.bash_profile`

  2. **Windows**: Todo

###Optional (but recommended): Creating a compiler alias

To allow running the compiler by simply


### The obligatory "hello world" example
Once installed, go ahead and punch in the following bit of RESOLVE:

```
Facility Hello;
        uses Standard_Characters;

    Operation Main();
        Procedure
            Std_Character_Fac :: Write_Line("hello world!");
        end Main;
end Hello;
```