# Introduction

# Local Filesystem

The main use of the local file system for the core library (other than the validator - see below) is for the 
[NPM package cache](https://confluence.hl7.org/display/FHIR/FHIR+Package+Cache). The default location and content
is as specified in the FHIR specification, but you can choose where this goes if you want, or provide your own NPM package cache manager. However there are other uses of the local file system scattered throughout the code, particularly in the test cases.

All access to the local file system runs through the class ManagedFileAccess. You can 
set the static features of this class to completely cut the library off from the 
local filesystem, or provide your own file system accessor, or limit the files accessed
to particular sub-directories. See ManagedFileAccess for details.

Note that libraries that this library depends on still access the filesystem directly. Review
of the use of these libraries is ongoing.

Validator: The validator CLI also accesses local files as specified in the command line parameters, 
and runs in the user context. TODO: we are considering whether to support a command line parameter 
restricting path access to particular directories.

# Network access

The library will access the web to download needed collateral, or to access terminology resources or servers.
All access is by http(s) using the Apache... library, and is controlled by the class XXXX where you can 
turn all network access off.

# Logging 

todo

# Terminology Server Access

todo

# Cryptography 

Other than the https client, the library doesn't have any crypto functions in it. 

TODO: Actually, it does, reading SHCs 

