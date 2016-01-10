# RESOLVE Compiler Command Line Options

There are currently a number of options you can supply to the compiler. This document simply gives a detailed description 
and example usage of each. Note that this list will likely fluctuate and change over time, so keep an eye on this list and 
update it accordingly.

## Glossary

If you invoke the RESOLVE compiler without command line arguments, you should get a help message that looks something like 
this:

```
RESOLVE Compiler Version 0.0.1
 -longMessages       show exception details on errors
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



