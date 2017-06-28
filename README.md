# Many-World Temporal Graph: Empowering Next-Gen Live Analytics

<p align="center"><img src="logo.png" /></p>

The GreyCat escapes from the quantum box by analyzing all alternatives, now he is ready to do the same with your data.
For any questions please contact us via our Gitter:

[![Join the chat at https://gitter.im/datathings/greycat](https://badges.gitter.im/datathings/greycat.svg)](https://gitter.im/datathings/greycat?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/datathings/greycat.svg?branch=master)](https://travis-ci.org/datathings/greycat)

# Changelog
- Version 9 (~end of June 2017)
    - introduction of model environment (grammar, IDEA plugin, generator Java and TS)
    - introduction of custom types
    - unified index API, declareIndex mandatory now
    - new find method (no need to repeat indexed attribute names, for add to index and find methods)
    - new J2TS (90) and TypeScript version (2.4.1)
    - New DValueNode for high speed time-series
    - Fix lower bound for travel in times
    - Simplified update methods for index
    - Fix NPM Compatibility v5, using the tgz file instead of NPM link during compilation
- Version 8 (09/06/2017)
    - Backend API change: new mini-batch trees
    - New Traverse in Times API
    - Introducing GreyCat modeling sub-project
    - Initiate the IDEA plugin
    - New protocol for TimeTree serialization
- Version 7 (15/05/2017)
    - fix bug in global dictionary
    - add option in builder to desactivate global dictionary and introspection ability
    - fix potential NPE into dephased struct proxy for EGraph
    - fix potential dephasing problem for DMatrix
    - API change: add name into Action interface
    - API change: add common arrays interface ArrayStruct
    - add clear method to all arrays chunk
    - add savePartial method
    - add batchSaveSize option to build
    - add start and end transaction
    - PROTOCOL change: adapt Remote Task Protocol to allows nested task result
- Version 6 (11/04/2017)
    - Introducing Hash to check Remote Cache Consistency
    - New API for React Remote Graph usage
    - New API for Remote Task Context usage
    - New API for Stream Output of task (also in remote usage)
    - Fix various bugs according to new Arrays APIs.
    - Fix in Base64
    - NPE protection in Task API
    - Fix missing declaration of standard actions
    - add typed API to access complex elements such as Map, Relation...
- Version 5 (20/03/2017) 
    - New API for Arrays (now defined as complex sub objects such as Map or Relationships)
    - New Proxy approach to seamlessly manage temporal semantic of complex objects (Relationships...)
    - Temporary unload of OffHeap module (some bug remaining before the release)
- Version 4 (13/03/17)
    - Replaced declaration() in ActionRegistry and NodeRegistry by getOrCreateDeclaration. declaration() now acts like a simple get.
    - fix free bug in flat task action
    - define interfaces for BLAS plugin
    - cleanup build scripts
    - implement multi-get within RocksDB plugin
- Version 3 (27/02/17)
    - fix a-priori dirty element on get
    - fix dirty flag after remote state chunk load *(need global check)*
    - fix dirty flag after remote timeTree chunk load *(need global check)*
    - switch test execution with full NPM management
    - use J2TS-JUNIT (https://www.npmjs.com/search?q=j2ts-junit)
    - rename sub test package as GreyCatTest to avoid conflict for NPM
    - new RocksDB support for ARM-V7 processor
    - closing issue #3 add TypeScript correct typing header to NPM
    - alignment of interface NodeState, StateChunk and Node (all inherit from container)
- Version 2 (17/02/17)
    - introduce new NPM packaging (https://www.npmjs.com/package/greycat)
    - use J2TS-JRE (https://www.npmjs.com/search?q=j2ts-junit)
    - rename base package as greycat
    - closing #2
- Version 1
    - first release with the GreyCat name, mostly renamed from ManyWorldGraph project
    
