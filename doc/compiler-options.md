# RESOLVE Compiler Command Line Options

There are currently a number of options you can supply to the compiler. This document simply gives a detailed description 
and example usage of each. Note that this list will likely fluctuate and change over time, so keep an eye on this list and 
update it accordingly.

## Glossary

If you invoke the RESOLVE compiler without command line arguments, you should get a help message that looks something like 
this:

```
RESOLVE Compiler Version 0.0.1
 -o ___              specify output directory where all output is generated
 -long-messages      show exception details when available for errors and warnings
 -lib ___            specify location of resolve source files
 -genCode ___        generate code
 -package ___        specify a package/namespace for the generated code
 -vcs                generate verification conditions (VCs)
 -Xlog               dump lots of logging info to edu.clemson.resolve-timestamp.log
``` 

Here are some additional details on the options:

## `-o <outdir>`

RESOLVE generates output files in the current directory by default. This option specifies where the compiler will place any generated code such as VC `asrt` files, executable java, proof results, logs files, etc.

```
$ resolve -o /tmp T.resolve
$ ls /tmp/T*
/tmp/T.java
```

## `-lib <libdir>`

Sets a root search directory for the compiler. That is, when searching for a targetfile (or anything else) the compiler will start its search at `lib` and proceed recursively through all subdirectories. 

Example. If your desired workspace directory is `playground` and is organized as follows:

```
$ cd ~/Documents/playground/
$ ls
$ concepts    facilities    precis
```
and you're targetfile is `Documents/playground/concepts/foo_template/T.resolve`, then you'll probably want to set `libdir` to `Documents/playground` as follows:
```
$ resolve concepts/foo_template/T.resolve -lib Documents/playground 
```

## `-genCode <target>`

Specifies that the compiler should generate executable code for the current `target` file. Note that currently we only support a single target language: `Java`. 

*Note: It's likely that we won't be adding any new targets (in near the future that is) so this command should likely be made argumentless soon.*

## `-package <pkg>`

Use this option to specify a package or namespace for any generated, executable `.java` files. If this is not present when generating java code, no namespace is present for the generated files.

## `-vcs`

Tells the compiler to generate VCs (**V**erification **C**ondition**s**) for the current target file. Results are written out to the current directory (unless `-o` is present) as a `.asrt` file with the same name as the target file specified.

