easy-auth-info
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-auth-info.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-auth-info)


SYNOPSIS
--------

    easy-auth-info file <uuid> <path>
    easy-auth-info run-service

DESCRIPTION
-----------

Provides consolidated authorization info about items in a bag store.


ARGUMENTS
---------

    Options:

        --help      Show help message
        --version   Show version of this program

    Subcommand: file - get accessibility of a file
          --help   Show help message
    
     trailing arguments:
      uuid (required)
      path (required)
    ---
    
    Subcommand: run-service - Starts EASY Auth Info as a daemon that services HTTP requests
        --help   Show help message
    ---


HTTP service
------------

When started with the sub-command `run-service` a REST API becomes available with HTTP method `GET` only.
In a path pattern `*` refers to any completion of the path, placeholders for variables start with a colon,
and optional parts are enclosed in square brackets.

Path       | Action
-----------|------------------------------------
`/`        | Return a simple message to indicate that the service is up: "EASY Auth Info Service running..."
`/:uuid/*` | Return the rights for the file from the bag with bag-id `:uuid` and bag local path `*`


EXAMPLES
--------

    easy-auth-info 40594b6d-8378-4260-b96b-13b57beadf7c data/pakbon.xml
    curl http://localhost:20170/40594b6d-8378-4260-b96b-13b57beadf7c/data/pakbon.xml

Note the space between uuid and path in the command line interface which becomes a slash for the REST interface.


INSTALLATION AND CONFIGURATION
------------------------------


### Dependencies

* [easy-bag-store](https://github.com/DANS-KNAW/easy-bag-store/)


### Steps

1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-auth-info-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-auth-info-<version>/bin/easy-auth-info /usr/bin



General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-auth-info.git
        cd easy-auth-info
        mvn install
