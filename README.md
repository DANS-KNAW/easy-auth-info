easy-auth-info
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-auth-info.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-auth-info)


SYNOPSIS
--------

    easy-auth-info <uuid>/<path>
    easy-auth-info run-service

DESCRIPTION
-----------

Provides consolidated authorization info about items in a bag store.


ARGUMENTS
---------

    Options:

        --help      Show help message
        --version   Show version of this program

    Subcommand: run-service - Starts EASY Auth Info as a daemon that services HTTP requests
        --help   Show help message
    ---


HTTP service
------------

When started with the sub-command `run-service` a REST API becomes available with HTTP method `GET` only.
In a path pattern `*` refers to any completion of the path, placeholders for variables start with a colon,
and optional parts are enclosed in square brackets.

Method   | Path       | Action
---------|------------|------------------------------------
`GET`    | `/`        | Return a simple message to indicate that the service is up: "EASY Auth Info Service running..."
`GET`    | `/:uuid/*` | Return the rights for the file from the bag with bag-id `:uuid` and bag local path `*`


EXAMPLES
--------

```jshelllanguage
easy-auth-info 40594b6d-8378-4260-b96b-13b57beadf7c/data/pakbon.xml
curl 'http://localhost:20170/40594b6d-8378-4260-b96b-13b57beadf7c/data/pakbon.xml'
```

```json
{
  "itemId":"40594b6d-8378-4260-b96b-13b57beadf7c/data/pakbon.xml",
  "owner":"someone",
  "dateAvailable":"1992-07-30",
  "accessibleTo":"KNOWN",
  "visibleTo":"KNOWN"
}
```

INSTALLATION AND CONFIGURATION
------------------------------


### Depending on services

* [easy-bag-store](https://github.com/DANS-KNAW/easy-bag-store/)


### Steps

1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-auth-info-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-auth-info-<version>/bin/easy-auth-info /usr/bin



General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.

### Security advice

Keep everything behind the firewall. Services like download and search will need access to the main
servlet implementing the `GET` methods. Only emergency fixes will need access to the update servlet
implementing the `DELETE` methods. Unintentional deletes won't hurt functionality but might have a
performance penalty.

### Performance advice

Keeping the default `solr.url` and `solr.core` in the `application.properties` while not having the
solr core up and running, will slow down the service even more than omitting the properties
as it will try to read and update each request in the solr cache.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-auth-info.git
        cd easy-auth-info
        mvn install
