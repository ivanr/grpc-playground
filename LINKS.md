
# Protocol Buffers (protobuf) and gRPC Notes

Protocol buffers are a language-neutral, platform-neutral extensible mechanism for serializing structured data. The homepage is at https://developers.google.com/protocol-buffers
We wish to use protocol buffers for the following reasons:
- The data is what matters, and we wish to put it centrestage. In other words, we wish to practice schema-led development.
- It's a mature, popular, and widely used protocol.
- Protocol buffers are language neutral, enabling interoperability no matter the language used for a component.
- Code generation from schemas reduces manual work.
- We need a good way to maintain and evolve data formats, and using an external framework for that is a good approach.

Additional requirements:
- JSON interoperability; we need to define protobuf schemas, but sometimes keep data in JSON format, for example in Postgres, Elasticsearch, and expose via our public APIs. For this to work as expected, virtually all fields must be messages or optional.

## Protobuf Resources
- Protocol Buffers homepage
https://developers.google.com/protocol-buffers
- Language guide (proto 3)
https://developers.google.com/protocol-buffers/docs/proto3
- Proto3 section in Google's API Design Guide
https://cloud.google.com/apis/design/proto3
- Protolock (backward compatibility checker)
https://protolock.dev
- Buf (a tool for schema-driven development incl. backward compatibility checker)
https://github.com/bufbuild/buf
- Prototool (Buf's predecessor)
https://github.com/uber/prototool
- Awesome gRPC (many protobuf tools)
https://github.com/grpc-ecosystem/awesome-grpc
- Wire (protobuf generation library from Square)
https://github.com/square/wire
- Wire has a parser for .proto files
https://square.github.io/wire/2.x/wire-schema/
- ClickHouse appears to support protocol buffers:
https://github.com/ClickHouse/ClickHouse/blob/master/docs/en/interfaces/formats.md#protobuf
- Related: Not as popular as Protocol Buffers, FlatBuffers are similar conceptually but support accessing data directly without needing to parse it. No copying! This would provide significant performance improvements:
https://google.github.io/flatbuffers/
- Good advice around style and API evolution from Envoy proxy developers:
https://blog.envoyproxy.io/evolving-a-protocol-buffer-canonical-api-e1b2c2ca0dec and https://blog.envoyproxy.io/dynamic-extensibility-and-protocol-buffers-dcd0bf0b8801
- Gradle Protobuf plugin
https://github.com/google/protobuf-gradle-plugin
- Style guides:
  - Google' Style Guide
  - Uber's V1 Style Guide
  - Uber's V2 Style Guide
- Schema management
  - https://www.bugsnag.com/blog/libraries-for-grpc-services
  - https://medium.com/namely-labs/how-we-build-grpc-services-at-namely-52a3ae9e7c35
- QuickBuffers (protocol buffers reimplementation in Java; faster, doesn't use reflection, less allocation, etc)
  - https://github.com/HebiRobotics/QuickBuffers

## Protobuf Notes
Two versions, beware. There are two versions of protocol buffers; the older v2 and the newer v3. We want to use the latter. In v3, there are no required fields (despite what the example on the homepage shows). At the time of writing, tutorials are available only for v2, so better to stick with the reference.

- Protobuf doesn't serialise NULL values. It's not possible to set any field to null. You will never get a null field. Always check for presence. Nullable types have to be explicitly created with `oneof`.
https://itnext.io/protobuf-and-null-support-1908a15311b6

- All fields have default values, which they take if the field is not in the message. This matches the behaviour of primitive values in many languages, but also makes interoperability with JSON tricky (such fields will not appear in JSON; some platforms support export of default values).

- Optional fields are supported since 3.15. The only difference is that optional fields can, like messages, be checked for presence. This is how it's implemented behind the scenes:
  ```
  message Foo {
    int32 bar = 1;
    oneof optional_baz {
    int32 baz = 2;
    }
  }
  ```

  - More information:
  https://stackoverflow.com/questions/42622015/how-to-define-an-optional-field-in-protobuf-3
  - Direct link: https://github.com/protocolbuffers/protobuf/blob/v3.12.0/docs/field_presence.md
  - The Wire protobuf compiler works differently and will reuse Java primitive wrappers such as Integer, Float, etc. I am not sure we can use it because I don't think it supports gRPC with Java as target (only Kotlin). Given that "optional" appears to be on the way, perhaps that's the best approach to take.
  - Validation:
    - https://github.com/envoyproxy/protoc-gen-validate
    - https://medium.com/teamdev-engineering/protobuf-validation-dda3e43fa70

- Well-known types. In addition to the types in the core specification, Google defines a number of additional higher common data structures. For example, the built-in string cannot be null, but there is StringValue that can. There are types for all scalar values:
https://developers.google.com/protocol-buffers/docs/reference/google.protobuf
  
- Common types (not embedded in protoc) https://github.com/googleapis/api-common-protos 

- JSON interoperability
https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/util/JsonFormat

- Maybe Useful
https://robertsahlin.com/schema-evolution-in-streaming-dataflow-jobs-and-bigquery-tables-part-1/
https://medium.com/@akhaku/protobuf-definition-best-practices-87f281576f31

## gRPC

- Google’s API Design Guide
https://cloud.google.com/apis/design/

- Google's API Improvement Proposals
https://google.aip.dev/

- HTTP REST API mapping
https://cloud.google.com/service-management/reference/rpc/google.api#http

- gRPC
https://grpc.io/docs/
Protobuf documentation generator
https://github.com/pseudomuto/protoc-gen-doc

- Cloud Endpoints
https://cloud.google.com/endpoints/
- gRPC to JSON gateway/proxy
https://github.com/grpc-ecosystem/grpc-gateway
- gRPC Web
https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md
- gRPC Web Early Access
https://github.com/grpc/grpc/issues/8682
Generate docs from Swagger/Open API files
https://github.com/sourcey/spectacle
- gRPC command-line tool
https://github.com/grpc/grpc/blob/master/doc/command_line_tool.md
- gRPC Java by Example
https://github.com/saturnism/grpc-java-by-example/
- Very useful posts, someone’s experience adopting gRPC:
  - https://blog.bugsnag.com/grpc-and-microservices-architecture/
  - https://blog.bugsnag.com/libraries-for-grpc-services/
  - https://blog.bugsnag.com/using-grpc-in-production/
- Modifying gRPC over time
https://github.com/sbueringer/kubecon-slides/blob/master/slides/2017-kubecon-na/Modifying%20gRPC%20Services%20Over%20Time%20%5BI%5D%20-%20Eric%20Anderson%2C%20Google%20-%202017%20CloudNativeCon%20-%20Mod%20gRPC%20Services.pdf
  - https://www.youtube.com/watch?v=F2WYEFLTKEw
https://github.com/akalini/grpcbridge In-process HTTP/JSON bridge for Java
- Error handling
  - https://stackoverflow.com/questions/48748745/pattern-for-rich-error-handling-in-grpc
  - https://grpc.io/docs/guides/error/
  - https://cloud.google.com/apis/design/errors#error_model
  - https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/errorhandling/DetailErrorSample.java
  - https://github.com/uw-labs/bloomrpc "The missing GUI Client for GRPC services."
- https://github.com/testinggospels/camouflage
- https://github.com/Fadelis/grpcmock
- https://github.com/tokopedia/gripmock
- https://retroryan8080.gitlab.io/grpc-java-workshop/index.html
- https://www.baeldung.com/grpcs-error-handling

- Concurrency
    - https://netflixtechblog.medium.com/performance-under-load-3e6fa9a60581
  - https://groups.google.com/g/grpc-io/c/XCMIva8NDO8
  - https://grpc.github.io/grpc-java/javadoc/io/grpc/netty/NettyServerBuilder.html#maxConcurrentCallsPerConnection-int-
  - https://github.com/resilience4j/resilience4j

- Field masks / FieldMask
  - Covered in this video https://dev.tube/video/F2WYEFLTKEw
  - https://pinkiepractices.com/posts/protobuf-field-masks/
  - https://github.com/grpc/grpc-java/blob/master/examples/src/test/java/io/grpc/examples/helloworld/HelloWorldClientTest.java 
  - https://netflixtechblog.com/practical-api-design-at-netflix-part-1-using-protobuf-fieldmask-35cfdc606518
  - https://netflixtechblog.com/practical-api-design-at-netflix-part-2-protobuf-fieldmask-for-mutation-operations-2e75e1d230e4