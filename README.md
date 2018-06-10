# Argo

A simple JSON-RPC 2.0 server framework.

See the [JSON-RPC Specification](http://www.jsonrpc.org/specification).

## Architecture

`JsonRpc.kt` provides a general-purpose JSON-RPC implementation.

`GsonRpcParser.kt` provides a serializer/deserializer based on [GSON](https://github.com/google/gson). Others could be implemented. This class can also be augmented to parse custom user parameter objects and export custom user response objects.

`Argo.kt` provides a simple framework for registering handlers to process JSON-RPC requests and produce responses.

`ArgoBitty.kt` uses a `BittyService` to receive HTTP(S) requests, pass them to an Argo handler, and send back corresponding responses.

See the test package for usage samples.

## Caveats

> This is very much a work-in-progress development release!

Things are likely to change. The Argo framework layer needs work (more flexible routing, for instance). 