/*
 Generated by org.infinispan.protostream.annotations.impl.processor.AutoProtoSchemaBuilderAnnotationProcessor
 for class org.infinispan.commons.marshall.LibraryInitializer
 annotated with @org.infinispan.protostream.annotations.AutoProtoSchemaBuilder(dependsOn=, service=false, autoImportClasses=false, excludeClasses=, includeClasses=org.infinispan.commons.marshall.Book,org.infinispan.commons.marshall.Author, basePackages={}, value={}, schemaPackageName="book_sample", schemaFilePath="proto/", schemaFileName="library.proto", className="")
 */

package org.infinispan.commons.marshall;

/**
 * WARNING: Generated code!
 */
@javax.annotation.Generated(value = "org.infinispan.protostream.annotations.impl.processor.AutoProtoSchemaBuilderAnnotationProcessor",
      comments = "Please do not edit this file!")
@org.infinispan.protostream.annotations.impl.OriginatingClasses({
      "org.infinispan.commons.marshall.Author",
      "org.infinispan.commons.marshall.Book"
})
/*@org.infinispan.protostream.annotations.AutoProtoSchemaBuilder(
   className = "LibraryInitializerImpl",
   schemaFileName = "library.proto",
   schemaFilePath = "proto/",
   schemaPackageName = "book_sample",
   service = false,
   autoImportClasses = false,
   classes = {
      org.infinispan.commons.marshall.Author.class,
      org.infinispan.commons.marshall.Book.class
   }
)*/
public class LibraryInitializerImpl implements org.infinispan.commons.marshall.LibraryInitializer {

   @Override
   public String getProtoFileName() { return "library.proto"; }

   @Override
   public String getProtoFile() { return org.infinispan.protostream.FileDescriptorSource.getResourceAsString(getClass(), "/proto/library.proto"); }

   @Override
   public void registerSchema(org.infinispan.protostream.SerializationContext serCtx) {
      serCtx.registerProtoFiles(org.infinispan.protostream.FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));
   }

   @Override
   public void registerMarshallers(org.infinispan.protostream.SerializationContext serCtx) {
      serCtx.registerMarshaller(new org.infinispan.commons.marshall.Book$___Marshaller_cdc76a682a43643e6e1d7e43ba6d1ef6f794949a45e1a8bc961046cda44c9a85());
      serCtx.registerMarshaller(new org.infinispan.commons.marshall.Author$___Marshaller_9b67e1c1ecea213b4207541b411fb9af2ae6f658610d2a4ca9126484d57786d1());
   }
}
